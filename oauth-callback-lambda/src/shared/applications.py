"""
Application configuration management for Lambda

For simplicity, applications are configured via environment variables.
For production, consider using DynamoDB or AWS Secrets Manager.
"""
import json
import os
from typing import Optional, List, Dict


class Application:
    """Application configuration"""

    def __init__(self, app_id: str, name: str, allowed_domains: List[str],
                 deep_link_scheme: str, shared_secret: str):
        self.app_id = app_id
        self.name = name
        self.allowed_domains = allowed_domains
        self.deep_link_scheme = deep_link_scheme
        self.shared_secret = shared_secret

    def is_domain_allowed(self, domain: str) -> bool:
        """Check if a domain is allowed for this application"""
        # Exact match or subdomain match
        return domain in self.allowed_domains


def load_applications() -> Dict[str, Application]:
    """
    Load applications from environment variable

    Environment variable format (JSON):
    APPLICATIONS='[
        {
            "app_id": "pevplugshare",
            "name": "PEV PlugShare",
            "allowed_domains": ["pevplugshare.a42r.com", "localhost"],
            "deep_link_scheme": "pevplugshare://oauth/callback",
            "shared_secret": "your-secret-here"
        }
    ]'

    Returns:
        Dict mapping domain -> Application
    """
    apps_json = os.environ.get('APPLICATIONS', '[]')

    try:
        apps_data = json.loads(apps_json)
    except json.JSONDecodeError:
        print(f"ERROR: Invalid APPLICATIONS JSON: {apps_json}")
        return {}

    # Build mapping: domain -> Application
    domain_map = {}

    for app_data in apps_data:
        app = Application(
            app_id=app_data['app_id'],
            name=app_data['name'],
            allowed_domains=app_data['allowed_domains'],
            deep_link_scheme=app_data['deep_link_scheme'],
            shared_secret=app_data['shared_secret']
        )

        # Map each domain to this application
        for domain in app.allowed_domains:
            domain_map[domain] = app

    return domain_map


def get_application_by_domain(domain: str, domain_map: Optional[Dict[str, Application]] = None) -> Optional[Application]:
    """
    Get application by request domain

    Args:
        domain: Request hostname (e.g., "pevplugshare.a42r.com")
        domain_map: Pre-loaded domain map (optional, will load if not provided)

    Returns:
        Application if found, None otherwise
    """
    if domain_map is None:
        domain_map = load_applications()

    return domain_map.get(domain)


# Load applications at module import (cached for Lambda container reuse)
_APPLICATIONS = load_applications()


def get_application(domain: str) -> Optional[Application]:
    """Get application by domain using cached configuration"""
    return _APPLICATIONS.get(domain)
