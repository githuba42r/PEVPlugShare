# Central Location Server - Feature Specification

## Overview

A self-hosted backend server that provides centralised storage for EV charging locations and images, tied to Google accounts. Users can selectively sync locations to the server, connect with friends (with mutual authorisation), create friend groups, and share locations. Shared locations support a collaborative editing model where recipients can suggest changes back to the author.

## Architecture

### System Components

```
+-------------------+         +-------------------+         +------------------+
|  Android App      | <-----> |  Backend Server    | <-----> |  PostgreSQL/     |
|  (PEVPlugShare)   |  REST   |  Node.js (Express) |         |  MySQL           |
|                   |  API    |  TypeScript        | <-----> +------------------+
+-------------------+         +-------------------+         |  File Storage    |
                                |             |             |  (local disk /   |
                                v             v             |   S3-compatible) |
                          Google OAuth   Redis (optional)   +------------------+
                                |
                                v
                          Admin Console
                          (Express + EJS/HTML)
```

### Tech Stack

- **Server**: Node.js (Express) with TypeScript
- **Database**: PostgreSQL or MySQL, via abstracted repository layer — no database-specific extensions (no PostGIS)
- **Future Database**: AWS DynamoDB (architecture must support migration)
- **Caching**: Redis (optional) — session store, query cache, rate limiting
- **Authentication**: Google OAuth 2.0 / Google Sign-In (app and admin console)
- **API**: REST with JSON payloads
- **File Storage**: Local filesystem initially; abstracted for S3 migration
- **Admin Console**: Server-rendered HTML (Express + EJS or similar), Google OAuth authenticated, role-gated
- **Deployment**: Single Docker image pushed to a registry; `docker run` to start. No Docker Compose for the build process. External PostgreSQL/MySQL and Redis are provided via environment variables.
- **Future Deployment**: AWS Lambda/FaaS migration

### Design Principles

1. **Abstracted data layer** — All database access through a repository interface. Implementations are swappable (PostgreSQL, MySQL, DynamoDB) without changing business logic.
2. **Stateless request handling** — No server-side session state beyond JWT tokens and optional Redis cache. Enables horizontal scaling and Lambda deployment.
3. **Handler/service/repository separation** — Route handlers are thin (parse request, call service, return response). Business logic lives in services. Data access lives in repositories.
4. **FaaS-ready structure** — Each route handler can be extracted as an individual Lambda function. Shared business logic stays in service modules.
5. **Simple geospatial queries** — Location lookups use plain latitude/longitude with bounding-box queries (`BETWEEN` clauses). No database-specific geospatial extensions, ensuring portability across PostgreSQL, MySQL, and DynamoDB.

### Project Structure (Node.js/Express/TypeScript)

```
server/
  src/
    app.ts                      # Express app factory
    server.ts                   # HTTP server entrypoint
    lambda.ts                   # AWS Lambda entrypoint (wraps Express)
    config.ts                   # Environment-based configuration

    auth/
      google-oauth.ts           # Google token verification
      middleware.ts             # JWT auth middleware
      admin-middleware.ts       # Admin role check middleware

    routes/
      users.ts                  # User profile endpoints
      locations.ts              # Location CRUD endpoints
      photos.ts                 # Photo upload/download endpoints
      friends.ts                # Friendship management endpoints
      groups.ts                 # Friend group endpoints
      sharing.ts                # Location sharing endpoints
      suggestions.ts            # Change suggestion endpoints

    admin/
      routes.ts                 # Admin console routes (server-rendered)
      views/                    # EJS templates
        layout.ejs
        login.ejs
        dashboard.ejs
        users.ejs
        user-detail.ejs
        groups.ejs
        locations.ejs
        location-detail.ejs
        rate-limits.ejs
        bans.ejs

    services/
      user-service.ts
      location-service.ts
      photo-service.ts
      friend-service.ts
      group-service.ts
      sharing-service.ts
      suggestion-service.ts
      rate-limit-service.ts
      ban-service.ts

    repositories/
      interfaces.ts             # Repository interfaces (TypeScript)
      postgres/                 # PostgreSQL implementations (pg/knex)
        user-repo.ts
        location-repo.ts
        photo-repo.ts
        friend-repo.ts
        group-repo.ts
        share-repo.ts
        suggestion-repo.ts
        rate-limit-repo.ts
        ban-repo.ts
      mysql/                    # MySQL implementations (knex)
        ...                     # Same files, different SQL dialect
      dynamodb/                 # DynamoDB implementations (future)
        ...

    storage/
      interfaces.ts
      local-storage.ts
      s3-storage.ts             # Future

    cache/
      interfaces.ts
      redis-cache.ts
      memory-cache.ts

    models/                     # Domain types (plain interfaces/types)
      user.ts
      location.ts
      photo.ts
      friendship.ts
      group.ts
      share.ts
      suggestion.ts
      rate-limit.ts
      ban.ts

  package.json
  tsconfig.json
```

### Docker Deployment

Single self-contained Docker image. PostgreSQL/MySQL and Redis are external services configured via environment variables.

**Dockerfile:**
```dockerfile
FROM node:20-slim
WORKDIR /app
COPY package*.json .
RUN npm ci --production
COPY . .
EXPOSE 8080
CMD ["node", "dist/server.js"]
```

**Build and run:**
```bash
# Build
docker build -t pevplugshare-server:latest .

# Push to registry
docker tag pevplugshare-server:latest registry.example.com/pevplugshare-server:latest
docker push registry.example.com/pevplugshare-server:latest

# Run (all external services via env vars)
docker run -d \
  --name pevplugshare-server \
  -p 8080:8080 \
  -e DB_BACKEND=postgres \
  -e DATABASE_URL=postgresql://user:pass@db-host:5432/pevplugshare \
  -e REDIS_URL=redis://redis-host:6379/0 \
  -e GOOGLE_CLIENT_ID=your-google-client-id \
  -e JWT_SECRET=your-jwt-secret \
  -e STORAGE_PATH=/data/photos \
  -v /host/photo/storage:/data/photos \
  pevplugshare-server:latest
```

**Environment variables:**
| Variable | Required | Description |
|---|---|---|
| `DB_BACKEND` | Yes | `postgres` or `mysql` (future: `dynamodb`) |
| `DATABASE_URL` | Yes | Full connection string |
| `REDIS_URL` | No | Redis connection string (omit to use in-memory cache) |
| `GOOGLE_CLIENT_ID` | Yes | Google OAuth client ID for token verification |
| `JWT_SECRET` | Yes | Secret for signing JWT tokens (signs server-issued JWTs that the app sends with every request to prove identity) |
| `STORAGE_PATH` | Yes | Path for photo file storage (mount a volume) |
| `LOG_LEVEL` | No | `debug`, `info`, `warn`, `error` (default: `info`) |
| `MAX_UPLOAD_SIZE_MB` | No | Max photo upload size in MB (default: `5`) |
| `ADMIN_SETUP_EMAIL` | No | Google email to auto-grant admin on first login (bootstrap) |

**Database migrations** run automatically on container start (or via a separate `docker run ... migrate` command).

---

### Repository Interface Pattern

The core abstraction enabling database migration. Example (TypeScript):

```typescript
// repositories/interfaces.ts

export interface LocationRepository {
  create(location: CreateLocationInput): Promise<Location>;
  getById(id: string): Promise<Location | null>;
  getByOwner(ownerId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  update(id: string, fields: Partial<Location>): Promise<Location>;
  delete(id: string): Promise<boolean>;
  getSharedWithUser(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  getNearby(lat: number, lon: number, radiusKm: number, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
}
```

Nearby queries use bounding-box calculation — no geospatial extensions:

```typescript
// Simple bounding-box query (works on any SQL database)
async getNearby(lat: number, lon: number, radiusKm: number, cursor?: string, limit: number = 20) {
  const latDelta = radiusKm / 111.0; // ~111km per degree latitude
  const lonDelta = radiusKm / (111.0 * Math.cos(lat * Math.PI / 180));

  const rows = await this.db.query(
    `SELECT * FROM server_locations
     WHERE latitude BETWEEN $1 AND $2
       AND longitude BETWEEN $3 AND $4
     ORDER BY id LIMIT $5`,
    [lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta, limit]
  );
  // Post-filter with Haversine for accurate radius, then apply cursor pagination
}
```

Repository selection is configuration-driven:

```typescript
// config.ts
export const DB_BACKEND = process.env.DB_BACKEND || 'postgres'; // postgres | mysql | dynamodb
```

### Redis Caching Strategy

Optional layer, gracefully degrades when unavailable:

- **Session/auth tokens**: Cache validated Google tokens (short TTL)
- **User profiles**: Cache frequently accessed user data
- **Friend lists**: Cache friend relationships (invalidate on change)
- **Shared location lists**: Cache per-user (invalidate on share/unshare)
- **Rate limiting**: Request counters per user/group/global

### AWS Lambda Migration Path

The server is structured so migration to Lambda is incremental:

1. **Phase 1 (current)**: Single Express process, all routes in one app
2. **Phase 2**: Deploy the same app behind API Gateway using `serverless-http` — zero code changes
3. **Phase 3**: Split hot-path routes into individual Lambda functions for independent scaling
4. **Phase 4**: Swap PostgreSQL/MySQL repository implementations for DynamoDB

---

## Data Model

### Users

```
users
  id              UUID PRIMARY KEY
  google_id       VARCHAR UNIQUE NOT NULL    -- Google account subject ID
  email           VARCHAR UNIQUE NOT NULL
  display_name    VARCHAR NOT NULL
  avatar_url      VARCHAR
  is_admin        BOOLEAN DEFAULT FALSE      -- admin console access
  is_banned       BOOLEAN DEFAULT FALSE       -- banned from using the service
  ban_reason      VARCHAR                     -- optional reason for ban
  banned_at       TIMESTAMP
  banned_by       UUID REFERENCES users(id)   -- admin who banned them
  created_at      TIMESTAMP
  last_seen_at    TIMESTAMP
```

### Banned Identifiers

Prevents banned users from re-registering with new accounts. Checked during Google OAuth login.

```
banned_identifiers
  id              UUID PRIMARY KEY
  identifier_type VARCHAR NOT NULL           -- EMAIL | GOOGLE_ID
  identifier      VARCHAR NOT NULL           -- the banned email or Google subject ID
  reason          VARCHAR
  banned_by       UUID REFERENCES users(id)
  created_at      TIMESTAMP

  UNIQUE(identifier_type, identifier)
```

### Server Locations

```
server_locations
  id              UUID PRIMARY KEY
  owner_id        UUID REFERENCES users(id)
  name            VARCHAR NOT NULL
  latitude        DOUBLE NOT NULL
  longitude       DOUBLE NOT NULL
  description     TEXT
  additional_info TEXT
  opening_hours   VARCHAR
  socket_types    VARCHAR              -- comma-separated: AS3112,TYPE2,...
  gpo_count       INT
  has_fee         BOOLEAN
  charging_costs  VARCHAR
  public_private  VARCHAR              -- PUBLIC | PRIVATE
  provider_name   VARCHAR
  provider_website VARCHAR
  business_name   VARCHAR
  street_address  VARCHAR
  suburb          VARCHAR
  state           VARCHAR
  postcode        VARCHAR
  color           VARCHAR
  icon            VARCHAR
  created_at      TIMESTAMP
  updated_at      TIMESTAMP
  version         INT DEFAULT 1        -- incremented on each update
```

### Server Location Photos

Photos are pre-processed on the phone before upload (downsampled, resized, compressed). The server stores only the optimised versions — no server-side image processing required.

```
server_location_photos
  id              UUID PRIMARY KEY
  location_id     UUID REFERENCES server_locations(id) ON DELETE CASCADE
  file_path       VARCHAR NOT NULL     -- relative path in file storage (optimised image)
  thumbnail_path  VARCHAR              -- server-stored thumbnail
  caption         VARCHAR
  latitude        DOUBLE               -- EXIF GPS
  longitude       DOUBLE
  altitude        DOUBLE
  bearing         FLOAT
  timestamp       BIGINT
  exif_data       TEXT                 -- JSON
  width           INT                  -- pixel dimensions of optimised image
  height          INT
  file_size       BIGINT               -- size of optimised image
  mime_type       VARCHAR DEFAULT 'image/jpeg'
  uploaded_at     TIMESTAMP
```

### Friendships

```
friendships
  id              UUID PRIMARY KEY
  requester_id    UUID REFERENCES users(id)
  addressee_id    UUID REFERENCES users(id)
  status          VARCHAR NOT NULL     -- PENDING | ACCEPTED | DECLINED | BLOCKED
  requested_at    TIMESTAMP
  responded_at    TIMESTAMP

  UNIQUE(requester_id, addressee_id)
```

### Friend Groups

```
friend_groups
  id              UUID PRIMARY KEY
  owner_id        UUID REFERENCES users(id)
  name            VARCHAR NOT NULL
  description     VARCHAR
  created_at      TIMESTAMP
  updated_at      TIMESTAMP
```

```
friend_group_members
  group_id        UUID REFERENCES friend_groups(id) ON DELETE CASCADE
  user_id         UUID REFERENCES users(id)
  added_at        TIMESTAMP

  PRIMARY KEY(group_id, user_id)
```

### Location Sharing

```
location_shares
  id              UUID PRIMARY KEY
  location_id     UUID REFERENCES server_locations(id) ON DELETE CASCADE
  shared_by       UUID REFERENCES users(id)       -- always the location owner
  shared_with_user  UUID REFERENCES users(id)      -- NULL if shared with group
  shared_with_group UUID REFERENCES friend_groups(id) -- NULL if shared with user
  shared_at       TIMESTAMP

  -- Either shared_with_user or shared_with_group must be set, not both
```

### Change Suggestions

```
change_suggestions
  id              UUID PRIMARY KEY
  location_id     UUID REFERENCES server_locations(id) ON DELETE CASCADE
  suggested_by    UUID REFERENCES users(id)
  status          VARCHAR NOT NULL     -- PENDING | ACCEPTED | REJECTED | WITHDRAWN
  changes_json    TEXT NOT NULL        -- JSON diff of proposed field changes
  message         VARCHAR              -- optional note from suggester
  response_msg    VARCHAR              -- optional note from owner on accept/reject
  created_at      TIMESTAMP
  responded_at    TIMESTAMP
```

### Rate Limits

Tiered rate limit configuration with precedence: **user > group > global**. A user-specific limit overrides a group limit, which overrides the global default.

```
rate_limits
  id              UUID PRIMARY KEY
  scope           VARCHAR NOT NULL     -- GLOBAL | GROUP | USER
  scope_id        UUID                 -- NULL for GLOBAL, group_id for GROUP, user_id for USER
  endpoint        VARCHAR NOT NULL     -- endpoint pattern (e.g. "POST /api/locations", "*" for all)
  max_requests    INT NOT NULL         -- max requests per window
  window_seconds  INT NOT NULL         -- time window in seconds
  created_at      TIMESTAMP
  updated_at      TIMESTAMP

  UNIQUE(scope, scope_id, endpoint)
```

**Resolution logic:**
1. Check for a user-specific rate limit for this endpoint
2. If none, check for rate limits on any group the user belongs to (most restrictive wins)
3. If none, apply the global rate limit for this endpoint
4. If no matching rate limit exists, allow the request (no limit)

**Default global rate limits** (created on first run):
| Endpoint | Max Requests | Window |
|---|---|---|
| `POST /api/locations` | 100 | 3600s (1 hour) |
| `POST /api/locations/*/photos` | 200 | 3600s |
| `POST /api/friends/request` | 50 | 3600s |
| `*` (catch-all) | 1000 | 3600s |

---

## API Endpoints

All list endpoints use **cursor-based pagination**:
- Request: `?cursor=<opaque_string>&limit=<number>` (default limit: 20, max: 100)
- Response includes: `{ data: [...], pagination: { cursor: "next_cursor_value" | null, hasMore: boolean } }`
- `cursor` is null or omitted for the first page
- When `hasMore` is false, there are no more results

### Authentication
- `POST /auth/google` — Exchange Google ID token for server JWT
  - Checks `banned_identifiers` table — returns 403 if email or Google ID is banned
  - Creates user record on first login
  - If `ADMIN_SETUP_EMAIL` matches and no admins exist, grants admin role

### User Profile
- `GET /api/users/me` — Get current user profile
- `GET /api/users/search?q={email_or_name}` — Search users to add as friends (paginated)

### Locations
- `POST /api/locations` — Upload a location to the server
- `PUT /api/locations/{id}` — Update an owned location
- `DELETE /api/locations/{id}` — Delete an owned location
- `GET /api/locations` — List own server locations (paginated)
- `GET /api/locations/{id}` — Get location detail with photos
- `GET /api/locations/shared-with-me` — List locations shared with the current user (paginated)
- `GET /api/locations/nearby?lat={lat}&lon={lon}&radius={km}` — Find locations within radius (paginated, bounding-box query)

### Location Photos
- `POST /api/locations/{id}/photos` — Upload pre-processed photo (multipart, phone handles downsampling)
- `DELETE /api/locations/{id}/photos/{photoId}` — Delete a photo
- `GET /api/photos/{photoId}/image` — Download optimised image (with ETag/Last-Modified for caching)
- `GET /api/photos/{photoId}/thumbnail` — Download thumbnail (with ETag/Last-Modified for caching)

### Friendships
- `POST /api/friends/request` — Send friend request (by email or user ID)
- `GET /api/friends/requests/incoming` — List pending incoming requests (paginated)
- `GET /api/friends/requests/outgoing` — List pending outgoing requests (paginated)
- `PUT /api/friends/requests/{id}/accept` — Accept a friend request
- `PUT /api/friends/requests/{id}/decline` — Decline a friend request
- `DELETE /api/friends/{userId}` — Remove a friend
- `GET /api/friends` — List all accepted friends (paginated)

### Friend Groups
- `POST /api/groups` — Create a group
- `PUT /api/groups/{id}` — Update group name/description
- `DELETE /api/groups/{id}` — Delete a group
- `GET /api/groups` — List own groups (paginated)
- `POST /api/groups/{id}/members` — Add friend(s) to group
- `DELETE /api/groups/{id}/members/{userId}` — Remove member from group

### Sharing
- `POST /api/locations/{id}/share` — Share location with user or group
- `DELETE /api/locations/{id}/share/{shareId}` — Revoke a share
- `GET /api/locations/{id}/shares` — List who a location is shared with (paginated)

### Change Suggestions
- `POST /api/locations/{id}/suggestions` — Suggest changes to a shared location
- `GET /api/suggestions/incoming` — List suggestions on owned locations (paginated)
- `GET /api/suggestions/outgoing` — List own suggestions on others' locations (paginated)
- `PUT /api/suggestions/{id}/accept` — Accept and integrate suggestion
- `PUT /api/suggestions/{id}/reject` — Reject suggestion
- `PUT /api/suggestions/{id}/withdraw` — Withdraw own suggestion

---

## Admin Console

A server-rendered web interface for managing users, groups, locations, bans, and rate limits. Authenticated via Google OAuth (same `GOOGLE_CLIENT_ID`), restricted to users with `is_admin = true`.

### Access Control

- Admin console is served at `/admin/*` on the same Express server
- Google OAuth login flow (redirects to Google, returns with token)
- Server verifies the Google token, looks up the user, checks `is_admin`
- If not admin, returns 403 with "Access denied — admin rights required"
- **Bootstrap**: If `ADMIN_SETUP_EMAIL` env var is set and no admin users exist, the first matching login is auto-granted admin

### Admin Console Screens

#### Dashboard (`/admin`)
- Total users, locations, photos, active shares
- Recent activity log (last 50 actions)
- System health (DB connection, Redis status, storage usage)

#### User Management (`/admin/users`)
- Paginated user list with search (by email, display name)
- User detail view: profile info, locations count, friends count, groups, shares
- **Grant/revoke admin** toggle
- **Ban user** — sets `is_banned`, records reason and admin who banned
- **Unban user** — clears ban flag
- View user's locations (links to location detail)

#### Ban Management (`/admin/bans`)
- List all banned users and banned identifiers
- **Ban email address** — add to `banned_identifiers` (prevents re-registration)
- **Ban Google account ID** — add to `banned_identifiers`
- **Remove ban** — remove identifier from banned list
- Banning a user also adds their email + Google ID to `banned_identifiers` automatically

#### Group Management (`/admin/groups`)
- Paginated group list with search
- Group detail: owner, members list, shared locations count
- Delete group (admin override)

#### Location Management (`/admin/locations`)
- Paginated location list with search (by name, owner email)
- Location detail: all fields, photos, share list, suggestions
- Delete location (admin override)
- View location photos

#### Rate Limit Management (`/admin/rate-limits`)
- View and edit global rate limits
- View and edit group-specific rate limits
- View and edit user-specific rate limits
- Create new rate limit rules
- Delete rate limit rules
- Show effective rate limit for a specific user (resolved through precedence)

### Admin API Endpoints

Admin endpoints are under `/admin/api/*` and require `is_admin = true`:

- `GET /admin/api/users` — List users (paginated, searchable)
- `GET /admin/api/users/{id}` — User detail
- `PUT /admin/api/users/{id}/admin` — Grant/revoke admin (`{ isAdmin: boolean }`)
- `PUT /admin/api/users/{id}/ban` — Ban user (`{ reason: string }`)
- `PUT /admin/api/users/{id}/unban` — Unban user
- `GET /admin/api/bans` — List banned identifiers
- `POST /admin/api/bans` — Add banned identifier (`{ type: "EMAIL"|"GOOGLE_ID", identifier: string, reason: string }`)
- `DELETE /admin/api/bans/{id}` — Remove banned identifier
- `GET /admin/api/groups` — List all groups (paginated)
- `GET /admin/api/groups/{id}` — Group detail with members
- `DELETE /admin/api/groups/{id}` — Delete group
- `GET /admin/api/locations` — List all locations (paginated, searchable)
- `GET /admin/api/locations/{id}` — Location detail
- `DELETE /admin/api/locations/{id}` — Delete location
- `GET /admin/api/rate-limits` — List all rate limit rules
- `POST /admin/api/rate-limits` — Create rate limit rule
- `PUT /admin/api/rate-limits/{id}` — Update rate limit rule
- `DELETE /admin/api/rate-limits/{id}` — Delete rate limit rule
- `GET /admin/api/rate-limits/resolve/{userId}` — Show effective limits for a user
- `GET /admin/api/stats` — Dashboard statistics

---

## Android App Changes

### New Screens / UI

1. **Server Account Screen** — Google Sign-In, connection status, account info
2. **Location Sync Controls** — Per-location toggle to sync to server (in edit screen)
3. **Bulk Sync Screen** — Select multiple locations to upload/sync
4. **Friends Screen** — List friends, search/add users, manage requests
5. **Friend Groups Screen** — Create/manage groups, add/remove members
6. **Share Location Dialog** — Pick friends or groups to share with
7. **Shared With Me Screen** — Browse locations shared by friends
8. **Suggestion Composer** — Edit a shared location and submit changes as a suggestion
9. **Incoming Suggestions Screen** — Review, accept/reject suggestions on owned locations

### Data Layer Changes

- New `ServerApiService` (Retrofit) for all backend endpoints
- New `ServerSyncRepository` to manage upload/download of locations and photos
- Extend `LocationEntity` with:
  - `serverId: String?` — UUID on the server (null = local only)
  - `serverVersion: Int?` — last known server version
  - `serverSyncStatus: String` — LOCAL_ONLY | SYNCED | MODIFIED | CONFLICT
- Google Sign-In integration (Google Identity Services library)

### Photo Processing (Client-Side)

Photos are processed on the phone before upload to optimise bandwidth and storage:

- **Downsampling**: Resize to a max dimension (e.g., 1920px longest edge) before upload
- **Compression**: JPEG quality reduced for transmission (e.g., 75-80%)
- **Thumbnail generation**: App generates and uploads a separate thumbnail (e.g., 400px)
- **Metadata preservation**: EXIF GPS, bearing, and timestamp extracted before resize and sent as structured fields
- **No server-side processing**: Server stores exactly what the phone sends

### Sync Behaviour

- **Upload**: User explicitly selects locations to sync (no automatic upload)
- **Download**: Shared locations appear in a separate "Shared With Me" section
- **Conflict detection**: Compare local `serverVersion` against server version on sync
- **Photos (owned locations)**: Pre-processed on phone, uploaded alongside location data
- **Photos (shared locations)**: NOT downloaded with the location. Images are loaded on-demand from the server and cached locally on the device. The app treats shared location photos as remote resources with local caching (LRU eviction, ETag-based revalidation). This avoids unnecessary storage consumption and large initial downloads when receiving shared locations.

---

## Collaborative Editing Flow

```
1. Author creates location, uploads to server
2. Author shares location with Friend (or Group)
3. Friend sees location in "Shared With Me"
4. Friend copies to local device for viewing
5. Friend makes edits locally, submits as "Change Suggestion"
6. Author receives suggestion notification
7. Author reviews diff, accepts or rejects
8. If accepted: server location updated, version incremented
9. All users with access see updated location on next sync
```

---

## Security & Privacy

- All API endpoints require authentication (JWT from Google OAuth)
- Users can only modify their own locations
- Friend requests require explicit acceptance before any data sharing
- Revoking a share immediately removes access
- Location data is only visible to owner + explicitly shared users
- Server stores Google ID and email only (no passwords)
- Banned users and identifiers are checked at login time
- Admin actions are logged with the admin user who performed them
- Rate limiting prevents abuse (tiered: user > group > global)

---

## Implementation Phases

### Phase 1: Server Foundation & Data Layer
- Project scaffolding (Express with TypeScript)
- Abstract repository interfaces and PostgreSQL/MySQL implementations
- Abstract file storage interface and local filesystem implementation
- Database schema and migrations (Knex)
- Optional Redis cache layer with in-memory fallback
- Configuration system (env-based, selects DB backend + storage + cache)
- Health check and basic error handling middleware
- Rate limiting middleware with tiered resolution (user > group > global)

### Phase 2: Auth & Core API
- Google OAuth token verification
- JWT session tokens (stateless, Redis-cached validation)
- User registration/profile endpoints
- Ban checking at login (email + Google ID against `banned_identifiers`)
- Location CRUD API (full REST with cursor pagination)
- Photo upload/download API with multipart handling
- Request validation and error responses

### Phase 3: Admin Console
- Admin Google OAuth login flow
- Admin middleware (role check)
- Dashboard with stats
- User management (list, search, ban/unban, grant admin)
- Ban management (email/Google ID bans)
- Location management (list, search, delete)
- Group management (list, delete)
- Rate limit management (CRUD, precedence viewer)
- Bootstrap: `ADMIN_SETUP_EMAIL` auto-grants admin on first login

### Phase 4: Android Server Integration
- Google Sign-In in the app
- Server API client (Retrofit)
- Per-location sync toggle and upload flow
- Download and display server locations
- Photo upload/download from app

### Phase 5: Friends & Groups
- Friend request/accept/decline flow (server + app)
- User search by email/name
- Friend groups CRUD (server + app)
- Group membership management

### Phase 6: Sharing & Collaborative Editing
- Share locations with individual friends or groups
- "Shared With Me" screen in app
- Revoke sharing
- Change suggestion submission, review, accept/reject
- Version-based conflict detection
- Propagation of accepted changes to all shared users

### Phase 7: FaaS Migration Preparation
- Lambda adapter integration (`serverless-http`)
- API Gateway configuration (SAM or Serverless Framework)
- DynamoDB repository implementations
- S3 storage implementation for photos
- Verify all endpoints work in both local and Lambda modes

---

## Resolved Decisions

| Question | Decision |
|---|---|
| Server language | Node.js / TypeScript / Express |
| Database extensions | No PostGIS — plain lat/lon with bounding-box queries for portability |
| Docker deployment | Single image, `docker run` with env vars, no Compose |
| Database | PostgreSQL (primary), MySQL (alternative), both via repository abstraction |
| JWT secret | Environment variable (`JWT_SECRET`) — sufficient for current stage |
| Pagination | Cursor-based on all list endpoints |
| Rate limiting | Tiered: user > group > global, configurable via admin console |
| Photo download (shared) | On-demand loading with local caching, no bulk download |
| Admin console | Server-rendered HTML, Google OAuth, role-gated (`is_admin`) |

## Open Questions

### Authentication & Identity
1. **Google Sign-In only**, or should the auth layer support pluggable providers (email/password, Apple) for future flexibility?
2. **Multiple devices** — Should a user be able to use the app on multiple Android devices with the same Google account and have everything sync?

### Data & Sync
3. **Full location sync or delta sync?** Full replacement is simpler for v1. Delta sync (only changed fields) can be added later. Confirm full sync is acceptable initially?
4. **Offline suggestions** — Can a user compose change suggestions while offline, queued for submission when connectivity returns?
5. **Location ownership transfer** — Should a location's ownership ever be transferable to another user?
6. **Auto-sync or manual only?** You specified user-elected sync. Should there also be an "auto-sync all new locations" preference for power users?

### Photos
7. **Max upload dimensions** — What max resolution for uploaded images? Suggested: 1920px longest edge. And max file size cap on the server (e.g., 2MB per image)?

### Social Features
8. **Friend discovery** — Email search only, or also QR code / share link for in-person connections?
9. **Group visibility** — Can group members see who else is in the group, or only the group owner?
10. **Notifications** — Push notifications (FCM) for friend requests, shares, and suggestions? Or polling on app open for v1?

### Shared Location Behaviour
11. **Cascading deletes** — If the author deletes a location, should it disappear from all friends' shared views, or should shared copies become independent?
12. **Multiple suggestions** — Can multiple friends submit suggestions on the same location simultaneously? How should the author handle conflicts between suggestions?
13. **Suggestion granularity** — Field-level diffs (e.g., "change opening hours to X") or whole-location replacement?

### Scope Boundaries
14. **Real-time features** — Any need for WebSockets/SSE, or is poll-on-open sufficient for v1?

### DynamoDB Migration
15. **Single-table or multi-table DynamoDB design?** Single-table design is more Lambda-efficient but harder to reason about. Multi-table is closer to the relational model. This affects how we design the repository abstraction and access patterns. Can be decided later but worth considering early.
