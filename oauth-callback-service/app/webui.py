"""
Web interface for OAuth application management

This Flask app runs on /admin prefix for Traefik routing.
Access at: http://localhost:8001/admin/ or https://oauth.a42r.com/admin/
"""
from flask import Flask, render_template, request, jsonify, Blueprint, send_from_directory, redirect
from werkzeug.middleware.proxy_fix import ProxyFix
from datetime import datetime
import secrets
import logging
import os

from app.models.database import SessionLocal, Application, OAuthSession

logger = logging.getLogger(__name__)

# Create Flask app with static URL path at /admin/static
app = Flask(__name__, static_url_path='/admin/static')

# Handle proxy headers from Traefik
app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1, x_host=1, x_prefix=1)

# Create blueprint for /admin routes
admin_bp = Blueprint('admin', __name__, url_prefix='/admin')


# Redirect root to /admin for convenience when accessing directly
@app.route('/')
def root():
    """Redirect to admin UI"""
    return redirect('/admin/')


@admin_bp.route('/')
def index():
    """Main dashboard"""
    return render_template('index.html')


@admin_bp.route('/api/applications', methods=['GET'])
def get_applications():
    """Get all applications"""
    db = SessionLocal()
    try:
        apps = db.query(Application).order_by(Application.app_id).all()
        return jsonify([{
            'app_id': app.app_id,
            'name': app.name,
            'allowed_domains': app.allowed_domains,
            'deep_link_scheme': app.deep_link_scheme,
            'is_active': app.is_active,
            'created_at': app.created_at.isoformat() if app.created_at else None,
            'updated_at': app.updated_at.isoformat() if app.updated_at else None,
            'secret_preview': app.shared_secret[:20] + '...' if app.shared_secret else ''
        } for app in apps])
    finally:
        db.close()


@admin_bp.route('/api/applications/<app_id>', methods=['GET'])
def get_application(app_id):
    """Get specific application (includes full secret)"""
    db = SessionLocal()
    try:
        app_obj = db.query(Application).filter(Application.app_id == app_id).first()
        if not app_obj:
            return jsonify({'error': 'Application not found'}), 404
        
        return jsonify({
            'app_id': app_obj.app_id,
            'name': app_obj.name,
            'allowed_domains': app_obj.allowed_domains,
            'deep_link_scheme': app_obj.deep_link_scheme,
            'shared_secret': app_obj.shared_secret,
            'is_active': app_obj.is_active,
            'created_at': app_obj.created_at.isoformat() if app_obj.created_at else None,
            'updated_at': app_obj.updated_at.isoformat() if app_obj.updated_at else None
        })
    finally:
        db.close()


@admin_bp.route('/api/applications', methods=['POST'])
def create_application():
    """Create new application"""
    data = request.json
    
    # Validate required fields
    required_fields = ['app_id', 'name', 'allowed_domains', 'deep_link_scheme']
    for field in required_fields:
        if not data.get(field):
            return jsonify({'error': f'Missing required field: {field}'}), 400
    
    # Generate secret if not provided
    shared_secret = data.get('shared_secret')
    if not shared_secret:
        shared_secret = secrets.token_urlsafe(32)
    
    db = SessionLocal()
    try:
        # Check if app already exists
        existing = db.query(Application).filter(Application.app_id == data['app_id']).first()
        if existing:
            return jsonify({'error': 'Application already exists'}), 409
        
        # Create new application
        app_obj = Application(
            app_id=data['app_id'],
            name=data['name'],
            allowed_domains=data['allowed_domains'],
            deep_link_scheme=data['deep_link_scheme'],
            shared_secret=shared_secret,
            is_active=data.get('is_active', True),
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        
        db.add(app_obj)
        db.commit()
        db.refresh(app_obj)
        
        return jsonify({
            'app_id': app_obj.app_id,
            'name': app_obj.name,
            'allowed_domains': app_obj.allowed_domains,
            'deep_link_scheme': app_obj.deep_link_scheme,
            'shared_secret': app_obj.shared_secret,
            'is_active': app_obj.is_active,
            'created_at': app_obj.created_at.isoformat(),
            'updated_at': app_obj.updated_at.isoformat()
        }), 201
    except Exception as e:
        db.rollback()
        logger.error(f"Error creating application: {e}")
        return jsonify({'error': str(e)}), 500
    finally:
        db.close()


@admin_bp.route('/api/applications/<app_id>', methods=['PUT'])
def update_application(app_id):
    """Update application"""
    data = request.json
    
    db = SessionLocal()
    try:
        app_obj = db.query(Application).filter(Application.app_id == app_id).first()
        if not app_obj:
            return jsonify({'error': 'Application not found'}), 404
        
        # Update fields
        if 'name' in data:
            app_obj.name = data['name']
        if 'allowed_domains' in data:
            app_obj.allowed_domains = data['allowed_domains']
        if 'deep_link_scheme' in data:
            app_obj.deep_link_scheme = data['deep_link_scheme']
        if 'shared_secret' in data:
            app_obj.shared_secret = data['shared_secret']
        if 'is_active' in data:
            app_obj.is_active = data['is_active']
        
        app_obj.updated_at = datetime.utcnow()
        
        db.commit()
        db.refresh(app_obj)
        
        return jsonify({
            'app_id': app_obj.app_id,
            'name': app_obj.name,
            'allowed_domains': app_obj.allowed_domains,
            'deep_link_scheme': app_obj.deep_link_scheme,
            'shared_secret': app_obj.shared_secret,
            'is_active': app_obj.is_active,
            'created_at': app_obj.created_at.isoformat(),
            'updated_at': app_obj.updated_at.isoformat()
        })
    except Exception as e:
        db.rollback()
        logger.error(f"Error updating application: {e}")
        return jsonify({'error': str(e)}), 500
    finally:
        db.close()


@admin_bp.route('/api/applications/<app_id>', methods=['DELETE'])
def delete_application(app_id):
    """Delete application"""
    db = SessionLocal()
    try:
        app_obj = db.query(Application).filter(Application.app_id == app_id).first()
        if not app_obj:
            return jsonify({'error': 'Application not found'}), 404
        
        db.delete(app_obj)
        db.commit()
        
        return jsonify({'success': True}), 200
    except Exception as e:
        db.rollback()
        logger.error(f"Error deleting application: {e}")
        return jsonify({'error': str(e)}), 500
    finally:
        db.close()


@admin_bp.route('/api/applications/<app_id>/toggle', methods=['POST'])
def toggle_application(app_id):
    """Enable/disable application"""
    db = SessionLocal()
    try:
        app_obj = db.query(Application).filter(Application.app_id == app_id).first()
        if not app_obj:
            return jsonify({'error': 'Application not found'}), 404
        
        app_obj.is_active = not app_obj.is_active
        app_obj.updated_at = datetime.utcnow()
        
        db.commit()
        db.refresh(app_obj)
        
        return jsonify({
            'app_id': app_obj.app_id,
            'is_active': app_obj.is_active
        })
    except Exception as e:
        db.rollback()
        logger.error(f"Error toggling application: {e}")
        return jsonify({'error': str(e)}), 500
    finally:
        db.close()


@admin_bp.route('/api/applications/<app_id>/regenerate-secret', methods=['POST'])
def regenerate_secret(app_id):
    """Regenerate shared secret for application"""
    db = SessionLocal()
    try:
        app_obj = db.query(Application).filter(Application.app_id == app_id).first()
        if not app_obj:
            return jsonify({'error': 'Application not found'}), 404
        
        app_obj.shared_secret = secrets.token_urlsafe(32)
        app_obj.updated_at = datetime.utcnow()
        
        db.commit()
        db.refresh(app_obj)
        
        return jsonify({
            'app_id': app_obj.app_id,
            'shared_secret': app_obj.shared_secret
        })
    except Exception as e:
        db.rollback()
        logger.error(f"Error regenerating secret: {e}")
        return jsonify({'error': str(e)}), 500
    finally:
        db.close()


@admin_bp.route('/api/sessions', methods=['GET'])
def get_sessions():
    """Get recent sessions"""
    limit = request.args.get('limit', 50, type=int)
    app_id = request.args.get('app_id')
    
    db = SessionLocal()
    try:
        query = db.query(OAuthSession).order_by(OAuthSession.created_at.desc())
        
        if app_id:
            query = query.filter(OAuthSession.app_id == app_id)
        
        sessions = query.limit(limit).all()
        
        return jsonify([{
            'session_id': session.session_id,
            'app_id': session.app_id,
            'provider': session.provider,
            'created_at': session.created_at.isoformat() if session.created_at else None,
            'expires_at': session.expires_at.isoformat() if session.expires_at else None,
            'used': session.used,
            'client_ip': session.client_ip,
            'is_expired': session.is_expired()
        } for session in sessions])
    finally:
        db.close()


# Register blueprint
app.register_blueprint(admin_bp)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8001, debug=True)
