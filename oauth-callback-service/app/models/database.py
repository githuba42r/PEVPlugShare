"""
Database models for OAuth session management
"""
from sqlalchemy import Column, String, Integer, Boolean, DateTime, create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime, timedelta
from app.config import settings

Base = declarative_base()


class OAuthSession(Base):
    """OAuth session model for tracking callback state"""
    __tablename__ = "oauth_sessions"

    session_id = Column(String(36), primary_key=True, index=True)
    provider = Column(String(50), nullable=False, index=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    expires_at = Column(DateTime, nullable=False, index=True)
    used = Column(Boolean, default=False, nullable=False)
    client_ip = Column(String(45), nullable=True)  # IPv4 or IPv6
    user_agent = Column(String(500), nullable=True)

    def is_expired(self) -> bool:
        """Check if session has expired"""
        return datetime.utcnow() > self.expires_at

    def is_valid(self) -> bool:
        """Check if session is valid (not used and not expired)"""
        return not self.used and not self.is_expired()


# Database setup
engine = create_engine(
    settings.database_url,
    connect_args={"check_same_thread": False} if "sqlite" in settings.database_url else {},
    echo=settings.log_level == "DEBUG"
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def init_db():
    """Initialize database tables"""
    Base.metadata.create_all(bind=engine)


def get_db():
    """Dependency for getting database session"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def cleanup_expired_sessions():
    """Remove expired sessions from database"""
    db = SessionLocal()
    try:
        cutoff_time = datetime.utcnow()
        deleted = db.query(OAuthSession).filter(
            OAuthSession.expires_at < cutoff_time
        ).delete()
        db.commit()
        return deleted
    finally:
        db.close()
