#!/bin/bash
set -e

echo "[CLOUDFLARED] ☁️  Starting Cloudflare Tunnel..."

# Wait for FastAPI to be ready
echo "[CLOUDFLARED] ⏳ Waiting for FastAPI to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8000/health > /dev/null 2>&1; then
        echo "[CLOUDFLARED] ✓ FastAPI is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "[CLOUDFLARED] ❌ FastAPI failed to start within 30 seconds"
        exit 1
    fi
    sleep 1
done

# Start cloudflared with the provided tunnel token
echo "[CLOUDFLARED] 🔗 Connecting to Cloudflare Tunnel..."
exec stdbuf -oL -eL cloudflared tunnel --no-autoupdate run --token "$CLOUDFLARE_TUNNEL_TOKEN" 2>&1 | stdbuf -oL -eL sed 's/^/[CLOUDFLARED] /'
