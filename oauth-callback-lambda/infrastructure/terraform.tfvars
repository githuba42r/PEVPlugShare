aws_region = "ap-southeast-2"
project_name = "oauth-callback"
custom_domain = "pevplugshare.a42r.com"
certificate_arn_us_east_1 = "arn:aws:acm:us-east-1:273681054217:certificate/56e52703-b623-4d60-a9dc-b9a83c09b726"

# REQUIRED: Applications configuration (supports multiple apps with multiple domains)
applications = [
  {
    app_id           = "pevplugshare"
    name             = "PEV PlugShare"
    allowed_domains  = ["oauth.pevplugshare.a42r.com", "nijij1ku52.execute-api.ap-southeast-2.amazonaws.com", "localhost:8000"]
    deep_link_scheme = "pevplugshare://oauth/callback"
    shared_secret = "CRxzBIleEQfuAW-8Y0hFgkHD4jCciDxxDQFd6LX2jrM"
  }
  # Add more applications as needed:
  # {
  #   app_id           = "another-app"
  #   name             = "Another App"
  #   allowed_domains  = ["app.example.com", "www.app.example.com"]
  #   deep_link_scheme = "anotherapp://oauth/callback"
  #   shared_secret    = "generate-separate-secret-per-app"
  # }
]

# Optional configurations
session_ttl_minutes = 10
oauth_providers = "openrouter,github,google"
