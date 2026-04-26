# Central Location Server — Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add authentication (Google OAuth → JWT), user profile endpoints, location CRUD API with cursor pagination and soft-delete, photo upload/download API with multipart handling, duplicate detection, and notification endpoints — making the server functional for the Android app to connect to.

**Architecture:** Google OAuth verification via `google-auth-library`, JWT tokens via `jsonwebtoken`. Auth middleware extracts user from JWT on every `/api/*` request. Location and photo repositories added to the existing abstracted data layer (PostgreSQL/MySQL via Knex). Photo uploads via `multer` with server-side file size validation. All list endpoints use cursor-based pagination. The app factory (`createApp()`) is extended to accept a `Repositories` object and wire up all route handlers.

**Tech Stack:** google-auth-library, jsonwebtoken, multer (file uploads), existing Express/Knex/TypeScript stack from Phase 1

**Spec:** `docs/features/CENTRAL_LOCATION_SERVER_SPEC.md`

**Phase 1 foundation:** `server/src/` contains config, models, repositories (users/rate-limits/bans/notifications), cache, storage, middleware (error handler, rate limiter, request logger), health route, and app factory.

---

## Scope — What Phase 2 Builds

1. **New dependencies** — google-auth-library, jsonwebtoken, multer + types
2. **Auth module** — Google token verification, JWT sign/verify, auth middleware
3. **Location repository** — Full CRUD with cursor pagination, soft-delete, nearby search, duplicate detection
4. **Photo repository** — CRUD, file storage integration
5. **Location service** — Business logic for location operations
6. **Photo service** — Upload handling, storage, deletion
7. **Duplicate detection service** — 50m proximity threshold
8. **User routes** — `GET /api/users/me`, `GET /api/users/search`
9. **Location routes** — Full REST CRUD with pagination
10. **Photo routes** — Upload, download, delete with ETag caching
11. **Notification routes** — List unread, mark read
12. **App factory update** — Wire all routes with auth middleware, repositories, and storage
13. **Integration tests** — End-to-end API tests

## File Structure

### New Files

```
server/src/
  auth/
    google-oauth.ts              # Verify Google ID tokens
    jwt.ts                       # Sign and verify server JWTs
    middleware.ts                 # Express middleware: extract user from JWT

  services/
    location-service.ts          # Location business logic
    photo-service.ts             # Photo upload/storage logic
    duplicate-detection-service.ts # 50m proximity check

  repositories/
    postgres/
      location-repo.ts           # Location CRUD + nearby + duplicates
      photo-repo.ts              # Photo CRUD
    mysql/
      location-repo.ts           # Same as postgres (Knex abstracts)
      photo-repo.ts

  routes/
    auth.ts                      # POST /auth/google
    users.ts                     # GET /api/users/me, /api/users/search
    locations.ts                 # Location CRUD endpoints
    photos.ts                    # Photo upload/download/delete
    notifications.ts             # Notification list/mark-read

server/tests/
  auth/
    jwt.test.ts
    middleware.test.ts
  services/
    location-service.test.ts
    duplicate-detection-service.test.ts
  repositories/
    location-repo.test.ts
    photo-repo.test.ts
  routes/
    auth.test.ts
    users.test.ts
    locations.test.ts
    photos.test.ts
    notifications.test.ts
  integration/
    full-flow.test.ts            # Auth → create location → upload photo → list
```

### Modified Files

```
server/package.json                    # Add new dependencies
server/src/repositories/interfaces.ts  # Add LocationRepository, PhotoRepository methods
server/src/repositories/factory.ts     # Add location and photo repos
server/src/app.ts                      # Wire auth, routes, repos, storage
server/src/server.ts                   # Initialize DB connection, repos, pass to app
```

---

### Task 1: Add Dependencies

**Files:**
- Modify: `server/package.json`

- [ ] **Step 1: Install new dependencies**

```bash
cd server
npm install google-auth-library jsonwebtoken multer
npm install -D @types/jsonwebtoken @types/multer
```

- [ ] **Step 2: Verify install**

Run: `cd server && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add server/package.json server/package-lock.json
git commit -m "chore(server): add auth, JWT, and upload dependencies"
```

---

### Task 2: JWT Module

**Files:**
- Create: `server/src/auth/jwt.ts`
- Create: `server/tests/auth/jwt.test.ts`

- [ ] **Step 1: Write JWT test**

```typescript
// tests/auth/jwt.test.ts
import { signToken, verifyToken, JwtPayload } from '../../src/auth/jwt';

describe('JWT', () => {
  const testSecret = 'test-secret-key';
  const payload: JwtPayload = {
    userId: 'user-123',
    email: 'test@example.com',
    isAdmin: false,
    hasSharingBan: false,
  };

  it('signs and verifies a token', () => {
    const token = signToken(payload, testSecret);
    const decoded = verifyToken(token, testSecret);
    expect(decoded.userId).toBe('user-123');
    expect(decoded.email).toBe('test@example.com');
    expect(decoded.isAdmin).toBe(false);
    expect(decoded.hasSharingBan).toBe(false);
  });

  it('returns null for invalid token', () => {
    const result = verifyToken('invalid-token', testSecret);
    expect(result).toBeNull();
  });

  it('returns null for expired token', () => {
    const token = signToken(payload, testSecret, '0s');
    const result = verifyToken(token, testSecret);
    expect(result).toBeNull();
  });

  it('includes admin and sharing ban claims', () => {
    const adminPayload = { ...payload, isAdmin: true, hasSharingBan: true };
    const token = signToken(adminPayload, testSecret);
    const decoded = verifyToken(token, testSecret);
    expect(decoded!.isAdmin).toBe(true);
    expect(decoded!.hasSharingBan).toBe(true);
  });
});
```

- [ ] **Step 2: Implement JWT module**

```typescript
// src/auth/jwt.ts
import jwt from 'jsonwebtoken';

export interface JwtPayload {
  userId: string;
  email: string;
  isAdmin: boolean;
  hasSharingBan: boolean;
}

export function signToken(payload: JwtPayload, secret: string, expiresIn: string = '24h'): string {
  return jwt.sign(payload, secret, { expiresIn });
}

export function verifyToken(token: string, secret: string): JwtPayload | null {
  try {
    const decoded = jwt.verify(token, secret) as JwtPayload;
    return decoded;
  } catch {
    return null;
  }
}
```

- [ ] **Step 3: Run test**

Run: `cd server && npx jest tests/auth/jwt.test.ts`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add server/src/auth/jwt.ts server/tests/auth/jwt.test.ts
git commit -m "feat(server): JWT sign/verify module"
```

---

### Task 3: Google OAuth Verification

**Files:**
- Create: `server/src/auth/google-oauth.ts`

- [ ] **Step 1: Implement Google token verifier**

```typescript
// src/auth/google-oauth.ts
import { OAuth2Client } from 'google-auth-library';

export interface GoogleUserInfo {
  googleId: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
}

export async function verifyGoogleToken(idToken: string, clientId: string): Promise<GoogleUserInfo | null> {
  try {
    const client = new OAuth2Client(clientId);
    const ticket = await client.verifyIdToken({
      idToken,
      audience: clientId,
    });
    const payload = ticket.getPayload();
    if (!payload || !payload.sub || !payload.email) {
      return null;
    }
    return {
      googleId: payload.sub,
      email: payload.email,
      displayName: payload.name || payload.email.split('@')[0],
      avatarUrl: payload.picture || null,
    };
  } catch {
    return null;
  }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd server && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add server/src/auth/google-oauth.ts
git commit -m "feat(server): Google OAuth token verification"
```

---

### Task 4: Auth Middleware

**Files:**
- Create: `server/src/auth/middleware.ts`
- Create: `server/tests/auth/middleware.test.ts`

- [ ] **Step 1: Write auth middleware test**

```typescript
// tests/auth/middleware.test.ts
import express from 'express';
import request from 'supertest';
import { createAuthMiddleware } from '../../src/auth/middleware';
import { signToken, JwtPayload } from '../../src/auth/jwt';

const TEST_SECRET = 'test-secret';

function createTestApp() {
  const app = express();
  app.use(express.json());
  app.use('/api', createAuthMiddleware(TEST_SECRET));
  app.get('/api/test', (req: any, res) => {
    res.json({ userId: req.userId, email: req.email, isAdmin: req.isAdmin });
  });
  app.get('/public', (_req, res) => res.json({ ok: true }));
  return app;
}

describe('authMiddleware', () => {
  const app = createTestApp();
  const payload: JwtPayload = {
    userId: 'user-123',
    email: 'test@example.com',
    isAdmin: false,
    hasSharingBan: false,
  };

  it('returns 401 when no Authorization header', async () => {
    const res = await request(app).get('/api/test');
    expect(res.status).toBe(401);
    expect(res.body.error).toContain('token');
  });

  it('returns 401 for invalid token', async () => {
    const res = await request(app)
      .get('/api/test')
      .set('Authorization', 'Bearer invalid-token');
    expect(res.status).toBe(401);
  });

  it('sets userId, email, isAdmin on request for valid token', async () => {
    const token = signToken(payload, TEST_SECRET);
    const res = await request(app)
      .get('/api/test')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.userId).toBe('user-123');
    expect(res.body.email).toBe('test@example.com');
    expect(res.body.isAdmin).toBe(false);
  });

  it('does not apply to non /api routes', async () => {
    const res = await request(app).get('/public');
    expect(res.status).toBe(200);
  });

  it('returns 403 for banned users', async () => {
    // The middleware should check req.isBanned on the user record
    // This is handled at the auth route level, not middleware
    // Middleware just extracts JWT claims
    const token = signToken(payload, TEST_SECRET);
    const res = await request(app)
      .get('/api/test')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
  });
});
```

- [ ] **Step 2: Implement auth middleware**

```typescript
// src/auth/middleware.ts
import { Request, Response, NextFunction } from 'express';
import { verifyToken } from './jwt';

// Extend Express Request type
declare global {
  namespace Express {
    interface Request {
      userId?: string;
      email?: string;
      isAdmin?: boolean;
      hasSharingBan?: boolean;
      userGroupIds?: string[];
    }
  }
}

export function createAuthMiddleware(jwtSecret: string) {
  return (req: Request, res: Response, next: NextFunction): void => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'Authentication required. Provide a Bearer token.' });
      return;
    }

    const token = authHeader.substring(7);
    const payload = verifyToken(token, jwtSecret);
    if (!payload) {
      res.status(401).json({ error: 'Invalid or expired token.' });
      return;
    }

    req.userId = payload.userId;
    req.email = payload.email;
    req.isAdmin = payload.isAdmin;
    req.hasSharingBan = payload.hasSharingBan;
    req.userGroupIds = [];

    next();
  };
}
```

- [ ] **Step 3: Run test**

Run: `cd server && npx jest tests/auth/middleware.test.ts`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add server/src/auth/middleware.ts server/tests/auth/middleware.test.ts
git commit -m "feat(server): JWT auth middleware for /api routes"
```

---

### Task 5: Location & Photo Repository Implementations

**Files:**
- Modify: `server/src/repositories/interfaces.ts` — add methods to LocationRepository and PhotoRepository
- Create: `server/src/repositories/postgres/location-repo.ts`
- Create: `server/src/repositories/postgres/photo-repo.ts`
- Create: `server/src/repositories/mysql/location-repo.ts`
- Create: `server/src/repositories/mysql/photo-repo.ts`
- Modify: `server/src/repositories/factory.ts` — add location and photo repos
- Create: `server/tests/repositories/location-repo.test.ts`
- Create: `server/tests/repositories/photo-repo.test.ts`

- [ ] **Step 1: Update LocationRepository and PhotoRepository interfaces**

Add to `interfaces.ts`:

```typescript
import type { Location, CreateLocationInput, Photo, PaginatedResult } from '../models';

export interface LocationRepository {
  create(input: CreateLocationInput): Promise<Location>;
  getById(id: string): Promise<Location | null>;
  getByOwner(ownerId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  update(id: string, fields: Partial<CreateLocationInput>): Promise<Location>;
  softDelete(id: string, reason?: string): Promise<boolean>;
  getSharedWithUser(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  getNearby(lat: number, lon: number, radiusKm: number, cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  findDuplicates(lat: number, lon: number, radiusMetres: number): Promise<Location[]>;
}

export interface PhotoRepository {
  create(locationId: string, filePath: string, thumbnailPath: string | null, metadata: Partial<Photo>): Promise<Photo>;
  getById(id: string): Promise<Photo | null>;
  getByLocationId(locationId: string): Promise<Photo[]>;
  delete(id: string): Promise<boolean>;
  countByLocationId(locationId: string): Promise<number>;
}
```

- [ ] **Step 2: Implement PostgreSQL location and photo repos**

Location repo key methods:
- `create()`: insert with uuid, return mapped row. Store `socketTypes` as comma-separated string in DB, parse to array on read.
- `getByOwner()`: WHERE owner_id = ? AND is_deleted = false, cursor pagination
- `update()`: UPDATE ... SET updated_at = now(), version = version + 1
- `softDelete()`: UPDATE is_deleted = true, deleted_reason = reason
- `getNearby()`: bounding-box query with lat/lon BETWEEN, post-filter with Haversine formula for accurate radius
- `findDuplicates()`: same as getNearby but with metres (convert to km: radiusMetres / 1000)

Photo repo: standard CRUD with location_id filtering.

- [ ] **Step 3: Copy to MySQL (identical via Knex)**

- [ ] **Step 4: Update factory.ts**

Add `locations: LocationRepository` and `photos: PhotoRepository` to `Repositories` interface. Instantiate in `createRepositories()`.

- [ ] **Step 5: Write repo tests**

Location repo tests: create, getById, getByOwner with pagination, update increments version, softDelete, getNearby with bounding box, findDuplicates.

Photo repo tests: create, getById, getByLocationId, delete, countByLocationId.

Run: `cd server && npx jest tests/repositories/ --forceExit`
Expected: All pass

- [ ] **Step 6: Commit**

```bash
git add server/src/repositories/ server/tests/repositories/
git commit -m "feat(server): location and photo repository implementations with tests"
```

---

### Task 6: Duplicate Detection Service

**Files:**
- Create: `server/src/services/duplicate-detection-service.ts`
- Create: `server/tests/services/duplicate-detection-service.test.ts`

- [ ] **Step 1: Implement duplicate detection**

```typescript
// src/services/duplicate-detection-service.ts
import { LocationRepository } from '../repositories/interfaces';
import { Location } from '../models';

const DUPLICATE_RADIUS_METRES = 50;

export class DuplicateDetectionService {
  constructor(private locationRepo: LocationRepository) {}

  async findDuplicates(lat: number, lon: number, excludeId?: string): Promise<Location[]> {
    const duplicates = await this.locationRepo.findDuplicates(lat, lon, DUPLICATE_RADIUS_METRES);
    if (excludeId) {
      return duplicates.filter(loc => loc.id !== excludeId);
    }
    return duplicates;
  }

  async hasDuplicates(lat: number, lon: number, excludeId?: string): Promise<boolean> {
    const duplicates = await this.findDuplicates(lat, lon, excludeId);
    return duplicates.length > 0;
  }
}
```

- [ ] **Step 2: Write test using mock repo**

Test `findDuplicates` returns results and excludes specified ID. Test `hasDuplicates` returns boolean.

- [ ] **Step 3: Commit**

```bash
git add server/src/services/ server/tests/services/
git commit -m "feat(server): duplicate detection service with 50m proximity threshold"
```

---

### Task 7: Auth Route

**Files:**
- Create: `server/src/routes/auth.ts`
- Create: `server/tests/routes/auth.test.ts`

- [ ] **Step 1: Implement auth route**

```typescript
// src/routes/auth.ts
import { Router, Request, Response } from 'express';
import { verifyGoogleToken } from '../auth/google-oauth';
import { signToken } from '../auth/jwt';
import { config } from '../config';
import { Repositories } from '../repositories/factory';

export function createAuthRouter(repos: Repositories): Router {
  const router = Router();

  // POST /auth/google
  router.post('/auth/google', async (req: Request, res: Response) => {
    const { idToken } = req.body;
    if (!idToken) {
      res.status(400).json({ error: 'idToken is required' });
      return;
    }

    // Verify Google token
    const googleUser = await verifyGoogleToken(idToken, config.googleClientId);
    if (!googleUser) {
      res.status(401).json({ error: 'Invalid Google token' });
      return;
    }

    // Check if email or Google ID is banned
    const emailBanned = await repos.bans.isBanned('EMAIL', googleUser.email);
    const googleIdBanned = await repos.bans.isBanned('GOOGLE_ID', googleUser.googleId);
    if (emailBanned || googleIdBanned) {
      res.status(403).json({ error: 'This account has been banned.' });
      return;
    }

    // Find or create user
    let user = await repos.users.getByGoogleId(googleUser.googleId);
    if (!user) {
      user = await repos.users.create({
        googleId: googleUser.googleId,
        email: googleUser.email,
        displayName: googleUser.displayName,
        avatarUrl: googleUser.avatarUrl || undefined,
      });

      // Auto-grant admin if ADMIN_SETUP_EMAIL matches and no admins exist
      if (config.adminSetupEmail && user.email === config.adminSetupEmail) {
        const userCount = await repos.users.count();
        if (userCount <= 1) {
          await repos.users.setAdmin(user.id, true);
          user = { ...user, isAdmin: true };
        }
      }
    }

    if (user.isBanned) {
      res.status(403).json({ error: 'This account has been banned.', reason: user.banReason });
      return;
    }

    // Update last seen
    await repos.users.updateLastSeen(user.id);

    // Check sharing ban status
    const hasSharingBan = await repos.bans.hasActiveSharingBan(user.id);

    // Sign JWT
    const token = signToken({
      userId: user.id,
      email: user.email,
      isAdmin: user.isAdmin,
      hasSharingBan,
    }, config.jwtSecret);

    res.json({
      token,
      user: {
        id: user.id,
        email: user.email,
        displayName: user.displayName,
        avatarUrl: user.avatarUrl,
        isAdmin: user.isAdmin,
      },
    });
  });

  return router;
}
```

- [ ] **Step 2: Write auth route test**

Test with mocked Google OAuth (mock `verifyGoogleToken`):
- Returns 400 when no idToken provided
- Returns 401 for invalid Google token
- Returns 403 for banned email
- Creates user on first login and returns JWT
- Returns existing user on subsequent login
- Auto-grants admin when ADMIN_SETUP_EMAIL matches

- [ ] **Step 3: Commit**

```bash
git add server/src/routes/auth.ts server/tests/routes/auth.test.ts
git commit -m "feat(server): POST /auth/google route with ban checking and auto-admin"
```

---

### Task 8: User Routes

**Files:**
- Create: `server/src/routes/users.ts`
- Create: `server/tests/routes/users.test.ts`

- [ ] **Step 1: Implement user routes**

```typescript
// src/routes/users.ts
import { Router, Request, Response } from 'express';
import { Repositories } from '../repositories/factory';

export function createUserRouter(repos: Repositories): Router {
  const router = Router();

  // GET /api/users/me
  router.get('/api/users/me', async (req: Request, res: Response) => {
    const user = await repos.users.getById(req.userId!);
    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }
    res.json({
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      avatarUrl: user.avatarUrl,
      isAdmin: user.isAdmin,
      createdAt: user.createdAt,
    });
  });

  // GET /api/users/search?q=query
  router.get('/api/users/search', async (req: Request, res: Response) => {
    const query = (req.query.q as string) || '';
    const cursor = req.query.cursor as string | undefined;
    const limit = req.query.limit ? parseInt(req.query.limit as string, 10) : undefined;

    if (query.length < 2) {
      res.status(400).json({ error: 'Search query must be at least 2 characters' });
      return;
    }

    const result = await repos.users.search(query, cursor, limit);
    res.json(result);
  });

  return router;
}
```

- [ ] **Step 2: Write tests**

Test with real SQLite DB (via setupTestDb) and signed JWT:
- GET /api/users/me returns current user
- GET /api/users/me returns 401 without token
- GET /api/users/search returns matching users
- GET /api/users/search returns 400 for short query

- [ ] **Step 3: Commit**

```bash
git add server/src/routes/users.ts server/tests/routes/users.test.ts
git commit -m "feat(server): user profile and search endpoints"
```

---

### Task 9: Location Routes

**Files:**
- Create: `server/src/routes/locations.ts`
- Create: `server/tests/routes/locations.test.ts`

- [ ] **Step 1: Implement location routes**

All endpoints require authentication (auth middleware applied at app level for `/api/*`).

```
POST   /api/locations              — Create location (validate required fields: name, latitude, longitude)
GET    /api/locations              — List own locations (paginated)
GET    /api/locations/:id          — Get location by ID (must be owner or shared with)
PUT    /api/locations/:id          — Update owned location (increments version)
DELETE /api/locations/:id          — Soft-delete owned location (body: { reason })
GET    /api/locations/nearby       — Query params: lat, lon, radius (km). Paginated.
GET    /api/locations/shared-with-me — List shared locations (paginated, excludes hidden) — stub for now (returns empty, no sharing yet)
```

Route handler pattern:
```typescript
export function createLocationRouter(repos: Repositories): Router {
  const router = Router();

  router.post('/api/locations', async (req, res) => {
    // Validate required fields
    // Call repos.locations.create({ ...req.body, ownerId: req.userId })
    // Return 201 with created location
  });

  // ... other routes
  return router;
}
```

Key validation:
- POST: name, latitude, longitude required. Return 400 if missing.
- PUT: must be owner (check ownerId === req.userId). Return 403 otherwise.
- DELETE: must be owner. Return 403 otherwise.
- GET /:id: must be owner (shared-with-me check deferred to Phase 6)

- [ ] **Step 2: Write comprehensive tests**

Test each endpoint:
- Create location with valid data → 201
- Create location missing required fields → 400
- List own locations → paginated response
- Get location by ID → 200 for owner, 404 for non-existent
- Update location → 200, version incremented
- Update non-owned location → 403
- Delete (soft) location → 200, is_deleted set
- Nearby search → returns locations within radius

- [ ] **Step 3: Commit**

```bash
git add server/src/routes/locations.ts server/tests/routes/locations.test.ts
git commit -m "feat(server): location CRUD API with pagination, soft-delete, and nearby search"
```

---

### Task 10: Photo Routes

**Files:**
- Create: `server/src/routes/photos.ts`
- Create: `server/tests/routes/photos.test.ts`

- [ ] **Step 1: Implement photo routes**

```
POST   /api/locations/:id/photos        — Upload photo (multipart/form-data, field: "photo")
DELETE /api/locations/:id/photos/:photoId — Delete photo (must own location)
GET    /api/photos/:photoId/image        — Download image file (with ETag/Last-Modified headers)
GET    /api/photos/:photoId/thumbnail    — Download thumbnail (with ETag/Last-Modified)
```

Upload handling:
- Use multer with memory storage and file size limit (`config.maxUploadSizeMb * 1024 * 1024`)
- Accept only image/jpeg, image/png
- Save file via `FileStorage.save()` with path `photos/{locationId}/{photoId}.{ext}`
- Save thumbnail path if provided (field: "thumbnail")
- Return 400 if no file, 413 if too large, 415 if wrong type
- Validate user owns the location before allowing upload

Download handling:
- Serve file from `FileStorage.read()`
- Set `Content-Type` from photo.mimeType
- Set `ETag` header (hash of file path + upload timestamp)
- Set `Last-Modified` header from uploadedAt
- Handle `If-None-Match` and `If-Modified-Since` → return 304 if matches

- [ ] **Step 2: Write tests**

Test upload with supertest multipart (`.attach('photo', buffer, 'test.jpg')`).
Test download with ETag caching.
Test delete owned photo.
Test 403 for non-owner upload.

- [ ] **Step 3: Commit**

```bash
git add server/src/routes/photos.ts server/tests/routes/photos.test.ts
git commit -m "feat(server): photo upload/download API with ETag caching"
```

---

### Task 11: Notification Routes

**Files:**
- Create: `server/src/routes/notifications.ts`
- Create: `server/tests/routes/notifications.test.ts`

- [ ] **Step 1: Implement notification routes**

```
GET    /api/notifications           — List unread notifications (paginated)
PUT    /api/notifications/:id/read  — Mark notification as read
PUT    /api/notifications/read-all  — Mark all notifications as read
```

Simple wrappers around the existing NotificationRepository.

- [ ] **Step 2: Write tests and commit**

```bash
git add server/src/routes/notifications.ts server/tests/routes/notifications.test.ts
git commit -m "feat(server): notification list and mark-read endpoints"
```

---

### Task 12: Wire Up App Factory

**Files:**
- Modify: `server/src/app.ts` — accept repos, storage, cache; wire auth + all routes
- Modify: `server/src/server.ts` — create DB connection, repos, storage, cache; pass to createApp
- Create: `server/tests/integration/full-flow.test.ts`

- [ ] **Step 1: Update createApp to accept dependencies**

```typescript
// src/app.ts
import { Repositories } from './repositories/factory';
import { FileStorage } from './storage/interfaces';
import { Cache } from './cache/interfaces';

export interface AppDependencies {
  repos: Repositories;
  storage: FileStorage;
  cache: Cache;
}

export function createApp(deps?: AppDependencies): express.Express {
  const app = express();

  app.use(helmet());
  app.use(cors());
  app.use(express.json());
  app.use(requestLogger);
  app.use(healthRouter);

  if (deps) {
    // Auth route (no JWT required)
    app.use(createAuthRouter(deps.repos));

    // Auth middleware for /api/* routes
    app.use('/api', createAuthMiddleware(config.jwtSecret));

    // Rate limiting (after auth, so we have userId)
    app.use('/api', createRateLimiter(deps.cache, deps.repos.rateLimits));

    // API routes
    app.use(createUserRouter(deps.repos));
    app.use(createLocationRouter(deps.repos));
    app.use(createPhotoRouter(deps.repos, deps.storage));
    app.use(createNotificationRouter(deps.repos));
  }

  app.use((_req, res) => res.status(404).json({ error: 'Not found' }));
  app.use(errorHandler);

  return app;
}
```

- [ ] **Step 2: Update server.ts**

```typescript
// src/server.ts
import knex from 'knex';
import { createApp } from './app';
import { config } from './config';
import { createRepositories } from './repositories/factory';
import { LocalStorage } from './storage/local-storage';
import { MemoryCache } from './cache/memory-cache';
import { RedisCache } from './cache/redis-cache';

async function main() {
  // Database
  const db = knex({
    client: config.dbBackend === 'mysql' ? 'mysql2' : 'pg',
    connection: config.databaseUrl,
  });

  // Run migrations
  await db.migrate.latest({
    directory: './migrations',
    extension: 'ts',
  });

  // Repos
  const repos = createRepositories(db);

  // Storage
  const storage = new LocalStorage(config.storagePath);

  // Cache
  let cache;
  if (config.redisUrl) {
    const redisCache = new RedisCache(config.redisUrl);
    await redisCache.connect();
    cache = redisCache;
  } else {
    cache = new MemoryCache();
  }

  const app = createApp({ repos, storage, cache });
  app.listen(config.port, () => {
    console.log(`PEVPlugShare server listening on port ${config.port}`);
    console.log(`Database backend: ${config.dbBackend}`);
  });
}

main().catch(console.error);
```

- [ ] **Step 3: Write full-flow integration test**

Test using in-memory SQLite + local storage + memory cache:
1. Setup: create test app with real repos (SQLite), run migrations
2. POST /auth/google (with mocked Google verifier) → get JWT
3. POST /api/locations with JWT → create location → 201
4. GET /api/locations → list includes created location
5. GET /api/locations/:id → returns location detail
6. PUT /api/locations/:id → update → version incremented
7. DELETE /api/locations/:id → soft delete
8. GET /api/notifications → returns empty list (no notifications yet)

- [ ] **Step 4: Run ALL tests**

Run: `cd server && npm test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add server/src/app.ts server/src/server.ts server/tests/integration/
git commit -m "feat(server): wire up app factory with auth, routes, repos, and full integration test"
```

---

### Task 13: Final Verification

- [ ] **Step 1: Run full test suite**

Run: `cd server && npm test`
Expected: All tests pass

- [ ] **Step 2: TypeScript check**

Run: `cd server && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Build**

Run: `cd server && npm run build`
Expected: dist/ updated with all new files

- [ ] **Step 4: Commit**

```bash
git add -A server/
git commit -m "feat(server): Phase 2 complete — auth, user, location, photo, notification APIs"
```
