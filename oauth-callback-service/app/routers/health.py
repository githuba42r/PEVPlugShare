"""
Health check and utility endpoints
"""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime

from app.models import OAuthSession, get_db, HealthResponse
from app.config import settings

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
async def health_check(db: Session = Depends(get_db)):
    """
    Health check endpoint

    Returns service status and active session count
    """
    # Count active (non-expired, non-used) sessions
    active_count = db.query(OAuthSession).filter(
        OAuthSession.used == False,
        OAuthSession.expires_at > datetime.utcnow()
    ).count()

    return HealthResponse(
        status="healthy",
        version="1.0.0",
        active_sessions=active_count
    )


@router.get("/")
async def root():
    """
    Root endpoint - API information
    """
    return {
        "service": "OAuth Callback Service",
        "version": "1.0.0",
        "description": "Generic OAuth 2.0 callback handler for mobile apps",
        "endpoints": {
            "health": "/health",
            "register": "POST /oauth/register",
            "callback": "GET /oauth/callback/{session_id}"
        },
        "supported_providers": settings.get_oauth_providers_list(),
        "documentation": "/docs"
    }
