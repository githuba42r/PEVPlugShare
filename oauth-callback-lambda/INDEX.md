# AWS Lambda OAuth Redirector - Documentation Index

## 📚 Complete Documentation Suite

This project includes comprehensive documentation for deploying and managing the OAuth Redirector Lambda service.

---

## 🎯 Choose Your Path

### ⚡ **I want to get started quickly (5 minutes)**
→ Start with: **[QUICKSTART.md](QUICKSTART.md)**
- Prerequisites checklist
- 4-step deployment (supports single or multiple apps)
- Verification steps
- Testing multi-app setup
- Basic troubleshooting

### 📖 **I want detailed instructions**
→ Start with: **[INSTALLATION_GUIDE.md](INSTALLATION_GUIDE.md)**
- Detailed prerequisites
- Multi-application configuration examples
- 7-step deployment with explanations
- Testing procedures
- Comprehensive troubleshooting
- Post-deployment configuration
- Security setup

### ⚙️ **I need to configure multiple applications**
→ Start with: **[TERRAFORM_UPDATE.md](TERRAFORM_UPDATE.md)**
- Multi-application configuration guide
- Before/after configuration format
- Examples: single app, multiple domains, multiple apps
- Field reference
- Migration from old format
- Security best practices

### 🏗️ **I want to understand the architecture**
→ Start with: **[README.md](README.md)**
- Project overview
- Architecture diagram
- Feature list (includes multi-app support)
- Project structure
- API reference
- Development guide

---

## 📄 Document Overview

| Document | Size | Time | For Whom |
|----------|------|------|----------|
| [README.md](README.md) | 11 KB | 5-10 min | Overview & architecture |
| [QUICKSTART.md](QUICKSTART.md) | 3 KB | 5 min | Fast deployment (any config) |
| [INSTALLATION_GUIDE.md](INSTALLATION_GUIDE.md) | 10 KB | 30 min | Detailed setup with examples |
| [TERRAFORM_UPDATE.md](TERRAFORM_UPDATE.md) | 8 KB | 15 min | Multi-app Terraform guide |
| [LAMBDA_DEPLOYMENT.md](LAMBDA_DEPLOYMENT.md) | 9.9 KB | 20 min | Configuration reference |

---

## 🚀 Quick Navigation

### Getting Started
1. **Prerequisites**: [INSTALLATION_GUIDE.md#prerequisites](INSTALLATION_GUIDE.md)
2. **Generate Secrets**: [QUICKSTART.md#1-generate-secrets-30-seconds](QUICKSTART.md)
3. **Configure Terraform**: [TERRAFORM_UPDATE.md#how-to-configure](TERRAFORM_UPDATE.md)
4. **Deploy**: [QUICKSTART.md#3-deploy-3-minutes](QUICKSTART.md)

### Configuration
- **Single Application**: [TERRAFORM_UPDATE.md#example-1-single-application](TERRAFORM_UPDATE.md)
- **Multiple Domains**: [TERRAFORM_UPDATE.md#example-2-development--production](TERRAFORM_UPDATE.md)
- **Multiple Applications**: [TERRAFORM_UPDATE.md#example-3-multiple-applications](TERRAFORM_UPDATE.md)

### Deployment
- **Using Deploy Script**: [INSTALLATION_GUIDE.md#option-a-using-the-deployment-script-recommended](INSTALLATION_GUIDE.md)
- **Manual Deployment**: [INSTALLATION_GUIDE.md#option-b-manual-deployment](INSTALLATION_GUIDE.md)

### Android App Integration
- **Configure App**: [INSTALLATION_GUIDE.md#step-6-configure-the-android-app](INSTALLATION_GUIDE.md)
- **Testing**: [INSTALLATION_GUIDE.md#step-7-test-the-lambda-deployment](INSTALLATION_GUIDE.md)

### Advanced
- **Security Setup**: [LAMBDA_DEPLOYMENT.md#security-best-practices](LAMBDA_DEPLOYMENT.md#security-best-practices)
- **Multi-App Setup**: [LAMBDA_DEPLOYMENT.md#multi-application-setup](LAMBDA_DEPLOYMENT.md#multi-application-setup)
- **Custom Domain**: [INSTALLATION_GUIDE.md#set-up-api-gateway-custom-domain](INSTALLATION_GUIDE.md#3-set-up-api-gateway-custom-domain)
- **Cleanup**: [INSTALLATION_GUIDE.md#cleanup-and-destruction](INSTALLATION_GUIDE.md#cleanup-and-destruction)

---

## 📋 Checklist for First-Time Deployment

### Prerequisites (10 minutes)
- [ ] AWS account created
- [ ] AWS CLI v2 installed: `aws --version`
- [ ] Terraform installed: `terraform --version`
- [ ] Python 3 installed: `python3 --version`
- [ ] AWS credentials configured: `aws configure`

### Deployment (5 minutes)
- [ ] Secret generated: `python3 -c "import secrets; print(secrets.token_urlsafe(32))"`
- [ ] terraform.tfvars configured
- [ ] `bash scripts/deploy.sh` executed
- [ ] Deployment confirmed with "yes"

### Integration (10 minutes)
- [ ] API endpoint captured from output
- [ ] Android app secrets.properties updated
- [ ] Android app rebuilt and deployed
- [ ] OAuth flow tested
- [ ] CloudWatch logs verified

### Optional Production Setup (30 minutes)
- [ ] CloudWatch alarms configured
- [ ] AWS Secrets Manager setup
- [ ] Custom domain configured
- [ ] Secret rotation scheduled

---

## 🔧 Project Structure

```
oauth-callback-lambda/
├── README.md                          ← Start here for overview
├── QUICKSTART.md                      ← 5-minute deployment
├── INSTALLATION_GUIDE.md              ← Complete instructions
├── LAMBDA_DEPLOYMENT.md               ← Configuration reference
├── INDEX.md                           ← This file
│
├── infrastructure/
│   ├── main.tf                        ← Terraform configuration
│   ├── terraform.tfvars.example       ← Config template
│   └── terraform.tfvars               ← Your config (create)
│
├── scripts/
│   ├── deploy.sh                      ← Main deployment script
│   ├── build.sh                       ← Build Lambda packages
│   └── destroy.sh                     ← Cleanup script
│
├── src/
│   ├── register.py                    ← OAuth register handler
│   ├── callback.py                    ← OAuth callback handler
│   ├── applications.py                ← App configuration
│   ├── dynamo.py                      ← DynamoDB operations
│   ├── security.py                    ← Signature verification
│   └── requirements.txt               ← Python dependencies
│
└── docker/
    └── Dockerfile                     ← Container image
```

---

## 💡 Key Concepts

### OAuth Flow
1. Mobile app requests OAuth session from `/oauth/register`
2. Lambda creates session in DynamoDB with TTL
3. App opens OAuth provider in browser
4. Provider calls Lambda callback at `/oauth/callback/{session_id}`
5. Lambda validates and redirects back to app

### Security
- **HMAC Signature**: All requests verified with shared secret
- **Timestamp Validation**: Prevents replay attacks
- **Per-App Secrets**: Each application has own secret
- **TTL Cleanup**: Sessions auto-expire and delete

### Infrastructure
- **Lambda Functions**: Serverless compute
- **API Gateway**: REST API with request routing
- **DynamoDB**: NoSQL session storage with TTL
- **CloudWatch**: Logging and monitoring
- **IAM**: Minimal permissions for security

---

## 📊 Quick Facts

| Item | Value |
|------|-------|
| **Setup Time** | 5 minutes |
| **Monthly Cost** | ~$0.05 (for 10K flows) |
| **Architecture** | Serverless (no servers to manage) |
| **Database** | DynamoDB with TTL |
| **Logging** | CloudWatch (automatic) |
| **Configuration** | Terraform (Infrastructure as Code) |
| **Language** | Python 3 |
| **Signature Method** | HMAC-SHA256 |
| **Session TTL** | 10 minutes (configurable) |

---

## 🆘 Troubleshooting Quick Links

### Common Issues
- **AWS credentials**: [INSTALLATION_GUIDE.md#aws-credentials-not-configured](INSTALLATION_GUIDE.md#troubleshooting)
- **Terraform not found**: [INSTALLATION_GUIDE.md#terraform-not-found](INSTALLATION_GUIDE.md#troubleshooting)
- **Deployment failed**: [INSTALLATION_GUIDE.md#deployment-failed](INSTALLATION_GUIDE.md#troubleshooting)
- **Lambda not visible**: [INSTALLATION_GUIDE.md#lambda-function-not-found](INSTALLATION_GUIDE.md#troubleshooting)
- **Invalid signature**: [LAMBDA_DEPLOYMENT.md#invalid_signature-error](LAMBDA_DEPLOYMENT.md#invalid_signature-error)

### Get Help
1. Check [Troubleshooting](INSTALLATION_GUIDE.md#troubleshooting)
2. View CloudWatch logs: `aws logs tail /aws/lambda/oauth-register`
3. Check Terraform plan: `cd infrastructure && terraform plan`
4. Review [AWS Documentation](https://docs.aws.amazon.com/)

---

## 📚 External Resources

### AWS Services
- [Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [API Gateway Guide](https://docs.aws.amazon.com/apigateway/)
- [DynamoDB Tutorial](https://docs.aws.amazon.com/dynamodb/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/)

### OAuth 2.0
- [OAuth 2.0 Flows](https://oauth.net/2/)
- [OpenRouter OAuth](https://openrouter.ai/)
- [GitHub OAuth Setup](https://docs.github.com/en/developers/apps/building-oauth-apps)

---

## ✅ What You'll Have After Deployment

- ✓ 2 Lambda functions (register + callback)
- ✓ API Gateway REST API
- ✓ DynamoDB sessions table with TTL
- ✓ CloudWatch logs for debugging
- ✓ IAM roles with minimal permissions
- ✓ API endpoint ready for your app

---

## 🎯 Next Steps

1. **Choose your path** from the options above
2. **Read the appropriate documentation**
3. **Follow the step-by-step instructions**
4. **Deploy and test**
5. **Integrate with your Android app**
6. **Monitor and maintain**

**Estimated total time: 30-45 minutes**

---

## 📞 Support

Need help? Check:
1. The documentation section for your topic
2. [Troubleshooting Guide](INSTALLATION_GUIDE.md#troubleshooting)
3. CloudWatch logs: `aws logs tail /aws/lambda/oauth-register --follow`
4. AWS support console

---

**Last Updated**: November 30, 2025  
**Version**: 1.0  
**Status**: ✅ Complete
