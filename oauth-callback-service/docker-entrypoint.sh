#!/bin/bash
set -e

echo "🚀 Starting OAuth Callback Service..."

# Check if Cloudflare Tunnel is enabled
if [ "${CLOUDFLARE_TUNNEL_ENABLED:-false}" = "true" ]; then
    echo "☁️  Cloudflare Tunnel is ENABLED"

    # Validate required Cloudflare environment variables
    if [ -z "$CLOUDFLARE_TUNNEL_TOKEN" ]; then
        echo "❌ ERROR: CLOUDFLARE_TUNNEL_TOKEN is required when CLOUDFLARE_TUNNEL_ENABLED=true"
        exit 1
    fi

    echo "✓ Cloudflare Tunnel configuration validated"
    echo "  Tunnel will connect on startup..."

    # Enable cloudflared in supervisor
    sed -i 's/autostart=false/autostart=true/' /etc/supervisor/conf.d/supervisord.conf
else
    echo "☁️  Cloudflare Tunnel is DISABLED (set CLOUDFLARE_TUNNEL_ENABLED=true to enable)"
fi

# Display configuration
echo ""
echo "📋 Configuration:"
echo "  PORT: ${PORT:-8000}"
echo "  DOMAIN_NAME: ${DOMAIN_NAME:-https://localhost:8000}"
echo "  SESSION_TTL: ${SESSION_TTL_MINUTES:-10} minutes"
echo "  CLOUDFLARE_TUNNEL: ${CLOUDFLARE_TUNNEL_ENABLED:-false}"
echo ""

# Start supervisord
echo "🎯 Starting supervisord..."
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf
