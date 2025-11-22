#!/bin/bash
set -e

echo "☁️  Starting Cloudflare Tunnel..."

# Wait for FastAPI to be ready
echo "⏳ Waiting for FastAPI to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8000/health > /dev/null 2>&1; then
        echo "✓ FastAPI is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ FastAPI failed to start within 30 seconds"
        exit 1
    fi
    sleep 1
done

# Start cloudflared with the provided tunnel token
echo "🔗 Connecting to Cloudflare Tunnel..."
exec cloudflared tunnel --no-autoupdate run --token "$CLOUDFLARE_TUNNEL_TOKEN"
