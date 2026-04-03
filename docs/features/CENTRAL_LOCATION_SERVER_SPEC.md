# Central Location Server - Feature Specification

## Overview

A self-hosted backend server that provides centralised storage for EV charging locations and images, tied to Google accounts. Users can selectively sync locations to the server, connect with friends (with mutual authorisation), create friend groups, and share locations. Shared locations support a collaborative editing model where recipients can suggest changes back to the author. Locations can also be shared directly via QR codes and deep links.

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
- **Future Database**: AWS DynamoDB (architecture supports migration, decision deferred)
- **Caching**: Redis (optional) — session store, query cache, rate limiting
- **Authentication**: Google OAuth 2.0 / Google Sign-In only (Android-only app, no iOS planned)
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
6. **All sync is user-initiated** — No background sync in either direction. Users explicitly pull shared locations and push suggestions.

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
      sharing.ts                # Location sharing + direct share endpoints
      suggestions.ts            # Change suggestion endpoints
      deep-links.ts             # QR code / deep link token generation and resolution

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
        sharing-bans.ejs

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
      duplicate-detection-service.ts
      deep-link-service.ts

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
        deep-link-repo.ts
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
      deep-link.ts

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
  -e SERVER_BASE_URL=https://your-server.example.com \
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
| `SERVER_BASE_URL` | Yes | Public URL of the server (used for deep link generation) |
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
  findDuplicates(lat: number, lon: number, radiusMetres: number): Promise<Location[]>;
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

DynamoDB table design (single-table vs multi-table) deferred until Phase 7 — will be informed by actual access patterns from production usage.

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
  is_banned       BOOLEAN DEFAULT FALSE      -- banned from using the service
  ban_reason      VARCHAR                    -- optional reason for ban
  banned_at       TIMESTAMP
  banned_by       UUID REFERENCES users(id)  -- admin who banned them
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

### Sharing Bans

Prevents specific users from sharing locations, without banning their account entirely. Used for etiquette breaches, privacy violations, or nuisance behaviour.

```
sharing_bans
  id              UUID PRIMARY KEY
  user_id         UUID REFERENCES users(id)
  reason          VARCHAR NOT NULL
  banned_by       UUID REFERENCES users(id)  -- admin who issued the ban
  expires_at      TIMESTAMP                  -- NULL = permanent
  created_at      TIMESTAMP
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
  is_deleted      BOOLEAN DEFAULT FALSE -- soft delete: keeps record for notification to share recipients
  deleted_reason  VARCHAR               -- reason shown to share recipients
  created_at      TIMESTAMP
  updated_at      TIMESTAMP
  version         INT DEFAULT 1        -- incremented on each update
```

### Server Location Photos

Photos are pre-processed on the phone before upload (downsampled to 1920px longest edge, JPEG 75-80%, max 2MB). The server stores only the optimised versions — no server-side image processing required.

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
  role            VARCHAR NOT NULL DEFAULT 'MEMBER'  -- ADMIN | MEMBER
  added_at        TIMESTAMP

  PRIMARY KEY(group_id, user_id)
```

Group admin rules:
- The group owner is always an admin (implicit, not stored as a member row)
- Group admins can approve/decline membership requests and remove members
- Regular members can nominate new users for membership (via QR code or deep link)
- Only admins can approve nominations

### Group Invitations

```
group_invitations
  id              UUID PRIMARY KEY
  group_id        UUID REFERENCES friend_groups(id) ON DELETE CASCADE
  invited_by      UUID REFERENCES users(id)          -- member who nominated
  invitee_id      UUID REFERENCES users(id)           -- NULL if via link (not yet claimed)
  invitee_email   VARCHAR                             -- NULL if invitee_id is set
  token           VARCHAR UNIQUE NOT NULL              -- unique token for deep link / QR
  status          VARCHAR NOT NULL DEFAULT 'PENDING'  -- PENDING | ACCEPTED | DECLINED | EXPIRED
  created_at      TIMESTAMP
  expires_at      TIMESTAMP                           -- invite link expiry
  responded_at    TIMESTAMP
  responded_by    UUID REFERENCES users(id)           -- admin who approved/declined
```

### Location Sharing

```
location_shares
  id              UUID PRIMARY KEY
  location_id     UUID REFERENCES server_locations(id)  -- no CASCADE: soft-delete on locations
  shared_by       UUID REFERENCES users(id)       -- always the location owner
  shared_with_user  UUID REFERENCES users(id)      -- NULL if shared with group
  shared_with_group UUID REFERENCES friend_groups(id) -- NULL if shared with user
  is_active       BOOLEAN DEFAULT TRUE             -- set to FALSE when location soft-deleted
  shared_at       TIMESTAMP

  -- Either shared_with_user or shared_with_group must be set, not both
```

When a location is soft-deleted (`is_deleted = true`), all its shares are marked `is_active = false` and recipients are notified with the `deleted_reason`. Recipients' local copies are unaffected.

### Direct Share Tokens

For sharing locations via QR code or deep link, bypassing the friend/group system.

```
direct_share_tokens
  id              UUID PRIMARY KEY
  location_id     UUID REFERENCES server_locations(id)
  created_by      UUID REFERENCES users(id)        -- must be the location owner
  token           VARCHAR UNIQUE NOT NULL           -- unique token for URL/QR
  is_single_use   BOOLEAN DEFAULT FALSE             -- TRUE = one-time use
  use_count       INT DEFAULT 0
  max_uses        INT                               -- NULL = unlimited
  expires_at      TIMESTAMP                         -- NULL = never expires
  created_at      TIMESTAMP
```

Deep link format: `pevplugshare://location/{token}` or `https://{SERVER_BASE_URL}/share/{token}`
QR code encodes the deep link URL.

### Hidden Shares

When a user sees the same physical location from multiple groups, they pick one and hide the others.

```
hidden_shares
  id              UUID PRIMARY KEY
  user_id         UUID REFERENCES users(id)
  location_id     UUID REFERENCES server_locations(id)
  share_id        UUID REFERENCES location_shares(id)
  hidden_at       TIMESTAMP
```

Hidden shares are automatically un-hidden when the underlying location's `version` increments (the update may contain new information worth reviewing).

### Change Suggestions

```
change_suggestions
  id              UUID PRIMARY KEY
  location_id     UUID REFERENCES server_locations(id)
  suggested_by    UUID REFERENCES users(id)
  status          VARCHAR NOT NULL     -- PENDING | ACCEPTED | REJECTED | WITHDRAWN
  changes_json    TEXT NOT NULL        -- JSON field-level diff: { "field": "new_value", ... }
  message         VARCHAR              -- optional note from suggester
  response_msg    VARCHAR              -- optional note from owner on accept/reject
  created_at      TIMESTAMP
  responded_at    TIMESTAMP
```

**Suggestion format** — field-level diffs:
```json
{
  "opening_hours": "Mon-Fri 8am-6pm",
  "has_fee": true,
  "charging_costs": "$2/hour"
}
```

Multiple suggestions can be pending simultaneously on the same location. The owner reviews and accepts/rejects each independently. Accepting a suggestion overwrites the specified fields and increments the version. If two suggestions change the same field, the second accepted one wins — the owner is the final arbiter.

### Notifications

Polled on app open — no push notifications for v1.

```
notifications
  id              UUID PRIMARY KEY
  user_id         UUID REFERENCES users(id)
  type            VARCHAR NOT NULL     -- FRIEND_REQUEST | SHARE_RECEIVED | SUGGESTION_RECEIVED
                                       -- | SUGGESTION_ACCEPTED | SUGGESTION_REJECTED
                                       -- | LOCATION_DELETED | GROUP_INVITE | DUPLICATE_DETECTED
  payload_json    TEXT NOT NULL        -- JSON with context (location_id, friend_id, reason, etc.)
  is_read         BOOLEAN DEFAULT FALSE
  created_at      TIMESTAMP
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

## Duplicate Detection

When a user pulls a shared location to their local copy, or when locations are shared across multiple groups, duplicates can occur. The server detects and manages duplicates:

### Detection
- **Proximity threshold**: 50 metres — locations within 50m of each other at the same coordinate precision are flagged as potential duplicates
- **Triggered on**: sharing a location, pulling a shared location to local
- **Server-side check**: `findDuplicates(lat, lon, 50)` query on the user's visible locations

### User Experience
- When a user sees the same physical location from multiple groups:
  1. App shows all versions in "Shared With Me"
  2. User selects which group's version to store locally
  3. Other duplicates are **hidden** (stored in `hidden_shares`)
  4. If a hidden location is updated (version increments), it is **un-hidden** and the user is notified — the update may contain new information worth reviewing

### Suggestion Deduplication
- When multiple users suggest changes to the same location, the owner is notified of the overlap
- Owner reviews each suggestion independently — no automatic merging
- Accepting one does not auto-reject others, but may make them stale if they touch the same fields

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
  - Checks `sharing_bans` table — includes sharing ban status in JWT claims
  - Creates user record on first login
  - If `ADMIN_SETUP_EMAIL` matches and no admins exist, grants admin role

### User Profile
- `GET /api/users/me` — Get current user profile
- `GET /api/users/search?q={email_or_name}` — Search users to add as friends (paginated)

### Locations
- `POST /api/locations` — Upload a location to the server
- `PUT /api/locations/{id}` — Update an owned location
- `DELETE /api/locations/{id}` — Soft-delete an owned location (with reason, notifies share recipients)
- `GET /api/locations` — List own server locations (paginated)
- `GET /api/locations/{id}` — Get location detail with photos
- `GET /api/locations/shared-with-me` — List locations shared with the current user (paginated, excludes hidden)
- `GET /api/locations/nearby?lat={lat}&lon={lon}&radius={km}` — Find locations within radius (paginated, bounding-box query)

### Location Photos
- `POST /api/locations/{id}/photos` — Upload pre-processed photo (multipart, max 2MB, 1920px longest edge)
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
- `DELETE /api/groups/{id}` — Delete a group (owner or admin only)
- `GET /api/groups` — List own groups (paginated)
- `GET /api/groups/{id}/members` — List group members (visible to all members)
- `POST /api/groups/{id}/members` — Add friend(s) to group (admin only)
- `DELETE /api/groups/{id}/members/{userId}` — Remove member from group (admin only)
- `PUT /api/groups/{id}/members/{userId}/role` — Change member role (owner only, `{ role: "ADMIN" | "MEMBER" }`)
- `POST /api/groups/{id}/invite` — Generate invite link/token (any member can nominate)
- `GET /api/groups/invite/{token}` — Get invite details (for deep link resolution)
- `POST /api/groups/invite/{token}/accept` — Claim invite (creates pending request for admin approval)
- `GET /api/groups/{id}/invitations` — List pending invitations (admin only)
- `PUT /api/groups/{id}/invitations/{inviteId}/approve` — Approve membership (admin only)
- `PUT /api/groups/{id}/invitations/{inviteId}/decline` — Decline membership (admin only)

### Sharing
- `POST /api/locations/{id}/share` — Share location with user or group (checks sharing ban)
- `DELETE /api/locations/{id}/share/{shareId}` — Revoke a share
- `GET /api/locations/{id}/shares` — List who a location is shared with (paginated)
- `POST /api/locations/{id}/share/direct` — Generate direct share token (QR code / deep link)
- `GET /api/share/{token}` — Resolve direct share token → location data
- `POST /api/locations/{shareId}/hide` — Hide a duplicate shared location
- `DELETE /api/locations/{shareId}/hide` — Un-hide a shared location

### Change Suggestions
- `POST /api/locations/{id}/suggestions` — Suggest field-level changes to a shared location
- `GET /api/suggestions/incoming` — List suggestions on owned locations (paginated)
- `GET /api/suggestions/outgoing` — List own suggestions on others' locations (paginated)
- `PUT /api/suggestions/{id}/accept` — Accept suggestion (applies field changes, increments version)
- `PUT /api/suggestions/{id}/reject` — Reject suggestion (with optional message)
- `PUT /api/suggestions/{id}/withdraw` — Withdraw own suggestion

### Notifications
- `GET /api/notifications` — List unread notifications (paginated)
- `PUT /api/notifications/{id}/read` — Mark notification as read
- `PUT /api/notifications/read-all` — Mark all notifications as read

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
- **Ban user** — sets `is_banned`, records reason and admin who banned; also adds email + Google ID to `banned_identifiers`
- **Unban user** — clears ban flag
- **Sharing ban** — ban user from sharing (temporary with expiry, or permanent)
- **Remove sharing ban**
- View user's locations (links to location detail)

#### Ban Management (`/admin/bans`)
- List all banned users and banned identifiers
- **Ban email address** — add to `banned_identifiers` (prevents re-registration)
- **Ban Google account ID** — add to `banned_identifiers`
- **Remove ban** — remove identifier from banned list

#### Sharing Ban Management (`/admin/sharing-bans`)
- List all active sharing bans (with expiry dates)
- **Create sharing ban** — ban user from sharing with reason and optional expiry
- **Remove sharing ban**
- View expired sharing bans (history)

#### Group Management (`/admin/groups`)
- Paginated group list with search
- Group detail: owner, admins, members list, shared locations count, pending invitations
- Delete group (admin override)

#### Location Management (`/admin/locations`)
- Paginated location list with search (by name, owner email)
- Location detail: all fields, photos, share list, suggestions
- **Delete location** (admin override, soft-delete with reason — notifies share recipients)
- **Remove from sharing** — prevent location from being shared further (without deleting)
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
- `POST /admin/api/users/{id}/sharing-ban` — Create sharing ban (`{ reason, expiresAt? }`)
- `DELETE /admin/api/users/{id}/sharing-ban` — Remove sharing ban
- `GET /admin/api/bans` — List banned identifiers
- `POST /admin/api/bans` — Add banned identifier (`{ type: "EMAIL"|"GOOGLE_ID", identifier, reason }`)
- `DELETE /admin/api/bans/{id}` — Remove banned identifier
- `GET /admin/api/sharing-bans` — List all sharing bans
- `GET /admin/api/groups` — List all groups (paginated)
- `GET /admin/api/groups/{id}` — Group detail with members and roles
- `DELETE /admin/api/groups/{id}` — Delete group
- `GET /admin/api/locations` — List all locations (paginated, searchable)
- `GET /admin/api/locations/{id}` — Location detail
- `DELETE /admin/api/locations/{id}` — Soft-delete location (`{ reason }`)
- `PUT /admin/api/locations/{id}/block-sharing` — Prevent further sharing of a location
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
5. **Friend Groups Screen** — Create/manage groups, add/remove members, manage admins
6. **Group Invite Screen** — Generate QR code / share link for group invitations
7. **Share Location Dialog** — Pick friends or groups to share with, or generate direct share link/QR
8. **Shared With Me Screen** — Browse locations shared by friends, pull to local, hide duplicates
9. **Suggestion Composer** — Edit a shared location and submit field-level changes as a suggestion
10. **Incoming Suggestions Screen** — Review field diffs, accept/reject suggestions on owned locations
11. **Notifications Screen** — View friend requests, shares, suggestions, deletion notices, group invites

### Deep Link Handling

The app registers handlers for:
- `pevplugshare://location/{token}` — direct location share, opens location detail from server
- `pevplugshare://group/{token}` — group invite, shows group info and join button
- `https://{SERVER_BASE_URL}/share/{token}` — web fallback, redirects to app or shows install prompt

### Data Layer Changes

- New `ServerApiService` (Retrofit) for all backend endpoints
- New `ServerSyncRepository` to manage upload/download of locations and photos
- Extend `LocationEntity` with:
  - `serverId: String?` — UUID on the server (null = local only)
  - `serverVersion: Int?` — last known server version
  - `serverSyncStatus: String` — LOCAL_ONLY | SYNCED | MODIFIED | CONFLICT
- Google Sign-In integration (Google Identity Services library)
- `PendingOperation` extended with `SUGGESTION` type for offline suggestion queueing

### Photo Processing (Client-Side)

Photos are processed on the phone before upload to optimise bandwidth and storage:

- **Downsampling**: Resize to max 1920px longest edge before upload
- **Compression**: JPEG quality 75-80%
- **Max file size**: 2MB per image (enforced client-side and server-side)
- **Thumbnail generation**: App generates and uploads a separate thumbnail (400px)
- **Metadata preservation**: EXIF GPS, bearing, and timestamp extracted before resize and sent as structured fields
- **No server-side processing**: Server stores exactly what the phone sends

### Sync Behaviour

- **All sync is user-initiated** — no background sync in either direction
- **Upload**: User explicitly selects locations to sync (no automatic upload)
- **Download (pull)**: User explicitly pulls shared locations to local — creates a local copy
- **Suggestions (push)**: User explicitly pushes local edits back as a suggestion
- **Conflict detection**: Compare local `serverVersion` against server version on sync
- **Photos (owned locations)**: Pre-processed on phone, uploaded alongside location data
- **Photos (shared locations)**: NOT downloaded with the location. Images are loaded on-demand from the server and cached locally on the device (LRU eviction, ETag-based revalidation)
- **Multi-device**: Same Google account on multiple devices works naturally — server is source of truth, each device syncs independently
- **Offline suggestions**: Suggestions composed offline are queued in `PendingOperation` and submitted when connectivity returns

---

## Collaborative Editing Flow

```
1. Author creates location, uploads to server
2. Author shares location with Friend, Group, or via direct link/QR
3. Recipient sees location in "Shared With Me"
4. Recipient pulls location to local device (creates a local copy)
5. Recipient makes edits locally
6. Recipient explicitly submits edits as a "Change Suggestion" (field-level diff)
7. Author receives notification (polled on app open)
8. Author reviews diff (side-by-side: current → proposed, per field)
9. Author accepts or rejects the suggestion
10. If accepted: server location fields updated, version incremented
11. All users with access see updated location on next poll
12. If location has duplicate suggestions touching same fields, owner reviews independently
```

### Shared Location Lifecycle

```
1. Owner shares location → share record created
2. Recipient pulls to local → local copy created, independent of server
3. Owner soft-deletes location → shares marked inactive, recipients notified with reason
4. Recipients' local copies persist (they own them now)
5. Admin can block further sharing of a location without deleting it
6. Admin can soft-delete a location (same effect as owner delete, with admin reason)
```

---

## Direct Sharing Flow (QR Code / Deep Link)

### Location Sharing
```
1. Owner taps "Share via Link" on a location
2. Server generates a direct_share_token (with optional single-use or max-uses limit)
3. App displays QR code and/or copy-to-clipboard deep link
4. Recipient scans QR or taps link → app opens (or Play Store if not installed)
5. App resolves token via GET /api/share/{token} → receives location data
6. Recipient can pull location to local
```

### Group Invitation
```
1. Any group member taps "Invite to Group"
2. Server generates a group_invitation token
3. App displays QR code and/or copy-to-clipboard deep link
4. Recipient scans QR or taps link → app opens
5. App resolves token via GET /api/groups/invite/{token} → shows group info
6. Recipient taps "Request to Join"
7. Group admin(s) receive notification of pending membership request
8. Admin approves or declines
9. If approved, recipient becomes a group member
```

---

## Security & Privacy

- All API endpoints require authentication (JWT from Google OAuth)
- Users can only modify their own locations
- Friend requests require explicit acceptance before any data sharing
- Revoking a share marks it inactive — recipients keep local copies but lose server access
- Location data is only visible to owner + explicitly shared users + direct share token holders
- Server stores Google ID and email only (no passwords)
- Banned users and identifiers are checked at login time
- Sharing bans prevent location sharing (temporary or permanent)
- Admin actions are logged with the admin user who performed them
- Rate limiting prevents abuse (tiered: user > group > global)
- Direct share tokens can be single-use or time-limited
- Group membership requires admin approval

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
- Notification table and basic polling endpoint

### Phase 2: Auth & Core API
- Google OAuth token verification
- JWT session tokens (stateless, Redis-cached validation)
- User registration/profile endpoints
- Ban checking at login (email + Google ID against `banned_identifiers`)
- Location CRUD API (full REST with cursor pagination, soft-delete)
- Photo upload/download API with multipart handling (2MB limit, 1920px max)
- Request validation and error responses
- Duplicate detection service (50m proximity threshold)

### Phase 3: Admin Console
- Admin Google OAuth login flow
- Admin middleware (role check)
- Dashboard with stats
- User management (list, search, ban/unban, sharing ban, grant admin)
- Ban management (email/Google ID bans)
- Sharing ban management (temporary/permanent)
- Location management (list, search, soft-delete, block sharing)
- Group management (list, members, roles, delete)
- Rate limit management (CRUD, precedence viewer)
- Bootstrap: `ADMIN_SETUP_EMAIL` auto-grants admin on first login

### Phase 4: Android Server Integration
- Google Sign-In in the app
- Server API client (Retrofit)
- Per-location sync toggle and upload flow
- Pull shared locations to local (explicit, user-initiated)
- Push suggestions back (explicit, user-initiated)
- Photo upload/download from app
- Notification polling on app open
- Offline suggestion queueing via PendingOperation

### Phase 5: Friends & Groups
- Friend request/accept/decline flow (server + app)
- User search by email
- Friend groups CRUD (server + app)
- Group admin roles and membership management
- Group invite via QR code / deep link with admin approval flow

### Phase 6: Sharing & Collaborative Editing
- Share locations with individual friends or groups
- Direct sharing via QR code / deep link (token-based)
- "Shared With Me" screen in app with duplicate detection and hide/unhide
- Revoke sharing (soft)
- Change suggestion submission (field-level diffs), review, accept/reject
- Version-based conflict detection
- Notification of location deletion to share recipients
- Sharing ban enforcement

### Phase 7: FaaS Migration Preparation
- Lambda adapter integration (`serverless-http`)
- API Gateway configuration (SAM or Serverless Framework)
- DynamoDB repository implementations (table design decided at this stage based on actual access patterns)
- S3 storage implementation for photos
- Verify all endpoints work in both local and Lambda modes

---

## Resolved Decisions

| # | Question | Decision |
|---|---|---|
| 1 | Server language | Node.js / TypeScript / Express |
| 2 | Database extensions | No PostGIS — plain lat/lon with bounding-box queries for portability |
| 3 | Docker deployment | Single image, `docker run` with env vars, no Compose |
| 4 | Database | PostgreSQL (primary), MySQL (alternative), both via repository abstraction |
| 5 | JWT secret | Environment variable (`JWT_SECRET`) — sufficient for current stage |
| 6 | Pagination | Cursor-based on all list endpoints |
| 7 | Rate limiting | Tiered: user > group > global, configurable via admin console |
| 8 | Photo download (shared) | On-demand loading with local caching, no bulk download |
| 9 | Admin console | Server-rendered HTML, Google OAuth, role-gated (`is_admin`) |
| 10 | Auth providers | Google-only (Android-only app, no iOS planned) |
| 11 | Multiple devices | Supported naturally — server is source of truth, each device syncs independently |
| 12 | Sync approach | Full location sync for v1 (delta sync can be added later) |
| 13 | Offline suggestions | Yes — queued in PendingOperation, submitted when online |
| 14 | Ownership transfer | No — never. Owner deletes, recipients keep local copies |
| 15 | Auto-sync | No — all sync is user-initiated in both directions |
| 16 | Photo dimensions | 1920px longest edge, 2MB max, JPEG 75-80% |
| 17 | Friend discovery | Email search for v1 |
| 18 | Location sharing | Via friends/groups AND direct QR code / deep link |
| 19 | Group invites | QR code / deep link, any member nominates, admin approves |
| 20 | Group visibility | All members see the full member list |
| 21 | Notifications | Polling on app open for v1, no push (FCM deferred) |
| 22 | Cascading deletes | Soft-delete: shares marked inactive, recipients notified, local copies persist |
| 23 | Multiple suggestions | Allowed simultaneously, owner reviews independently |
| 24 | Suggestion granularity | Field-level diffs |
| 25 | Real-time features | Polling only for v1, no WebSockets/SSE |
| 26 | DynamoDB design | Deferred to Phase 7, informed by actual access patterns |
| 27 | Duplicate detection | 50m proximity threshold, user picks preferred version, others hidden until updated |
| 28 | Sharing bans | Temporary (with expiry) or permanent, managed via admin console |

## Open Questions

All questions resolved — spec is ready for implementation planning.
