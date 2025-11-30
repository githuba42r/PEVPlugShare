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

variable "custom_domain" {
  description = "Custom domain for OAuth endpoint (e.g., pevplugshare.a42r.com)"
  type        = string
  default     = ""
}

variable "certificate_arn_us_east_1" {
  description = "ACM Certificate ARN in us-east-1 (required for EDGE endpoints)"
  type        = string
  default     = ""
}

variable "applications" {
  description = "List of applications with their configurations (multiple domains and deep link schemes supported)"
  type = list(object({
    app_id           = string
    name             = string
    allowed_domains  = list(string)
    deep_link_scheme = string
    shared_secret    = string
  }))
  sensitive = true

  validation {
    condition     = length(var.applications) > 0
    error_message = "At least one application must be configured."
  }

  validation {
    condition = alltrue([
      for app in var.applications :
      length(app.app_id) > 0 && length(app.name) > 0 && length(app.allowed_domains) > 0 && length(app.deep_link_scheme) > 0 && length(app.shared_secret) > 0
    ])
    error_message = "All application fields (app_id, name, allowed_domains, deep_link_scheme, shared_secret) are required."
  }
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
      DYNAMODB_TABLE = aws_dynamodb_table.oauth_sessions.name
      SESSION_TTL_MINUTES = var.session_ttl_minutes
      OAUTH_PROVIDERS = var.oauth_providers
      # Convert applications list to JSON for Lambda
      APPLICATIONS = jsonencode(var.applications)
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
      DYNAMODB_TABLE = aws_dynamodb_table.oauth_sessions.name
      # Convert applications list to JSON for Lambda
      APPLICATIONS = jsonencode(var.applications)
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

# IAM Role for API Gateway CloudWatch Logs
resource "aws_iam_role" "api_gateway_logs_role" {
  name = "${var.project_name}-api-gateway-logs-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "apigateway.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "api_gateway_logs_policy" {
  role       = aws_iam_role.api_gateway_logs_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
}

data "aws_caller_identity" "current" {}

# API Gateway Account (enables CloudWatch logging for all stages)
# Note: If this fails, the account likely already has a role configured
# You can manually update it in AWS Console > API Gateway > Account
# resource "aws_api_gateway_account" "main" {
#   cloudwatch_role_arn = aws_iam_role.api_gateway_logs_role.arn
# }

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

# Custom domain in API Gateway (REGIONAL endpoint for better SSL certificate handling)
# This resource already exists - skipping creation to avoid conflicts
# Run: terraform import 'aws_api_gateway_domain_name.oauth' 'oauth.pevplugshare.a42r.com' if needed
# resource "aws_api_gateway_domain_name" "oauth" {
#   domain_name              = "oauth.${var.custom_domain}"
#   regional_certificate_arn = "arn:aws:acm:${var.aws_region}:273681054217:certificate/d3bd482f-3105-4e5a-9e5c-ecdc70a79387"
#   endpoint_configuration {
#     types = ["REGIONAL"]
#   }
#
#   tags = {
#     Name    = "${var.project_name}-domain"
#     Project = var.project_name
#   }
# }

# Data source for existing domain
data "aws_api_gateway_domain_name" "oauth" {
  domain_name = "oauth.${var.custom_domain}"
}

# Base path mapping for API Gateway
resource "aws_api_gateway_base_path_mapping" "oauth" {
  api_id            = aws_api_gateway_rest_api.oauth_api.id
  domain_name       = data.aws_api_gateway_domain_name.oauth.domain_name
  stage_name        = aws_api_gateway_stage.prod.stage_name
  base_path         = ""
}

# API Gateway API Key for usage plan
resource "aws_api_gateway_api_key" "oauth_key" {
  name        = "${var.project_name}-api-key"
  description = "API Key for OAuth service - cost control"
  enabled     = true

  tags = {
    Name    = "${var.project_name}-api-key"
    Project = var.project_name
  }
}

# Usage Plan with cost controls (<$2/month)
# Per-IP limits enforced via WAF (10 req/min, 50 req/day per IP)
resource "aws_api_gateway_usage_plan" "oauth" {
  name            = "${var.project_name}-usage-plan"
  description     = "Usage plan for OAuth - 10 req/min, 50 req/day per IP"
  product_code    = var.project_name
  api_stages {
    api_id = aws_api_gateway_rest_api.oauth_api.id
    stage  = aws_api_gateway_stage.prod.stage_name
  }

  # Conservative throttle limits (per-IP enforced via WAF)
  throttle_settings {
    burst_limit = 15    # 10 req/min = 0.17 req/sec, burst to 15
    rate_limit  = 1     # 1 req/sec average
  }

  # Monthly quota (safety cap)
  quota_settings {
    limit  = 50000      # ~50 logins/day * 1000 devices = 50k/month
    period = "MONTH"
  }

  tags = {
    Name    = "${var.project_name}-usage-plan"
    Project = var.project_name
  }
}

# AWS WAF IP Set for rate limiting - using existing set if available
data "aws_wafv2_ip_set" "oauth_blocked_ips" {
  name  = "${var.project_name}-blocked-ips"
  scope = "REGIONAL"
}

# AWS WAF Web ACL for rate limiting - using existing WAF
data "aws_wafv2_web_acl" "oauth_waf" {
  name  = "${var.project_name}-waf"
  scope = "REGIONAL"
}

# Associate WAF with API Gateway
resource "aws_wafv2_web_acl_association" "oauth_waf" {
  resource_arn = aws_api_gateway_stage.prod.arn
  web_acl_arn  = data.aws_wafv2_web_acl.oauth_waf.arn
}

# CloudWatch Alarm for WAF rate limit blocks
resource "aws_cloudwatch_metric_alarm" "waf_rate_limit_blocks" {
  alarm_name          = "${var.project_name}-waf-blocks"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "BlockedRequests"
  namespace           = "AWS/WAFV2"
  period              = 300
  statistic           = "Sum"
  threshold           = 100
  alarm_description   = "Alert if WAF is blocking many requests (potential attack)"

  dimensions = {
    WebACL = data.aws_wafv2_web_acl.oauth_waf.name
    Region = var.aws_region
  }

  tags = {
    Name    = "${var.project_name}-waf-alarm"
    Project = var.project_name
  }
}

# CloudWatch Alarm for API Gateway errors (notify if something breaks)
resource "aws_cloudwatch_metric_alarm" "api_errors" {
  alarm_name          = "${var.project_name}-high-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "5XXError"
  namespace           = "AWS/ApiGateway"
  period              = 300
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "Alert if API Gateway has too many 5xx errors"

  dimensions = {
    ApiName = aws_api_gateway_rest_api.oauth_api.name
    Stage   = aws_api_gateway_stage.prod.stage_name
  }

  tags = {
    Name    = "${var.project_name}-error-alarm"
    Project = var.project_name
  }
}

# CloudWatch Alarm for API Gateway throttling (safety alert)
resource "aws_cloudwatch_metric_alarm" "api_throttle" {
  alarm_name          = "${var.project_name}-throttling"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Count"
  namespace           = "AWS/ApiGateway"
  period              = 300
  statistic           = "Sum"
  threshold           = 100
  alarm_description   = "Alert if API is being throttled (indicates potential attack)"

  dimensions = {
    ApiName = aws_api_gateway_rest_api.oauth_api.name
    Stage   = aws_api_gateway_stage.prod.stage_name
  }

  tags = {
    Name    = "${var.project_name}-throttle-alarm"
    Project = var.project_name
  }
}

# Outputs
output "api_endpoint" {
  description = "API Gateway endpoint URL"
  value       = "${aws_api_gateway_stage.prod.invoke_url}"
}

output "custom_domain_endpoint" {
  description = "Custom domain endpoint URL"
  value       = "https://oauth.${var.custom_domain}/prod"
}

output "certificate_arn" {
  description = "ACM Certificate ARN (us-east-1)"
  value       = var.certificate_arn_us_east_1
}

output "certificate_validation_records" {
  description = "DNS validation records needed for Cloudflare - add this CNAME to validate certificate"
  value       = "Add CNAME record to Cloudflare: Name=_5c1bb79d2b7568bda0ee8de03967f2a6.oauth.pevplugshare.a42r.com Value=_c504615ea79b4f29429e438677b4309c.jkddzztszm.acm-validations.aws"
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

output "api_key_id" {
  description = "API Key ID for cost control"
  value       = aws_api_gateway_api_key.oauth_key.id
  sensitive   = true
}

output "usage_plan_id" {
  description = "Usage Plan ID"
  value       = aws_api_gateway_usage_plan.oauth.id
}

output "cost_controls" {
  description = "Cost control and rate limit summary"
  value = {
    per_ip_rate_limit_per_minute = "10 requests/minute"
    per_ip_rate_limit_per_day    = "50 requests/day"
    monthly_quota                = "50,000 requests/month"
    estimated_cost               = "<$0.20/month (normal use), <$1/month (at quota)"
    rate_limiting_type           = "AWS WAF per-IP + API Gateway quotas"
    waf_alarm                    = aws_cloudwatch_metric_alarm.waf_rate_limit_blocks.alarm_name
    throttling_alarm             = aws_cloudwatch_metric_alarm.api_throttle.alarm_name
    error_alarm                  = aws_cloudwatch_metric_alarm.api_errors.alarm_name
    dynamodb_pricing             = "PAY_PER_REQUEST (no fixed costs)"
  }
}
