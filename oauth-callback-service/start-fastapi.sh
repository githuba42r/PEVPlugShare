#!/bin/bash
exec stdbuf -oL -eL uvicorn app.main:app --host 0.0.0.0 --port 8000 2>&1 | stdbuf -oL -eL sed 's/^/[FASTAPI] /'
