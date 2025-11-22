"""
Pydantic schemas for API request/response validation
"""
from pydantic import BaseModel, Field
from typing import Optional


class RegisterRequest(BaseModel):
    """Request body for session registration"""
    provider: str = Field(..., description="OAuth provider name (e.g., 'openrouter', 'github')")
    timestamp: int = Field(..., description="Unix timestamp for request validation")
    signature: str = Field(..., description="HMAC-SHA256 signature of request data")

    class Config:
        json_schema_extra = {
            "example": {
                "provider": "openrouter",
                "timestamp": 1700000000,
                "signature": "abc123..."
            }
        }


class RegisterResponse(BaseModel):
    """Response for successful session registration"""
    session_id: str = Field(..., description="Unique session identifier")
    callback_url: str = Field(..., description="Full callback URL to use in OAuth flow")
    expires_in: int = Field(..., description="Session TTL in seconds")
    provider: str = Field(..., description="OAuth provider name")

    class Config:
        json_schema_extra = {
            "example": {
                "session_id": "550e8400-e29b-41d4-a716-446655440000",
                "callback_url": "https://your-domain.com/oauth/callback/550e8400-e29b-41d4-a716-446655440000",
                "expires_in": 600,
                "provider": "openrouter"
            }
        }


class ErrorResponse(BaseModel):
    """Standard error response"""
    error: str = Field(..., description="Error type")
    message: str = Field(..., description="Human-readable error message")
    details: Optional[dict] = Field(None, description="Additional error details")

    class Config:
        json_schema_extra = {
            "example": {
                "error": "invalid_signature",
                "message": "Request signature verification failed",
                "details": {"timestamp_diff": 305}
            }
        }


class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field(..., description="Service status")
    version: str = Field(..., description="API version")
    active_sessions: int = Field(..., description="Number of active sessions")

    class Config:
        json_schema_extra = {
            "example": {
                "status": "healthy",
                "version": "1.0.0",
                "active_sessions": 5
            }
        }
