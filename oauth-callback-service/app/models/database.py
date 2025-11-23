"""
Database models for OAuth session management
"""
from sqlalchemy import Column, String, Integer, Boolean, DateTime, Text, create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from datetime import datetime, timedelta
from app.config import settings

Base = declarative_base()


class Application(Base):
    """Application model for managing multiple client applications"""
    __tablename__ = "applications"

    app_id = Column(String(50), primary_key=True, index=True)
    name = Column(String(200), nullable=False)
    allowed_domains = Column(Text, nullable=False)  # Comma-separated list
    deep_link_scheme = Column(String(200), nullable=False)
    shared_secret = Column(String(500), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)

    def get_allowed_domains_list(self):
        """Parse comma-separated domains into list"""
        return [domain.strip() for domain in self.allowed_domains.split(",")]

    def is_domain_allowed(self, domain: str) -> bool:
        """Check if a domain is allowed for this application"""
        allowed = self.get_allowed_domains_list()
        return domain in allowed


class OAuthSession(Base):
    """OAuth session model for tracking callback state"""
    __tablename__ = "oauth_sessions"

    session_id = Column(String(36), primary_key=True, index=True)
    app_id = Column(String(50), nullable=False, index=True)  # Foreign key to applications
    provider = Column(String(50), nullable=False, index=True)
    deep_link_url = Column(String(500), nullable=False)  # Client-provided deep link URL
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


def seed_sample_application():
    """Seed a sample application for development/testing"""
    db = SessionLocal()
    try:
        # Check if sample app already exists
        existing = db.query(Application).filter(
            Application.app_id == "pevplugshare"
        ).first()

        if not existing:
            sample_app = Application(
                app_id="pevplugshare",
                name="PEV PlugShare",
                allowed_domains="pevplugshare.a42r.com,pevplugshare-prod.a42r.com,localhost:8000",
                deep_link_scheme="pevplugshare://oauth/callback",
                shared_secret="CHANGE_THIS_SECRET",  # Should be changed in production
                is_active=True
            )
            db.add(sample_app)
            db.commit()
            return True
        return False
    finally:
        db.close()


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
