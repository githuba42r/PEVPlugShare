#!/bin/bash
exec stdbuf -oL -eL python -m flask --app app.webui run --host 0.0.0.0 --port ${WEBUI_PORT:-8001} 2>&1 | stdbuf -oL -eL sed 's/^/[WEBUI] /'
