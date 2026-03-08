"""
Health check and utility endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime
import time
import sys
import platform
import json
import os

from app.models import OAuthSession, get_db, HealthResponse
from app.config import settings

router = APIRouter()

# Simple in-memory rate limiter for diagnostic endpoints
_diagnostic_rate_limit = {}
_diagnostic_limit_per_minute = 5  # Very strict limit


def load_version_info():
    """Load version information from version.json"""
    version_file = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'version.json')
    try:
        if os.path.exists(version_file):
            with open(version_file, 'r') as f:
                return json.load(f)
    except Exception:
        pass
    return {'version': '1.0.0', 'build_date': '', 'git_commit': '', 'git_branch': ''}


_version_info = load_version_info()


def check_diagnostic_rate_limit(client_ip: str) -> bool:
    """Check if client has exceeded diagnostic endpoint rate limit"""
    now = time.time()
    cutoff = now - 60  # 1 minute window

    # Clean up old entries
    _diagnostic_rate_limit[client_ip] = [
        ts for ts in _diagnostic_rate_limit.get(client_ip, [])
        if ts > cutoff
    ]

    # Check limit
    if len(_diagnostic_rate_limit.get(client_ip, [])) >= _diagnostic_limit_per_minute:
        return False

    # Record this request
    _diagnostic_rate_limit.setdefault(client_ip, []).append(now)
    return True


@router.get("/ping")
async def ping(request: Request):
    """
    Simple ping endpoint for connectivity testing

    Rate limited to 5 requests per minute per IP.
    Only available when ENABLE_DIAGNOSTIC_ENDPOINTS=true
    """
    if not settings.enable_diagnostic_endpoints:
        raise HTTPException(status_code=404, detail="Not found")

    # Rate limiting
    client_ip = request.client.host if request.client else "unknown"
    if not check_diagnostic_rate_limit(client_ip):
        raise HTTPException(
            status_code=429,
            detail="Rate limit exceeded for diagnostic endpoints (5 per minute)"
        )

    return {
        "status": "pong",
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "server_time": int(time.time())
    }


@router.get("/version")
async def version(request: Request):
    """
    Version and system information endpoint

    Rate limited to 5 requests per minute per IP.
    Only available when ENABLE_DIAGNOSTIC_ENDPOINTS=true
    """
    if not settings.enable_diagnostic_endpoints:
        raise HTTPException(status_code=404, detail="Not found")

    # Rate limiting
    client_ip = request.client.host if request.client else "unknown"
    if not check_diagnostic_rate_limit(client_ip):
        raise HTTPException(
            status_code=429,
            detail="Rate limit exceeded for diagnostic endpoints (5 per minute)"
        )

    return {
        "service": "OAuth Callback Service",
        "version": _version_info['version'],
        "build_date": _version_info['build_date'],
        "git_commit": _version_info['git_commit'],
        "python_version": f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}",
        "platform": platform.system(),
        "platform_release": platform.release(),
        "uptime_seconds": int(time.time() - _start_time),
        "diagnostic_endpoints_enabled": settings.enable_diagnostic_endpoints,
        "supported_providers": settings.get_oauth_providers_list(),
        "rate_limits": {
            "oauth_endpoints": {
                "per_minute": settings.rate_limit_per_minute,
                "per_hour": settings.rate_limit_per_hour,
                "per_day": settings.rate_limit_per_day
            },
            "diagnostic_endpoints": {
                "per_minute": _diagnostic_limit_per_minute
            }
        }
    }


# Track service start time for uptime calculation
_start_time = time.time()

# Simple in-memory rate limiter for diagnostic endpoints
_diagnostic_rate_limit = {}
_diagnostic_limit_per_minute = 5  # Very strict limit


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
        version=_version_info['version'],
        active_sessions=active_count
    )


@router.get("/")
async def root():
    """
    Root endpoint - API information
    """
    return {
        "service": "OAuth Callback Service",
        "version": _version_info['version'],
        "build_date": _version_info['build_date'],
        "description": "Generic OAuth 2.0 callback handler for mobile apps",
        "endpoints": {
            "health": "/health",
            "register": "POST /oauth/register",
            "callback": "GET /oauth/callback/{session_id}"
        },
        "supported_providers": settings.get_oauth_providers_list(),
        "documentation": "/docs"
    }
