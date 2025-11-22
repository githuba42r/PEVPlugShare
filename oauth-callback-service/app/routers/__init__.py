"""
__init__.py for routers package
"""
from app.routers.oauth import router as oauth_router
from app.routers.health import router as health_router

__all__ = ["oauth_router", "health_router"]
