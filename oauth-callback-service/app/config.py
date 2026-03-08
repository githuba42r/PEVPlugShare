"""
Configuration management for OAuth callback service
"""
from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Service settings
    host: str = "0.0.0.0"
    port: int = 8000
    webui_port: int = 8001

    # Database
    database_url: str = "sqlite:///./oauth_sessions.db"

    # Session settings
    session_ttl_minutes: int = 10
    cleanup_interval_minutes: int = 5

    # Rate limiting (per IP address)
    rate_limit_per_minute: int = 10
    rate_limit_per_hour: int = 100
    rate_limit_per_day: int = 1000

    # CORS
    allowed_origins: str = "*"

    def get_allowed_origins_list(self) -> List[str]:
        """Parse comma-separated CORS origins"""
        if self.allowed_origins == "*":
            return ["*"]
        return [origin.strip() for origin in self.allowed_origins.split(",")]

    # OAuth providers
    oauth_providers: str = "openrouter,github,google"

    def get_oauth_providers_list(self) -> List[str]:
        """Parse comma-separated OAuth provider names"""
        return [provider.strip().lower() for provider in self.oauth_providers.split(",")]

    # Logging
    log_level: str = "INFO"
    environment: str = "development"  # production or development

    # Diagnostic endpoints (for testing/debugging)
    enable_diagnostic_endpoints: bool = False

    # Application identification method
    # Options: "domain" (default, matches by allowed_domains) or "appid" (matches by app_id in request)
    app_identification_method: str = "domain"

    class Config:
        env_file = ".env"
        case_sensitive = False


# Global settings instance
settings = Settings()
