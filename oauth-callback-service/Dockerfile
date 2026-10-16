FROM python:3.12-slim

# Set working directory
WORKDIR /app

# Install system dependencies including cloudflared, supervisor, and sqlite3
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    curl \
    supervisor \
    sqlite3 \
    && curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb \
    && dpkg -i cloudflared.deb \
    && rm cloudflared.deb \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app ./app

# Copy logging configuration
COPY logging_config.json ./logging_config.json

# Copy version information
COPY version.json ./version.json

# Copy database management script (deprecated shim) and the oauthctl CLI
COPY manage_db.py ./manage_db.py
COPY oauthctl /usr/local/bin/oauthctl

# Copy supervisor configuration
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Copy startup scripts
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
COPY start-cloudflared.sh /app/start-cloudflared.sh
COPY start-fastapi.sh /app/start-fastapi.sh
COPY start-webui.sh /app/start-webui.sh
RUN chmod +x /app/docker-entrypoint.sh /app/start-cloudflared.sh /app/start-fastapi.sh /app/start-webui.sh /usr/local/bin/oauthctl

# Create directories
RUN mkdir -p /app/data /var/log/supervisor /root/.cloudflared

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    DATABASE_URL=sqlite:////app/data/oauth_sessions.db

# Expose port
EXPOSE 8000 8001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the startup script which launches supervisord
CMD ["/app/docker-entrypoint.sh"]
