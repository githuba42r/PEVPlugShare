# Central Location Server - Feature Specification

## Overview

A self-hosted backend server that provides centralised storage for EV charging locations and images, tied to Google accounts. Users can selectively sync locations to the server, connect with friends (with mutual authorisation), create friend groups, and share locations. Shared locations support a collaborative editing model where recipients can suggest changes back to the author.

## Architecture

### System Components

```
+-------------------+         +-------------------+         +------------------+
|  Android App      | <-----> |  Backend Server    | <-----> |  PostgreSQL/     |
|  (PEVPlugShare)   |  REST   |  Python (Flask)    |         |  MySQL           |
|                   |  API    |  or Node (Express) | <-----> +------------------+
+-------------------+         +-------------------+         |  File Storage    |
                                |             |             |  (local disk /   |
                                v             v             |   S3-compatible) |
                          Google OAuth   Redis (optional)   +------------------+
```

### Tech Stack

- **Server**: Python (Flask/Flask-RESTful) or Node.js (Express) - TBD
- **Database**: PostgreSQL (primary, with PostGIS for geospatial queries), MySQL as alternative, via abstracted repository layer
- **Future Database**: AWS DynamoDB (architecture must support migration)
- **Caching**: Redis (optional) - session store, query cache, rate limiting
- **Authentication**: Google OAuth 2.0 / Google Sign-In
- **API**: REST with JSON payloads
- **File Storage**: Local filesystem initially; abstracted for S3 migration
- **Deployment**: Single Docker image pushed to a registry; `docker run` to start. No Docker Compose for the build process. External PostgreSQL/MySQL and Redis are provided via environment variables.
- **Future Deployment**: AWS Lambda/FaaS migration

### Design Principles

1. **Abstracted data layer** - All database access through a repository interface. Implementations are swappable (PostgreSQL, MySQL, DynamoDB) without changing business logic.
2. **Stateless request handling** - No server-side session state beyond JWT tokens and optional Redis cache. Enables horizontal scaling and Lambda deployment.
3. **Handler/service/repository separation** - Route handlers are thin (parse request, call service, return response). Business logic lives in services. Data access lives in repositories.
4. **FaaS-ready structure** - Each route handler can be extracted as an individual Lambda function. Shared business logic stays in service modules.

### Project Structure (Python/Flask variant)

```
server/
  app.py                        # Flask app factory, WSGI entrypoint
  config.py                     # Environment-based configuration
  lambda_handler.py             # AWS Lambda entrypoint (wraps Flask app)
  requirements.txt

  auth/
    google_oauth.py             # Google token verification
    middleware.py               # JWT auth middleware

  routes/
    users.py                    # User profile endpoints
    locations.py                # Location CRUD endpoints
    photos.py                   # Photo upload/download endpoints
    friends.py                  # Friendship management endpoints
    groups.py                   # Friend group endpoints
    sharing.py                  # Location sharing endpoints
    suggestions.py              # Change suggestion endpoints

  services/
    user_service.py
    location_service.py
    photo_service.py
    friend_service.py
    group_service.py
    sharing_service.py
    suggestion_service.py

  repositories/
    base.py                     # Abstract repository interfaces
    postgres/                   # PostgreSQL implementations
      user_repo.py
      location_repo.py
      photo_repo.py
      friend_repo.py
      group_repo.py
      share_repo.py
      suggestion_repo.py
    dynamodb/                   # DynamoDB implementations (future)
      ...

  storage/
    base.py                     # Abstract file storage interface
    local_storage.py            # Local filesystem implementation
    s3_storage.py               # S3 implementation (future)

  cache/
    base.py                     # Abstract cache interface
    redis_cache.py              # Redis implementation
    memory_cache.py             # In-memory fallback (dev/testing)

  models/
    user.py                     # Domain models (plain dataclasses)
    location.py
    photo.py
    friendship.py
    group.py
    share.py
    suggestion.py

  db/
    migrations/                 # Alembic or similar migration files
    schema.sql                  # Reference schema
```

### Project Structure (Node.js/Express variant)

```
server/
  src/
    app.ts                      # Express app factory
    server.ts                   # HTTP server entrypoint
    lambda.ts                   # AWS Lambda entrypoint (wraps Express)
    config.ts                   # Environment-based configuration

    auth/
      google-oauth.ts
      middleware.ts

    routes/
      users.ts
      locations.ts
      photos.ts
      friends.ts
      groups.ts
      sharing.ts
      suggestions.ts

    services/                   # Business logic (same structure as Python)
    repositories/
      interfaces.ts             # Repository interfaces (TypeScript)
      postgres/                 # PostgreSQL implementations (pg/knex)
      dynamodb/                 # DynamoDB implementations (future)

    storage/
      interfaces.ts
      local-storage.ts
      s3-storage.ts             # Future

    cache/
      interfaces.ts
      redis-cache.ts
      memory-cache.ts

    models/                     # Domain types

  package.json
  tsconfig.json
```

### Docker Deployment

Single self-contained Docker image. PostgreSQL/MySQL and Redis are external services configured via environment variables.

**Dockerfile** (Python variant):
```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8080
CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:8080", "app:create_app()"]
```

**Dockerfile** (Node.js variant):
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
| `JWT_SECRET` | Yes | Secret for signing JWT tokens |
| `STORAGE_PATH` | Yes | Path for photo file storage (mount a volume) |
| `LOG_LEVEL` | No | `debug`, `info`, `warn`, `error` (default: `info`) |
| `MAX_UPLOAD_SIZE_MB` | No | Max photo upload size in MB (default: `5`) |

**Database migrations** run automatically on container start (or via a separate `docker run ... migrate` command).

---

### Repository Interface Pattern

The core abstraction enabling database migration. Example (Python):

```python
# repositories/base.py
from abc import ABC, abstractmethod

class LocationRepository(ABC):
    @abstractmethod
    async def create(self, location: Location) -> Location: ...

    @abstractmethod
    async def get_by_id(self, id: str) -> Location | None: ...

    @abstractmethod
    async def get_by_owner(self, owner_id: str) -> list[Location]: ...

    @abstractmethod
    async def update(self, id: str, fields: dict) -> Location: ...

    @abstractmethod
    async def delete(self, id: str) -> bool: ...

    @abstractmethod
    async def get_shared_with_user(self, user_id: str) -> list[Location]: ...
```

```python
# repositories/postgres/location_repo.py
class PostgresLocationRepository(LocationRepository):
    def __init__(self, db_pool):
        self.pool = db_pool

    async def create(self, location: Location) -> Location:
        async with self.pool.acquire() as conn:
            row = await conn.fetchrow("INSERT INTO server_locations ...")
            return Location.from_row(row)
```

Repository selection is configuration-driven:

```python
# config.py
DB_BACKEND = os.getenv("DB_BACKEND", "postgres")  # postgres | mysql | dynamodb
```

### Redis Caching Strategy

Optional layer, gracefully degrades when unavailable:

- **Session/auth tokens**: Cache validated Google tokens (short TTL)
- **User profiles**: Cache frequently accessed user data
- **Friend lists**: Cache friend relationships (invalidate on change)
- **Shared location lists**: Cache per-user (invalidate on share/unshare)
- **Rate limiting**: Request counters per user/endpoint

### AWS Lambda Migration Path

The server is structured so migration to Lambda is incremental:

1. **Phase 1 (current)**: Single Flask/Express process, all routes in one app
2. **Phase 2**: Deploy the same app behind API Gateway using a Lambda adapter (e.g., `mangum` for Flask, `serverless-http` for Express) - zero code changes
3. **Phase 3**: Split hot-path routes into individual Lambda functions for independent scaling
4. **Phase 4**: Swap PostgreSQL repository implementations for DynamoDB

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
  created_at      TIMESTAMP
  last_seen_at    TIMESTAMP
```

### Server Locations

```
server_locations
  id              UUID PRIMARY KEY
  owner_id        UUID REFERENCES users(id)
  name            VARCHAR NOT NULL
  latitude        DOUBLE NOT NULL
  longitude       DOUBLE NOT NULL
  geom            GEOMETRY(Point, 4326)  -- PostGIS: auto-populated from lat/lon via trigger
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

Photos are pre-processed on the phone before upload (downsampled, resized, compressed). The server stores only the optimised versions - no server-side image processing required.

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

---

## API Endpoints

### Authentication
- `POST /auth/google` - Exchange Google ID token for server session/JWT

### User Profile
- `GET /api/users/me` - Get current user profile
- `GET /api/users/search?q={email_or_name}` - Search users to add as friends

### Locations
- `POST /api/locations` - Upload a location to the server
- `PUT /api/locations/{id}` - Update an owned location
- `DELETE /api/locations/{id}` - Delete an owned location
- `GET /api/locations` - List own server locations
- `GET /api/locations/{id}` - Get location detail with photos
- `GET /api/locations/shared-with-me` - List locations shared with the current user

### Location Photos
- `POST /api/locations/{id}/photos` - Upload pre-processed photo (multipart, phone handles downsampling)
- `DELETE /api/locations/{id}/photos/{photoId}` - Delete a photo
- `GET /api/photos/{photoId}/image` - Download optimised image (with ETag/Last-Modified for caching)
- `GET /api/photos/{photoId}/thumbnail` - Download thumbnail (with ETag/Last-Modified for caching)

### Friendships
- `POST /api/friends/request` - Send friend request (by email or user ID)
- `GET /api/friends/requests/incoming` - List pending incoming requests
- `GET /api/friends/requests/outgoing` - List pending outgoing requests
- `PUT /api/friends/requests/{id}/accept` - Accept a friend request
- `PUT /api/friends/requests/{id}/decline` - Decline a friend request
- `DELETE /api/friends/{userId}` - Remove a friend
- `GET /api/friends` - List all accepted friends

### Friend Groups
- `POST /api/groups` - Create a group
- `PUT /api/groups/{id}` - Update group name/description
- `DELETE /api/groups/{id}` - Delete a group
- `GET /api/groups` - List own groups
- `POST /api/groups/{id}/members` - Add friend(s) to group
- `DELETE /api/groups/{id}/members/{userId}` - Remove member from group

### Sharing
- `POST /api/locations/{id}/share` - Share location with user or group
- `DELETE /api/locations/{id}/share/{shareId}` - Revoke a share
- `GET /api/locations/{id}/shares` - List who a location is shared with

### Change Suggestions
- `POST /api/locations/{id}/suggestions` - Suggest changes to a shared location
- `GET /api/suggestions/incoming` - List suggestions on owned locations
- `GET /api/suggestions/outgoing` - List own suggestions on others' locations
- `PUT /api/suggestions/{id}/accept` - Accept and integrate suggestion
- `PUT /api/suggestions/{id}/reject` - Reject suggestion
- `PUT /api/suggestions/{id}/withdraw` - Withdraw own suggestion

---

## Android App Changes

### New Screens / UI

1. **Server Account Screen** - Google Sign-In, connection status, account info
2. **Location Sync Controls** - Per-location toggle to sync to server (in edit screen)
3. **Bulk Sync Screen** - Select multiple locations to upload/sync
4. **Friends Screen** - List friends, search/add users, manage requests
5. **Friend Groups Screen** - Create/manage groups, add/remove members
6. **Share Location Dialog** - Pick friends or groups to share with
7. **Shared With Me Screen** - Browse locations shared by friends
8. **Suggestion Composer** - Edit a shared location and submit changes as a suggestion
9. **Incoming Suggestions Screen** - Review, accept/reject suggestions on owned locations

### Data Layer Changes

- New `ServerApiService` (Retrofit) for all backend endpoints
- New `ServerSyncRepository` to manage upload/download of locations and photos
- Extend `LocationEntity` with:
  - `serverId: String?` - UUID on the server (null = local only)
  - `serverVersion: Int?` - last known server version
  - `serverSyncStatus: String` - LOCAL_ONLY | SYNCED | MODIFIED | CONFLICT
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

---

## Implementation Phases

### Phase 1: Server Foundation & Data Layer
- Project scaffolding (Flask or Express with TypeScript)
- Abstract repository interfaces and PostgreSQL/MySQL implementations
- Abstract file storage interface and local filesystem implementation
- Database schema and migrations (Alembic for Python / Knex for Node)
- Optional Redis cache layer with in-memory fallback
- Configuration system (env-based, selects DB backend + storage + cache)
- Health check and basic error handling middleware

### Phase 2: Auth & Core API
- Google OAuth token verification
- JWT session tokens (stateless, Redis-cached validation)
- User registration/profile endpoints
- Location CRUD API (full REST)
- Photo upload/download API with multipart handling
- Request validation and error responses

### Phase 3: Android Server Integration
- Google Sign-In in the app
- Server API client (Retrofit)
- Per-location sync toggle and upload flow
- Download and display server locations
- Photo upload/download from app

### Phase 4: Friends & Groups
- Friend request/accept/decline flow (server + app)
- User search by email/name
- Friend groups CRUD (server + app)
- Group membership management

### Phase 5: Sharing & Collaborative Editing
- Share locations with individual friends or groups
- "Shared With Me" screen in app
- Revoke sharing
- Change suggestion submission, review, accept/reject
- Version-based conflict detection
- Propagation of accepted changes to all shared users

### Phase 6: FaaS Migration Preparation
- Lambda adapter integration (mangum / serverless-http)
- API Gateway configuration (SAM or Serverless Framework)
- DynamoDB repository implementations
- S3 storage implementation for photos
- Verify all endpoints work in both local and Lambda modes

---

## Open Questions & Clarifications Needed

### Server Language (Decision Required)
1. **Python (Flask) or Node.js (Express/TypeScript)?** Both support the abstracted architecture and Lambda migration. Python has simpler async (asyncio/asyncpg), richer data science ecosystem. Node/TS has native async, strong typing, larger middleware ecosystem. Which do you prefer?

### Deployment (Partially Resolved)
2. ~~Docker Compose for local dev?~~ **RESOLVED**: Single Docker image, `docker run` with env vars. No Docker Compose for build. Image pushed to registry.
3. ~~PostgreSQL or MySQL~~ **RESOLVED**: PostgreSQL as primary, with PostGIS extension for geospatial queries (ST_DWithin for nearby searches, spatial indexing, distance calculations). MySQL remains supported via repository abstraction but without geospatial optimisations.

### Authentication & Identity
4. **Google Sign-In only**, or should the auth layer support pluggable providers (email/password, Apple) for future flexibility?
5. **Multiple devices** - Should a user be able to use the app on multiple Android devices with the same Google account and have everything sync?

### Data & Sync
6. **Full location sync or delta sync?** Full replacement is simpler for v1. Delta sync (only changed fields) can be added later. Confirm full sync is acceptable initially?
7. **Offline suggestions** - Can a user compose change suggestions while offline, queued for submission when connectivity returns?
8. **Location ownership transfer** - Should a location's ownership ever be transferable to another user?
9. **Auto-sync or manual only?** You specified user-elected sync. Should there also be an "auto-sync all new locations" preference for power users?

### Photos (Partially Resolved)
10. **Max upload dimensions** - What max resolution for uploaded images? Suggested: 1920px longest edge. And max file size cap on the server (e.g., 2MB per image)?
11. ~~Photo download for shared locations~~ **RESOLVED**: On-demand loading with local caching. No bulk download.

### Social Features
12. **Friend discovery** - Email search only, or also QR code / share link for in-person connections?
13. **Group visibility** - Can group members see who else is in the group, or only the group owner?
14. **Notifications** - Push notifications (FCM) for friend requests, shares, and suggestions? Or polling on app open for v1?

### Shared Location Behaviour
15. **Cascading deletes** - If the author deletes a location, should it disappear from all friends' shared views, or should shared copies become independent?
16. **Multiple suggestions** - Can multiple friends submit suggestions on the same location simultaneously? How should the author handle conflicts between suggestions?
17. **Suggestion granularity** - Field-level diffs (e.g., "change opening hours to X") or whole-location replacement?

### Scope Boundaries
18. **Real-time features** - Any need for WebSockets/SSE, or is poll-on-open sufficient for v1?
19. **Rate limiting** - Worth implementing in v1 (Redis-based), or defer until public deployment?

### DynamoDB Migration
20. **Single-table or multi-table DynamoDB design?** Single-table design is more Lambda-efficient but harder to reason about. Multi-table is closer to the relational model. This affects how we design the repository abstraction and access patterns. Can be decided later but worth considering early.
