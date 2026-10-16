---
name: deploy-oauth-connector
description: Deploy the PEV PlugShare oauth2 connector (oauth-callback-service) to oracle-cloud. Use when cutting a new service release (vX.Y.Z), upgrading https://oauth.a42r.com, or verifying the connector after a change. Two phases: (1) collect inputs and present a change-set for operator confirmation; (2) build, push, pull, restart, verify autonomously after confirmation.
---

# Deploy oauth connector

Production runs on **oracle-cloud** as rootful podman + systemd + nginx.
Never deploy `latest`; always a pinned `vX.Y.Z` tag.

## Layout (oracle-cloud)

- Unit: `/etc/systemd/system/oauth.service` → container `oauth`
- Ports: loopback `127.0.0.1:18082` (FastAPI), `127.0.0.1:18083` (admin UI)
- Data: `/opt/apps/oauth/data` (SQLite); env: `/opt/apps/oauth/oauth.env`
- nginx: `/etc/nginx/sites-available/oauth.a42r.com`
  (`/admin` → 18083, `/` → 18082); cert `/etc/letsencrypt/live/oauth.a42r.com`
- DNS: Cloudflare proxied A `oauth.a42r.com` → 161.33.93.37

## Phase 1 — plan (ask operator)

1. Read current version: `ssh oracle-cloud 'sudo podman exec oauth cat /app/version.json'`
   and local `oauth-callback-service/version.json`.
2. Decide bump type (major/minor/patch) from the changes; run
   `./bump-version.sh <type>` or set version.json directly.
3. Present a single change-set: new version, notable changes, image tag,
   and that DNS/nginx do NOT change for a plain upgrade. Wait for
   confirmation.

## Phase 2 — execute

```bash
# local machine (from oauth-callback-service/)
./build.sh vX.Y.Z                       # verify image
source ~/.bashrc-envvars
echo "$GH_PAT" | docker login ghcr.io -u githuba42r --password-stdin
docker push ghcr.io/githuba42r/oauth2-redirector:vX.Y.Z

# oracle-cloud
ssh oracle-cloud 'sudo podman pull ghcr.io/githuba42r/oauth2-redirector:vX.Y.Z \
  && sudo systemctl restart oauth'
ssh oracle-cloud 'curl -sf http://127.0.0.1:18082/health'   # version must match
curl -sf https://oauth.a42r.com/health                     # public path
```

**Gotcha**: `systemctl restart` reuses the cached image tag — always
`podman pull` first when re-pushing the same tag.

## Verification

- `/health` (loopback and https) reports the new version.
- `curl -s -o /dev/null -w '%{http_code}' https://oauth.a42r.com/admin/login` → 200.
- `sudo podman exec oauth oauthctl app list` shows `pevplugshare`.
- If auth/API code changed: mint a throwaway token against the loopback
  (`/oauth/token` with an API client) and hit `/api/v1/whoami`.

## Do NOT

- Change the `pevplugshare` shared secret casually — it must match the
  Android app's `secrets.properties` (live secret); rotating it breaks
  released app builds.
- Commit/push version.json with `wip` as git_commit (bump-version.sh
  handles this when the tree is clean).
