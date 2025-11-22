terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Variables
variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
  default     = "oauth-callback"
}

variable "domain_name" {
  description = "Public domain name for API"
  type        = string
}

variable "shared_secret" {
  description = "Shared secret for HMAC signatures"
  type        = string
  sensitive   = true
}

variable "session_ttl_minutes" {
  description = "Session TTL in minutes"
  type        = number
  default     = 10
}

variable "oauth_providers" {
  description = "Comma-separated list of allowed OAuth providers"
  type        = string
  default     = "openrouter,github,google"
}

variable "deep_link_scheme" {
  description = "Mobile app deep link scheme"
  type        = string
  default     = "pevplugshare://oauth/callback"
}

# DynamoDB Table
resource "aws_dynamodb_table" "oauth_sessions" {
  name           = "${var.project_name}-sessions"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "session_id"

  attribute {
    name = "session_id"
    type = "S"
  }

  ttl {
    attribute_name = "expires_at"
    enabled        = true
  }

  tags = {
    Name        = "${var.project_name}-sessions"
    Project     = var.project_name
    Environment = terraform.workspace
  }
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "${var.project_name}-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })

  tags = {
    Name    = "${var.project_name}-lambda-role"
    Project = var.project_name
  }
}

# IAM Policy for Lambda - CloudWatch Logs
resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# IAM Policy for Lambda - DynamoDB Access
resource "aws_iam_role_policy" "lambda_dynamodb" {
  name = "${var.project_name}-lambda-dynamodb"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:Query",
          "dynamodb:Scan"
        ]
        Resource = aws_dynamodb_table.oauth_sessions.arn
      }
    ]
  })
}

# Lambda Function - Register
resource "aws_lambda_function" "register" {
  filename      = "${path.module}/../build/register.zip"
  function_name = "${var.project_name}-register"
  role          = aws_iam_role.lambda_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.12"
  timeout       = 10
  memory_size   = 128

  source_code_hash = filebase64sha256("${path.module}/../build/register.zip")

  environment {
    variables = {
      DYNAMODB_TABLE        = aws_dynamodb_table.oauth_sessions.name
      DOMAIN_NAME           = var.domain_name
      SESSION_TTL_MINUTES   = var.session_ttl_minutes
      OAUTH_PROVIDERS       = var.oauth_providers
      SHARED_SECRET         = var.shared_secret
    }
  }

  tags = {
    Name    = "${var.project_name}-register"
    Project = var.project_name
  }
}

# Lambda Function - Callback
resource "aws_lambda_function" "callback" {
  filename      = "${path.module}/../build/callback.zip"
  function_name = "${var.project_name}-callback"
  role          = aws_iam_role.lambda_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.12"
  timeout       = 10
  memory_size   = 128

  source_code_hash = filebase64sha256("${path.module}/../build/callback.zip")

  environment {
    variables = {
      DYNAMODB_TABLE    = aws_dynamodb_table.oauth_sessions.name
      DEEP_LINK_SCHEME  = var.deep_link_scheme
    }
  }

  tags = {
    Name    = "${var.project_name}-callback"
    Project = var.project_name
  }
}

# API Gateway REST API
resource "aws_api_gateway_rest_api" "oauth_api" {
  name        = "${var.project_name}-api"
  description = "OAuth callback handler API"

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = {
    Name    = "${var.project_name}-api"
    Project = var.project_name
  }
}

# API Gateway Resource - /oauth
resource "aws_api_gateway_resource" "oauth" {
  rest_api_id = aws_api_gateway_rest_api.oauth_api.id
  parent_id   = aws_api_gateway_rest_api.oauth_api.root_resource_id
  path_part   = "oauth"
}

# API Gateway Resource - /oauth/register
resource "aws_api_gateway_resource" "register" {
  rest_api_id = aws_api_gateway_rest_api.oauth_api.id
  parent_id   = aws_api_gateway_resource.oauth.id
  path_part   = "register"
}

# API Gateway Resource - /oauth/callback
resource "aws_api_gateway_resource" "callback" {
  rest_api_id = aws_api_gateway_rest_api.oauth_api.id
  parent_id   = aws_api_gateway_resource.oauth.id
  path_part   = "callback"
}

# API Gateway Resource - /oauth/callback/{session_id}
resource "aws_api_gateway_resource" "callback_session" {
  rest_api_id = aws_api_gateway_rest_api.oauth_api.id
  parent_id   = aws_api_gateway_resource.callback.id
  path_part   = "{session_id}"
}

# API Gateway Method - POST /oauth/register
resource "aws_api_gateway_method" "register_post" {
  rest_api_id   = aws_api_gateway_rest_api.oauth_api.id
  resource_id   = aws_api_gateway_resource.register.id
  http_method   = "POST"
  authorization = "NONE"
}

# API Gateway Method - GET /oauth/callback/{session_id}
resource "aws_api_gateway_method" "callback_get" {
  rest_api_id   = aws_api_gateway_rest_api.oauth_api.id
  resource_id   = aws_api_gateway_resource.callback_session.id
  http_method   = "GET"
  authorization = "NONE"

  request_parameters = {
    "method.request.path.session_id" = true
  }
}

# API Gateway Integration - Register Lambda
resource "aws_api_gateway_integration" "register_lambda" {
  rest_api_id = aws_api_gateway_rest_api.oauth_api.id
  resource_id = aws_api_gateway_resource.register.id
  http_method = aws_api_gateway_method.register_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.register.invoke_arn
}

# API Gateway Integration - Callback Lambda
resource "aws_api_gateway_integration" "callback_lambda" {
  rest_api_id = aws_api_gateway_rest_api.oauth_api.id
  resource_id = aws_api_gateway_resource.callback_session.id
  http_method = aws_api_gateway_method.callback_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.callback.invoke_arn
}

# Lambda Permissions for API Gateway
resource "aws_lambda_permission" "register_apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.register.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.oauth_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "callback_apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.callback.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.oauth_api.execution_arn}/*/*"
}

# API Gateway Deployment
resource "aws_api_gateway_deployment" "oauth_api" {
  depends_on = [
    aws_api_gateway_integration.register_lambda,
    aws_api_gateway_integration.callback_lambda
  ]

  rest_api_id = aws_api_gateway_rest_api.oauth_api.id

  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.register.id,
      aws_api_gateway_method.register_post.id,
      aws_api_gateway_integration.register_lambda.id,
      aws_api_gateway_resource.callback_session.id,
      aws_api_gateway_method.callback_get.id,
      aws_api_gateway_integration.callback_lambda.id,
    ]))
  }

  lifecycle {
    create_before_destroy = true
  }
}

# API Gateway Stage
resource "aws_api_gateway_stage" "prod" {
  deployment_id = aws_api_gateway_deployment.oauth_api.id
  rest_api_id   = aws_api_gateway_rest_api.oauth_api.id
  stage_name    = "prod"

  tags = {
    Name    = "${var.project_name}-prod"
    Project = var.project_name
  }
}

# CloudWatch Log Group for API Gateway
resource "aws_cloudwatch_log_group" "api_gateway" {
  name              = "/aws/apigateway/${var.project_name}"
  retention_in_days = 7

  tags = {
    Name    = "${var.project_name}-api-logs"
    Project = var.project_name
  }
}

# Outputs
output "api_endpoint" {
  description = "API Gateway endpoint URL"
  value       = "${aws_api_gateway_stage.prod.invoke_url}"
}

output "dynamodb_table" {
  description = "DynamoDB table name"
  value       = aws_dynamodb_table.oauth_sessions.name
}

output "register_lambda_arn" {
  description = "Register Lambda function ARN"
  value       = aws_lambda_function.register.arn
}

output "callback_lambda_arn" {
  description = "Callback Lambda function ARN"
  value       = aws_lambda_function.callback.arn
}
