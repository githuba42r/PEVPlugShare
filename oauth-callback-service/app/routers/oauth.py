"""
OAuth router - handles session registration and callback endpoints
"""
import uuid
from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, Request, HTTPException, Query
from fastapi.responses import HTMLResponse, RedirectResponse
from sqlalchemy.orm import Session
from typing import Optional

from app.models import (
    OAuthSession,
    get_db,
    RegisterRequest,
    RegisterResponse,
    ErrorResponse
)
from app.config import settings
from app.utils import verify_signature

router = APIRouter()


@router.post("/oauth/register", response_model=RegisterResponse, responses={
    400: {"model": ErrorResponse},
    429: {"model": ErrorResponse}
})
async def register_session(
    request_data: RegisterRequest,
    request: Request,
    db: Session = Depends(get_db)
):
    """
    Register a new OAuth session and get callback URL

    This endpoint creates a temporary session that will be used to track
    the OAuth flow. The returned callback_url should be used as the
    redirect_uri/callback_url in the OAuth authorization request.
    """
    # Verify provider is allowed
    provider_lower = request_data.provider.lower()
    allowed_providers = settings.get_oauth_providers_list()

    if provider_lower not in allowed_providers:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "invalid_provider",
                "message": f"Provider '{request_data.provider}' is not supported",
                "details": {"allowed_providers": allowed_providers}
            }
        )

    # Verify signature
    is_valid, error_msg = verify_signature(
        provider_lower,
        request_data.timestamp,
        request_data.signature
    )

    if not is_valid:
        raise HTTPException(
            status_code=400,
            detail={
                "error": "invalid_signature",
                "message": error_msg,
                "details": {"timestamp": request_data.timestamp}
            }
        )

    # Generate unique session ID
    session_id = str(uuid.uuid4())

    # Calculate expiration
    expires_at = datetime.utcnow() + timedelta(minutes=settings.session_ttl_minutes)

    # Get client info
    client_ip = request.client.host if request.client else "unknown"
    user_agent = request.headers.get("user-agent", "unknown")

    # Create session in database
    session = OAuthSession(
        session_id=session_id,
        provider=provider_lower,
        created_at=datetime.utcnow(),
        expires_at=expires_at,
        used=False,
        client_ip=client_ip,
        user_agent=user_agent
    )

    db.add(session)
    db.commit()

    # Build callback URL
    callback_url = f"{settings.domain_name}/oauth/callback/{session_id}"

    return RegisterResponse(
        session_id=session_id,
        callback_url=callback_url,
        expires_in=settings.session_ttl_minutes * 60,
        provider=provider_lower
    )


@router.get("/oauth/callback/{session_id}", response_class=HTMLResponse)
async def handle_callback(
    session_id: str,
    code: Optional[str] = Query(None, description="Authorization code from OAuth provider"),
    error: Optional[str] = Query(None, description="Error from OAuth provider"),
    error_description: Optional[str] = Query(None, description="Error description"),
    db: Session = Depends(get_db)
):
    """
    Handle OAuth provider callback

    This endpoint receives the callback from the OAuth provider and redirects
    to the mobile app's deep link scheme with the authorization code.
    """
    # Lookup session
    session = db.query(OAuthSession).filter(
        OAuthSession.session_id == session_id
    ).first()

    if not session:
        return create_error_page(
            "Session Not Found",
            "The OAuth session has expired or does not exist.",
            "Please try connecting your account again from the app."
        )

    # Check if session is valid
    if session.used:
        return create_error_page(
            "Session Already Used",
            "This OAuth session has already been completed.",
            "Please start a new connection from the app."
        )

    if session.is_expired():
        return create_error_page(
            "Session Expired",
            f"This OAuth session expired at {session.expires_at.strftime('%Y-%m-%d %H:%M:%S UTC')}.",
            "Sessions are valid for 10 minutes. Please try again from the app."
        )

    # Mark session as used
    session.used = True
    db.commit()

    # Handle OAuth error
    if error:
        deep_link = f"{settings.deep_link_scheme}?error={error}"
        if error_description:
            deep_link += f"&error_description={error_description}"

        return create_redirect_page(
            "Authorization Failed",
            f"OAuth authorization failed: {error}",
            deep_link,
            is_error=True
        )

    # Handle missing code
    if not code:
        return create_error_page(
            "Missing Authorization Code",
            "No authorization code was provided by the OAuth provider.",
            "This may indicate a problem with the OAuth flow."
        )

    # Build deep link with authorization code
    deep_link = f"{settings.deep_link_scheme}?code={code}&provider={session.provider}"

    return create_redirect_page(
        "Authorization Successful",
        f"You've successfully authorized with {session.provider.title()}!",
        deep_link,
        is_error=False
    )


def create_redirect_page(title: str, message: str, redirect_url: str, is_error: bool = False) -> str:
    """Create HTML redirect page"""
    status_color = "#dc3545" if is_error else "#28a745"
    icon = "❌" if is_error else "✅"

    return f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta http-equiv="refresh" content="2;url={redirect_url}">
        <title>{title}</title>
        <style>
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                display: flex;
                justify-content: center;
                align-items: center;
                min-height: 100vh;
                margin: 0;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            }}
            .container {{
                background: white;
                padding: 3rem;
                border-radius: 1rem;
                box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                text-align: center;
                max-width: 500px;
            }}
            .icon {{
                font-size: 4rem;
                margin-bottom: 1rem;
            }}
            h1 {{
                color: {status_color};
                margin: 0 0 1rem 0;
                font-size: 1.75rem;
            }}
            p {{
                color: #666;
                margin: 1rem 0;
                line-height: 1.6;
            }}
            .redirect-notice {{
                margin-top: 2rem;
                padding: 1rem;
                background: #f8f9fa;
                border-radius: 0.5rem;
                font-size: 0.875rem;
                color: #666;
            }}
            a {{
                color: #667eea;
                text-decoration: none;
                font-weight: 500;
            }}
            a:hover {{
                text-decoration: underline;
            }}
            .spinner {{
                margin: 2rem auto;
                width: 40px;
                height: 40px;
                border: 4px solid #f3f3f3;
                border-top: 4px solid #667eea;
                border-radius: 50%;
                animation: spin 1s linear infinite;
            }}
            @keyframes spin {{
                0% {{ transform: rotate(0deg); }}
                100% {{ transform: rotate(360deg); }}
            }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="icon">{icon}</div>
            <h1>{title}</h1>
            <p>{message}</p>
            <div class="spinner"></div>
            <div class="redirect-notice">
                <p>Redirecting back to PEV PlugShare...</p>
                <p>If you are not redirected automatically, <a href="{redirect_url}">click here</a>.</p>
            </div>
        </div>
    </body>
    </html>
    """


def create_error_page(title: str, message: str, hint: str) -> str:
    """Create HTML error page"""
    return f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>{title}</title>
        <style>
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                display: flex;
                justify-content: center;
                align-items: center;
                min-height: 100vh;
                margin: 0;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            }}
            .container {{
                background: white;
                padding: 3rem;
                border-radius: 1rem;
                box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                text-align: center;
                max-width: 500px;
            }}
            .icon {{
                font-size: 4rem;
                margin-bottom: 1rem;
            }}
            h1 {{
                color: #dc3545;
                margin: 0 0 1rem 0;
                font-size: 1.75rem;
            }}
            p {{
                color: #666;
                margin: 1rem 0;
                line-height: 1.6;
            }}
            .hint {{
                margin-top: 2rem;
                padding: 1rem;
                background: #fff3cd;
                border-left: 4px solid #ffc107;
                border-radius: 0.25rem;
                text-align: left;
            }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="icon">⚠️</div>
            <h1>{title}</h1>
            <p>{message}</p>
            <div class="hint">
                <strong>💡 Tip:</strong> {hint}
            </div>
        </div>
    </body>
    </html>
    """
