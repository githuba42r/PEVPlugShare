#!/bin/bash
# Run the OAuth callback service in development mode

# Activate virtual environment if it exists
if [ -d "venv" ]; then
    source venv/bin/activate
fi

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "❌ Error: .env file not found"
    echo "Please copy .env.example to .env and configure it:"
    echo "  cp .env.example .env"
    exit 1
fi

# Run the service
echo "🚀 Starting OAuth Callback Service..."
echo "📖 API docs will be available at: http://localhost:8000/docs"
echo "🏥 Health check: http://localhost:8000/health"
echo ""

python -m app.main
