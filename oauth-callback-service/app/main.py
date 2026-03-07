"""
Main FastAPI application
"""
import asyncio
import logging
import json
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.models import init_db, cleanup_expired_sessions, seed_sample_application
from app.routers import oauth_router, health_router
from app.utils import RateLimiter


# Custom formatter to include extra fields as JSON
class JSONFormatter(logging.Formatter):
    """Format log records as JSON with extra fields"""

    def format(self, record):
        log_data = {
            "timestamp": self.formatTime(record, self.datefmt),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }

        # Add extra fields - they're stored directly as attributes on the record
        for key in ['session_id', 'provider', 'app_name', 'client_ip', 'error',
                   'callback_url', 'request_hostname', 'has_code', 'has_error',
                   'error_description', 'deep_link_scheme', 'expires_at',
                   'timestamp', 'user_agent', 'expected_scheme', 'provided']:
            if hasattr(record, key):
                log_data[key] = getattr(record, key)

        # Add exception info if present
        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        return json.dumps(log_data)


# Configure logging
root_logger = logging.getLogger()
root_logger.setLevel(settings.log_level)

# Remove any existing handlers
for handler in root_logger.handlers[:]:
    root_logger.removeHandler(handler)

# Add our custom handler
handler = logging.StreamHandler()
if settings.environment == "production":
    handler.setFormatter(JSONFormatter())
else:
    # Use human-readable format for development
    handler.setFormatter(
        logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    )

root_logger.addHandler(handler)
logger = logging.getLogger(__name__)

# Initialize rate limiter
rate_limiter = RateLimiter(
    per_minute=settings.rate_limit_per_minute,
    per_hour=settings.rate_limit_per_hour,
    per_day=settings.rate_limit_per_day
)


async def cleanup_task():
    """Background task to cleanup expired sessions"""
    while True:
        try:
            await asyncio.sleep(settings.cleanup_interval_minutes * 60)
            deleted = cleanup_expired_sessions()
            if deleted > 0:
                logger.info(f"Cleaned up {deleted} expired sessions")
        except Exception as e:
            logger.error(f"Error in cleanup task: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan context manager for startup/shutdown events"""
    # Startup
    logger.info("Starting OAuth Callback Service...")
    init_db()
    logger.info("Database initialized")

    # Seed sample application for development
    if seed_sample_application():
        logger.info("Sample application seeded")
    else:
        logger.info("Sample application already exists")

    # Start cleanup task
    cleanup_task_handle = asyncio.create_task(cleanup_task())
    logger.info(f"Session cleanup task started (interval: {settings.cleanup_interval_minutes} minutes)")

    yield

    # Shutdown
    logger.info("Shutting down OAuth Callback Service...")
    cleanup_task_handle.cancel()
    try:
        await cleanup_task_handle
    except asyncio.CancelledError:
        pass


# Create FastAPI app
app = FastAPI(
    title="OAuth Callback Service",
    description="Generic OAuth 2.0 callback handler for mobile applications",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.get_allowed_origins_list(),
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


@app.middleware("http")
async def logging_and_rate_limit_middleware(request: Request, call_next):
    """Combined logging filter and rate limiting middleware"""
    # Skip logging for health check endpoint to avoid log spam
    is_health_check = request.url.path == "/health"
    
    # Skip rate limiting for health check and docs
    if request.url.path in ["/health", "/", "/docs", "/redoc", "/openapi.json"]:
        response = await call_next(request)
        return response

    # Get client IP
    client_ip = request.client.host if request.client else "unknown"

    # Check rate limit
    allowed, error_msg = rate_limiter.is_allowed(client_ip)

    if not allowed:
        logger.warning(f"Rate limit exceeded for {client_ip}: {error_msg}")
        return JSONResponse(
            status_code=429,
            content={
                "error": "rate_limit_exceeded",
                "message": error_msg,
                "details": rate_limiter.get_stats(client_ip)
            }
        )

    response = await call_next(request)
    return response


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """Custom HTTP exception handler"""
    return JSONResponse(
        status_code=exc.status_code,
        content=exc.detail if isinstance(exc.detail, dict) else {"error": "http_error", "message": str(exc.detail)}
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """General exception handler"""
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "internal_server_error",
            "message": "An unexpected error occurred"
        }
    )


# Include routers
app.include_router(health_router, tags=["Health"])
app.include_router(oauth_router, tags=["OAuth"])

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
        log_level=settings.log_level.lower()
    )
