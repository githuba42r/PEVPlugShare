# Central Location Server — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Node.js/TypeScript/Express server foundation with abstracted data layer, database migrations, file storage, caching, configuration, rate limiting, and health check — everything needed before auth and API endpoints are added in Phase 2.

**Architecture:** Express app factory (`createApp()`) with environment-based configuration. All data access goes through TypeScript interfaces with swappable implementations (PostgreSQL via Knex, MySQL via Knex, in-memory for tests). File storage abstracted behind an interface (local filesystem initially). Optional Redis cache with in-memory fallback. Rate limiting middleware with tiered resolution (user > group > global). The server lives at `server/` within the PEVPlugShare monorepo.

**Tech Stack:** Node.js 20, TypeScript 5, Express 4, Knex (query builder + migrations), pg (PostgreSQL driver), mysql2 (MySQL driver), ioredis (Redis client), Jest + Supertest (testing), Docker

**Spec:** `docs/features/CENTRAL_LOCATION_SERVER_SPEC.md`

---

## Scope — What Phase 1 Builds

Phase 1 creates the infrastructure that all subsequent phases build on:

1. **Project scaffolding** — package.json, tsconfig, scripts, directory structure
2. **Configuration** — env-based config with validation
3. **Domain models** — TypeScript types for all 13 spec tables
4. **Database migrations** — Knex migrations for ALL tables in the spec (users, locations, photos, friendships, groups, group_members, group_invitations, location_shares, direct_share_tokens, hidden_shares, change_suggestions, notifications, rate_limits, banned_identifiers, sharing_bans)
5. **Repository interfaces** — TypeScript interfaces for all repositories
6. **Repository implementations** — PostgreSQL and MySQL implementations for Phase 1 tables (users, rate_limits, banned_identifiers, sharing_bans, notifications). Other tables get stub implementations that throw "not yet implemented"
7. **Cache layer** — Redis cache + in-memory fallback
8. **File storage** — Local filesystem + interface for S3
9. **Rate limiting** — Middleware with tiered resolution
10. **Health check** — `/health` endpoint
11. **Error handling** — Middleware for consistent error responses
12. **Express app** — App factory, server entrypoint
13. **Docker** — Dockerfile, .dockerignore
14. **Tests** — Unit + integration tests for all implemented components

## File Structure

### New Files (all under `server/`)

```
server/
  package.json
  tsconfig.json
  jest.config.ts
  .dockerignore
  Dockerfile
  .env.example
  knexfile.ts

  src/
    app.ts                          # Express app factory
    server.ts                       # HTTP server entrypoint
    config.ts                       # Env-based configuration with validation

    models/
      common.ts                     # PaginatedResult, pagination helpers
      user.ts
      location.ts
      photo.ts
      friendship.ts
      group.ts
      share.ts
      suggestion.ts
      rate-limit.ts
      ban.ts
      notification.ts
      deep-link.ts

    repositories/
      interfaces.ts                 # All repository interfaces
      factory.ts                    # Creates repos based on config.DB_BACKEND
      postgres/
        connection.ts               # Knex instance for PostgreSQL
        user-repo.ts
        rate-limit-repo.ts
        ban-repo.ts
        notification-repo.ts
      mysql/
        connection.ts               # Knex instance for MySQL
        user-repo.ts
        rate-limit-repo.ts
        ban-repo.ts
        notification-repo.ts

    storage/
      interfaces.ts                 # FileStorage interface
      local-storage.ts              # Local filesystem implementation

    cache/
      interfaces.ts                 # Cache interface
      redis-cache.ts                # Redis implementation
      memory-cache.ts               # In-memory fallback

    middleware/
      error-handler.ts              # Global error handling
      rate-limiter.ts               # Tiered rate limiting middleware
      request-logger.ts             # Request logging

    routes/
      health.ts                     # GET /health

  migrations/
    001_create_users.ts
    002_create_banned_identifiers.ts
    003_create_sharing_bans.ts
    004_create_server_locations.ts
    005_create_server_location_photos.ts
    006_create_friendships.ts
    007_create_friend_groups.ts
    008_create_friend_group_members.ts
    009_create_group_invitations.ts
    010_create_location_shares.ts
    011_create_direct_share_tokens.ts
    012_create_hidden_shares.ts
    013_create_change_suggestions.ts
    014_create_notifications.ts
    015_create_rate_limits.ts
    016_seed_default_rate_limits.ts

  tests/
    setup.ts                        # Test setup (in-memory DB, etc.)
    config.test.ts
    cache/
      memory-cache.test.ts
      redis-cache.test.ts
    storage/
      local-storage.test.ts
    middleware/
      rate-limiter.test.ts
      error-handler.test.ts
    repositories/
      user-repo.test.ts
      rate-limit-repo.test.ts
      ban-repo.test.ts
      notification-repo.test.ts
    routes/
      health.test.ts
    integration/
      app.test.ts                   # Full app integration test
```

---

### Task 1: Project Scaffolding

**Files:**
- Create: `server/package.json`
- Create: `server/tsconfig.json`
- Create: `server/jest.config.ts`
- Create: `server/.dockerignore`
- Create: `server/.env.example`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "pevplugshare-server",
  "version": "0.1.0",
  "description": "PEVPlugShare central location server",
  "main": "dist/server.js",
  "scripts": {
    "build": "tsc",
    "start": "node dist/server.js",
    "dev": "ts-node-dev --respawn --transpile-only src/server.ts",
    "test": "jest --forceExit --detectOpenHandles",
    "test:watch": "jest --watch",
    "lint": "tsc --noEmit",
    "migrate": "knex migrate:latest --knexfile knexfile.ts",
    "migrate:rollback": "knex migrate:rollback --knexfile knexfile.ts",
    "migrate:make": "knex migrate:make --knexfile knexfile.ts"
  },
  "dependencies": {
    "express": "^4.21.0",
    "knex": "^3.1.0",
    "pg": "^8.13.0",
    "mysql2": "^3.11.0",
    "ioredis": "^5.4.1",
    "uuid": "^10.0.0",
    "dotenv": "^16.4.5",
    "helmet": "^8.0.0",
    "cors": "^2.8.5",
    "morgan": "^1.10.0"
  },
  "devDependencies": {
    "typescript": "^5.6.0",
    "ts-node": "^10.9.2",
    "ts-node-dev": "^2.0.0",
    "@types/node": "^20.16.0",
    "@types/express": "^4.17.21",
    "@types/uuid": "^10.0.0",
    "@types/cors": "^2.8.17",
    "@types/morgan": "^1.9.9",
    "jest": "^29.7.0",
    "ts-jest": "^29.2.0",
    "@types/jest": "^29.5.0",
    "supertest": "^7.0.0",
    "@types/supertest": "^6.0.2",
    "better-sqlite3": "^11.3.0",
    "@types/better-sqlite3": "^7.6.11"
  }
}
```

- [ ] **Step 2: Create tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "commonjs",
    "lib": ["ES2022"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "moduleResolution": "node"
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist", "tests"]
}
```

- [ ] **Step 3: Create jest.config.ts**

```typescript
import type { Config } from 'jest';

const config: Config = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/tests'],
  testMatch: ['**/*.test.ts'],
  moduleFileExtensions: ['ts', 'js', 'json'],
  collectCoverageFrom: ['src/**/*.ts', '!src/server.ts', '!src/lambda.ts'],
  setupFilesAfterSetup: [],
};

export default config;
```

- [ ] **Step 4: Create .dockerignore**

```
node_modules
dist
.env
*.test.ts
tests/
coverage/
.git
```

- [ ] **Step 5: Create .env.example**

```
DB_BACKEND=postgres
DATABASE_URL=postgresql://pevplugshare:devpassword@localhost:5432/pevplugshare
REDIS_URL=redis://localhost:6379/0
GOOGLE_CLIENT_ID=your-google-client-id
JWT_SECRET=dev-jwt-secret-change-in-production
STORAGE_PATH=./data/photos
SERVER_BASE_URL=http://localhost:8080
LOG_LEVEL=debug
MAX_UPLOAD_SIZE_MB=5
ADMIN_SETUP_EMAIL=
PORT=8080
```

- [ ] **Step 6: Install dependencies**

Run: `cd server && npm install`
Expected: `node_modules` created, no errors

- [ ] **Step 7: Verify TypeScript compiles (empty project)**

Create a minimal `src/config.ts` placeholder:
```typescript
export const PORT = process.env.PORT || 8080;
```

Run: `cd server && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 8: Commit**

```bash
git add server/
git commit -m "feat(server): project scaffolding — package.json, tsconfig, jest, docker"
```

---

### Task 2: Configuration

**Files:**
- Create: `server/src/config.ts`
- Create: `server/tests/config.test.ts`

- [ ] **Step 1: Write config test**

```typescript
// tests/config.test.ts
describe('config', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('uses defaults when env vars not set', () => {
    delete process.env.PORT;
    delete process.env.DB_BACKEND;
    delete process.env.LOG_LEVEL;
    const { config } = require('../src/config');
    expect(config.port).toBe(8080);
    expect(config.dbBackend).toBe('postgres');
    expect(config.logLevel).toBe('info');
  });

  it('reads values from environment', () => {
    process.env.PORT = '3000';
    process.env.DB_BACKEND = 'mysql';
    process.env.DATABASE_URL = 'mysql://localhost/test';
    process.env.JWT_SECRET = 'test-secret';
    process.env.GOOGLE_CLIENT_ID = 'test-client';
    process.env.STORAGE_PATH = '/tmp/photos';
    process.env.SERVER_BASE_URL = 'https://example.com';
    process.env.LOG_LEVEL = 'debug';
    process.env.MAX_UPLOAD_SIZE_MB = '10';
    const { config } = require('../src/config');
    expect(config.port).toBe(3000);
    expect(config.dbBackend).toBe('mysql');
    expect(config.databaseUrl).toBe('mysql://localhost/test');
    expect(config.jwtSecret).toBe('test-secret');
    expect(config.googleClientId).toBe('test-client');
    expect(config.storagePath).toBe('/tmp/photos');
    expect(config.serverBaseUrl).toBe('https://example.com');
    expect(config.logLevel).toBe('debug');
    expect(config.maxUploadSizeMb).toBe(10);
  });

  it('validates DB_BACKEND is a known value', () => {
    process.env.DB_BACKEND = 'oracle';
    expect(() => require('../src/config')).toThrow();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd server && npx jest tests/config.test.ts`
Expected: FAIL

- [ ] **Step 3: Implement config**

```typescript
// src/config.ts
import dotenv from 'dotenv';
dotenv.config();

const VALID_DB_BACKENDS = ['postgres', 'mysql'] as const;
type DbBackend = typeof VALID_DB_BACKENDS[number];

export interface Config {
  port: number;
  dbBackend: DbBackend;
  databaseUrl: string;
  redisUrl: string | undefined;
  googleClientId: string;
  jwtSecret: string;
  storagePath: string;
  serverBaseUrl: string;
  logLevel: string;
  maxUploadSizeMb: number;
  adminSetupEmail: string | undefined;
}

function loadConfig(): Config {
  const dbBackend = (process.env.DB_BACKEND || 'postgres') as string;
  if (!VALID_DB_BACKENDS.includes(dbBackend as DbBackend)) {
    throw new Error(`Invalid DB_BACKEND: ${dbBackend}. Must be one of: ${VALID_DB_BACKENDS.join(', ')}`);
  }

  return {
    port: parseInt(process.env.PORT || '8080', 10),
    dbBackend: dbBackend as DbBackend,
    databaseUrl: process.env.DATABASE_URL || '',
    redisUrl: process.env.REDIS_URL,
    googleClientId: process.env.GOOGLE_CLIENT_ID || '',
    jwtSecret: process.env.JWT_SECRET || '',
    storagePath: process.env.STORAGE_PATH || './data/photos',
    serverBaseUrl: process.env.SERVER_BASE_URL || 'http://localhost:8080',
    logLevel: process.env.LOG_LEVEL || 'info',
    maxUploadSizeMb: parseInt(process.env.MAX_UPLOAD_SIZE_MB || '5', 10),
    adminSetupEmail: process.env.ADMIN_SETUP_EMAIL || undefined,
  };
}

export const config = loadConfig();
```

- [ ] **Step 4: Run tests**

Run: `cd server && npx jest tests/config.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/src/config.ts server/tests/config.test.ts
git commit -m "feat(server): environment-based configuration with validation"
```

---

### Task 3: Domain Models

**Files:**
- Create: `server/src/models/common.ts`
- Create: `server/src/models/user.ts`
- Create: `server/src/models/location.ts`
- Create: `server/src/models/photo.ts`
- Create: `server/src/models/friendship.ts`
- Create: `server/src/models/group.ts`
- Create: `server/src/models/share.ts`
- Create: `server/src/models/suggestion.ts`
- Create: `server/src/models/rate-limit.ts`
- Create: `server/src/models/ban.ts`
- Create: `server/src/models/notification.ts`
- Create: `server/src/models/deep-link.ts`
- Create: `server/src/models/index.ts`

- [ ] **Step 1: Create common types**

```typescript
// src/models/common.ts
export interface PaginatedResult<T> {
  data: T[];
  pagination: {
    cursor: string | null;
    hasMore: boolean;
  };
}

export interface PaginationParams {
  cursor?: string;
  limit?: number;
}

export const DEFAULT_PAGE_LIMIT = 20;
export const MAX_PAGE_LIMIT = 100;

export function clampLimit(limit?: number): number {
  const l = limit ?? DEFAULT_PAGE_LIMIT;
  return Math.min(Math.max(1, l), MAX_PAGE_LIMIT);
}
```

- [ ] **Step 2: Create all model files**

```typescript
// src/models/user.ts
export interface User {
  id: string;
  googleId: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
  isAdmin: boolean;
  isBanned: boolean;
  banReason: string | null;
  bannedAt: Date | null;
  bannedBy: string | null;
  createdAt: Date;
  lastSeenAt: Date;
}

export interface CreateUserInput {
  googleId: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
}
```

```typescript
// src/models/location.ts
export interface Location {
  id: string;
  ownerId: string;
  name: string;
  latitude: number;
  longitude: number;
  description: string | null;
  additionalInfo: string | null;
  openingHours: string | null;
  socketTypes: string | null;
  gpoCount: number | null;
  hasFee: boolean | null;
  chargingCosts: string | null;
  publicPrivate: string | null;
  providerName: string | null;
  providerWebsite: string | null;
  businessName: string | null;
  streetAddress: string | null;
  suburb: string | null;
  state: string | null;
  postcode: string | null;
  color: string | null;
  icon: string | null;
  isDeleted: boolean;
  deletedReason: string | null;
  createdAt: Date;
  updatedAt: Date;
  version: number;
}

export interface CreateLocationInput {
  ownerId: string;
  name: string;
  latitude: number;
  longitude: number;
  description?: string;
  additionalInfo?: string;
  openingHours?: string;
  socketTypes?: string;
  gpoCount?: number;
  hasFee?: boolean;
  chargingCosts?: string;
  publicPrivate?: string;
  providerName?: string;
  providerWebsite?: string;
  businessName?: string;
  streetAddress?: string;
  suburb?: string;
  state?: string;
  postcode?: string;
  color?: string;
  icon?: string;
}
```

```typescript
// src/models/photo.ts
export interface Photo {
  id: string;
  locationId: string;
  filePath: string;
  thumbnailPath: string | null;
  caption: string | null;
  latitude: number | null;
  longitude: number | null;
  altitude: number | null;
  bearing: number | null;
  timestamp: number | null;
  exifData: string | null;
  width: number | null;
  height: number | null;
  fileSize: number | null;
  mimeType: string;
  uploadedAt: Date;
}
```

```typescript
// src/models/friendship.ts
export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'BLOCKED';

export interface Friendship {
  id: string;
  requesterId: string;
  addresseeId: string;
  status: FriendshipStatus;
  requestedAt: Date;
  respondedAt: Date | null;
}
```

```typescript
// src/models/group.ts
export type GroupMemberRole = 'ADMIN' | 'MEMBER';

export interface FriendGroup {
  id: string;
  ownerId: string;
  name: string;
  description: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export interface GroupMember {
  groupId: string;
  userId: string;
  role: GroupMemberRole;
  addedAt: Date;
}

export type GroupInvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED';

export interface GroupInvitation {
  id: string;
  groupId: string;
  invitedBy: string;
  inviteeId: string | null;
  inviteeEmail: string | null;
  token: string;
  status: GroupInvitationStatus;
  createdAt: Date;
  expiresAt: Date | null;
  respondedAt: Date | null;
  respondedBy: string | null;
}
```

```typescript
// src/models/share.ts
export interface LocationShare {
  id: string;
  locationId: string;
  sharedBy: string;
  sharedWithUser: string | null;
  sharedWithGroup: string | null;
  isActive: boolean;
  sharedAt: Date;
}

export interface HiddenShare {
  id: string;
  userId: string;
  locationId: string;
  shareId: string;
  hiddenAt: Date;
}
```

```typescript
// src/models/suggestion.ts
export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export interface ChangeSuggestion {
  id: string;
  locationId: string;
  suggestedBy: string;
  status: SuggestionStatus;
  changesJson: string;
  message: string | null;
  responseMsg: string | null;
  createdAt: Date;
  respondedAt: Date | null;
}
```

```typescript
// src/models/rate-limit.ts
export type RateLimitScope = 'GLOBAL' | 'GROUP' | 'USER';

export interface RateLimit {
  id: string;
  scope: RateLimitScope;
  scopeId: string | null;
  endpoint: string;
  maxRequests: number;
  windowSeconds: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateRateLimitInput {
  scope: RateLimitScope;
  scopeId?: string;
  endpoint: string;
  maxRequests: number;
  windowSeconds: number;
}

export interface ResolvedRateLimit {
  maxRequests: number;
  windowSeconds: number;
  source: RateLimitScope;
  sourceId: string | null;
}
```

```typescript
// src/models/ban.ts
export type BanIdentifierType = 'EMAIL' | 'GOOGLE_ID';

export interface BannedIdentifier {
  id: string;
  identifierType: BanIdentifierType;
  identifier: string;
  reason: string | null;
  bannedBy: string | null;
  createdAt: Date;
}

export interface SharingBan {
  id: string;
  userId: string;
  reason: string;
  bannedBy: string;
  expiresAt: Date | null;
  createdAt: Date;
}
```

```typescript
// src/models/notification.ts
export type NotificationType =
  | 'FRIEND_REQUEST'
  | 'SHARE_RECEIVED'
  | 'SUGGESTION_RECEIVED'
  | 'SUGGESTION_ACCEPTED'
  | 'SUGGESTION_REJECTED'
  | 'LOCATION_DELETED'
  | 'GROUP_INVITE'
  | 'DUPLICATE_DETECTED';

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  payloadJson: string;
  isRead: boolean;
  createdAt: Date;
}
```

```typescript
// src/models/deep-link.ts
export interface DirectShareToken {
  id: string;
  locationId: string;
  createdBy: string;
  token: string;
  isSingleUse: boolean;
  useCount: number;
  maxUses: number | null;
  expiresAt: Date | null;
  createdAt: Date;
}
```

```typescript
// src/models/index.ts
export * from './common';
export * from './user';
export * from './location';
export * from './photo';
export * from './friendship';
export * from './group';
export * from './share';
export * from './suggestion';
export * from './rate-limit';
export * from './ban';
export * from './notification';
export * from './deep-link';
```

- [ ] **Step 3: Verify compilation**

Run: `cd server && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add server/src/models/
git commit -m "feat(server): domain models for all spec tables"
```

---

### Task 4: Database Migrations

**Files:**
- Create: `server/knexfile.ts`
- Create: `server/migrations/001_create_users.ts` through `server/migrations/016_seed_default_rate_limits.ts`

- [ ] **Step 1: Create knexfile.ts**

```typescript
// knexfile.ts
import type { Knex } from 'knex';
import dotenv from 'dotenv';
dotenv.config();

const dbBackend = process.env.DB_BACKEND || 'postgres';

const baseConfig: Partial<Knex.Config> = {
  migrations: {
    directory: './migrations',
    extension: 'ts',
  },
};

const configs: Record<string, Knex.Config> = {
  postgres: {
    ...baseConfig,
    client: 'pg',
    connection: process.env.DATABASE_URL,
  },
  mysql: {
    ...baseConfig,
    client: 'mysql2',
    connection: process.env.DATABASE_URL,
  },
  test: {
    ...baseConfig,
    client: 'better-sqlite3',
    connection: { filename: ':memory:' },
    useNullAsDefault: true,
  },
};

const config = configs[process.env.NODE_ENV === 'test' ? 'test' : dbBackend];
if (!config) {
  throw new Error(`Unknown DB_BACKEND: ${dbBackend}`);
}

export default config;
module.exports = config;
```

- [ ] **Step 2: Create all migration files**

Each migration creates one table. I'll show the first few; the implementer should create all 16 files following this exact pattern.

```typescript
// migrations/001_create_users.ts
import { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('users', (t) => {
    t.uuid('id').primary();
    t.string('google_id').unique().notNullable();
    t.string('email').unique().notNullable();
    t.string('display_name').notNullable();
    t.string('avatar_url');
    t.boolean('is_admin').defaultTo(false);
    t.boolean('is_banned').defaultTo(false);
    t.string('ban_reason');
    t.timestamp('banned_at');
    t.uuid('banned_by').references('id').inTable('users');
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('last_seen_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists('users');
}
```

```typescript
// migrations/002_create_banned_identifiers.ts
import { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('banned_identifiers', (t) => {
    t.uuid('id').primary();
    t.string('identifier_type').notNullable();
    t.string('identifier').notNullable();
    t.string('reason');
    t.uuid('banned_by').references('id').inTable('users');
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.unique(['identifier_type', 'identifier']);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists('banned_identifiers');
}
```

```typescript
// migrations/003_create_sharing_bans.ts
import { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('sharing_bans', (t) => {
    t.uuid('id').primary();
    t.uuid('user_id').references('id').inTable('users').notNullable();
    t.string('reason').notNullable();
    t.uuid('banned_by').references('id').inTable('users').notNullable();
    t.timestamp('expires_at');
    t.timestamp('created_at').defaultTo(knex.fn.now());
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists('sharing_bans');
}
```

```typescript
// migrations/004_create_server_locations.ts
import { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('server_locations', (t) => {
    t.uuid('id').primary();
    t.uuid('owner_id').references('id').inTable('users').notNullable();
    t.string('name').notNullable();
    t.double('latitude').notNullable();
    t.double('longitude').notNullable();
    t.text('description');
    t.text('additional_info');
    t.string('opening_hours');
    t.string('socket_types');
    t.integer('gpo_count');
    t.boolean('has_fee');
    t.string('charging_costs');
    t.string('public_private');
    t.string('provider_name');
    t.string('provider_website');
    t.string('business_name');
    t.string('street_address');
    t.string('suburb');
    t.string('state');
    t.string('postcode');
    t.string('color');
    t.string('icon');
    t.boolean('is_deleted').defaultTo(false);
    t.string('deleted_reason');
    t.timestamp('created_at').defaultTo(knex.fn.now());
    t.timestamp('updated_at').defaultTo(knex.fn.now());
    t.integer('version').defaultTo(1);
    t.index(['latitude', 'longitude']);
    t.index('owner_id');
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists('server_locations');
}
```

Continue this pattern for migrations 005-015 (photos, friendships, friend_groups, friend_group_members, group_invitations, location_shares, direct_share_tokens, hidden_shares, change_suggestions, notifications, rate_limits). Each migration creates the table exactly as defined in the spec's Data Model section, with proper foreign keys, defaults, and indexes.

```typescript
// migrations/016_seed_default_rate_limits.ts
import { Knex } from 'knex';
import { v4 as uuidv4 } from 'uuid';

export async function up(knex: Knex): Promise<void> {
  const now = new Date();
  await knex('rate_limits').insert([
    { id: uuidv4(), scope: 'GLOBAL', scope_id: null, endpoint: 'POST /api/locations', max_requests: 100, window_seconds: 3600, created_at: now, updated_at: now },
    { id: uuidv4(), scope: 'GLOBAL', scope_id: null, endpoint: 'POST /api/locations/*/photos', max_requests: 200, window_seconds: 3600, created_at: now, updated_at: now },
    { id: uuidv4(), scope: 'GLOBAL', scope_id: null, endpoint: 'POST /api/friends/request', max_requests: 50, window_seconds: 3600, created_at: now, updated_at: now },
    { id: uuidv4(), scope: 'GLOBAL', scope_id: null, endpoint: '*', max_requests: 1000, window_seconds: 3600, created_at: now, updated_at: now },
  ]);
}

export async function down(knex: Knex): Promise<void> {
  await knex('rate_limits').where('scope', 'GLOBAL').del();
}
```

- [ ] **Step 3: Test migrations run against SQLite (test mode)**

Run: `cd server && NODE_ENV=test npx knex migrate:latest --knexfile knexfile.ts`
Expected: All migrations applied successfully

- [ ] **Step 4: Commit**

```bash
git add server/knexfile.ts server/migrations/
git commit -m "feat(server): database migrations for all 15 spec tables + default rate limits"
```

---

### Task 5: Cache Layer

**Files:**
- Create: `server/src/cache/interfaces.ts`
- Create: `server/src/cache/memory-cache.ts`
- Create: `server/src/cache/redis-cache.ts`
- Create: `server/tests/cache/memory-cache.test.ts`

- [ ] **Step 1: Write cache interface**

```typescript
// src/cache/interfaces.ts
export interface Cache {
  get(key: string): Promise<string | null>;
  set(key: string, value: string, ttlSeconds?: number): Promise<void>;
  del(key: string): Promise<void>;
  increment(key: string, ttlSeconds?: number): Promise<number>;
  exists(key: string): Promise<boolean>;
}
```

- [ ] **Step 2: Write memory cache test**

```typescript
// tests/cache/memory-cache.test.ts
import { MemoryCache } from '../../src/cache/memory-cache';

describe('MemoryCache', () => {
  let cache: MemoryCache;

  beforeEach(() => {
    cache = new MemoryCache();
  });

  it('returns null for missing keys', async () => {
    expect(await cache.get('missing')).toBeNull();
  });

  it('stores and retrieves values', async () => {
    await cache.set('key', 'value');
    expect(await cache.get('key')).toBe('value');
  });

  it('deletes values', async () => {
    await cache.set('key', 'value');
    await cache.del('key');
    expect(await cache.get('key')).toBeNull();
  });

  it('increments counters', async () => {
    expect(await cache.increment('counter')).toBe(1);
    expect(await cache.increment('counter')).toBe(2);
    expect(await cache.increment('counter')).toBe(3);
  });

  it('checks existence', async () => {
    expect(await cache.exists('key')).toBe(false);
    await cache.set('key', 'value');
    expect(await cache.exists('key')).toBe(true);
  });

  it('expires values after TTL', async () => {
    await cache.set('key', 'value', 1); // 1 second TTL
    expect(await cache.get('key')).toBe('value');
    await new Promise(r => setTimeout(r, 1100));
    expect(await cache.get('key')).toBeNull();
  });
});
```

- [ ] **Step 3: Implement memory cache**

```typescript
// src/cache/memory-cache.ts
import { Cache } from './interfaces';

interface CacheEntry {
  value: string;
  expiresAt: number | null;
}

export class MemoryCache implements Cache {
  private store = new Map<string, CacheEntry>();

  private isExpired(entry: CacheEntry): boolean {
    return entry.expiresAt !== null && Date.now() > entry.expiresAt;
  }

  async get(key: string): Promise<string | null> {
    const entry = this.store.get(key);
    if (!entry || this.isExpired(entry)) {
      if (entry) this.store.delete(key);
      return null;
    }
    return entry.value;
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    const expiresAt = ttlSeconds ? Date.now() + ttlSeconds * 1000 : null;
    this.store.set(key, { value, expiresAt });
  }

  async del(key: string): Promise<void> {
    this.store.delete(key);
  }

  async increment(key: string, ttlSeconds?: number): Promise<number> {
    const current = await this.get(key);
    const next = (current ? parseInt(current, 10) : 0) + 1;
    await this.set(key, next.toString(), ttlSeconds);
    return next;
  }

  async exists(key: string): Promise<boolean> {
    return (await this.get(key)) !== null;
  }
}
```

- [ ] **Step 4: Implement Redis cache**

```typescript
// src/cache/redis-cache.ts
import Redis from 'ioredis';
import { Cache } from './interfaces';

export class RedisCache implements Cache {
  private client: Redis;

  constructor(redisUrl: string) {
    this.client = new Redis(redisUrl, { lazyConnect: true });
  }

  async connect(): Promise<void> {
    await this.client.connect();
  }

  async disconnect(): Promise<void> {
    await this.client.quit();
  }

  async get(key: string): Promise<string | null> {
    return this.client.get(key);
  }

  async set(key: string, value: string, ttlSeconds?: number): Promise<void> {
    if (ttlSeconds) {
      await this.client.setex(key, ttlSeconds, value);
    } else {
      await this.client.set(key, value);
    }
  }

  async del(key: string): Promise<void> {
    await this.client.del(key);
  }

  async increment(key: string, ttlSeconds?: number): Promise<number> {
    const val = await this.client.incr(key);
    if (ttlSeconds && val === 1) {
      await this.client.expire(key, ttlSeconds);
    }
    return val;
  }

  async exists(key: string): Promise<boolean> {
    return (await this.client.exists(key)) === 1;
  }
}
```

- [ ] **Step 5: Run tests**

Run: `cd server && npx jest tests/cache/memory-cache.test.ts`
Expected: PASS (6 tests)

- [ ] **Step 6: Commit**

```bash
git add server/src/cache/ server/tests/cache/
git commit -m "feat(server): cache layer — memory cache + Redis cache with shared interface"
```

---

### Task 6: File Storage

**Files:**
- Create: `server/src/storage/interfaces.ts`
- Create: `server/src/storage/local-storage.ts`
- Create: `server/tests/storage/local-storage.test.ts`

- [ ] **Step 1: Write storage interface**

```typescript
// src/storage/interfaces.ts
export interface FileStorage {
  save(relativePath: string, data: Buffer): Promise<void>;
  read(relativePath: string): Promise<Buffer>;
  delete(relativePath: string): Promise<void>;
  exists(relativePath: string): Promise<boolean>;
  getAbsolutePath(relativePath: string): string;
}
```

- [ ] **Step 2: Write local storage test**

```typescript
// tests/storage/local-storage.test.ts
import { LocalStorage } from '../../src/storage/local-storage';
import fs from 'fs';
import path from 'path';
import os from 'os';

describe('LocalStorage', () => {
  let storage: LocalStorage;
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pevtest-'));
    storage = new LocalStorage(tmpDir);
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it('saves and reads files', async () => {
    const data = Buffer.from('hello world');
    await storage.save('test/file.txt', data);
    const result = await storage.read('test/file.txt');
    expect(result.toString()).toBe('hello world');
  });

  it('checks file existence', async () => {
    expect(await storage.exists('missing.txt')).toBe(false);
    await storage.save('exists.txt', Buffer.from('data'));
    expect(await storage.exists('exists.txt')).toBe(true);
  });

  it('deletes files', async () => {
    await storage.save('delete-me.txt', Buffer.from('data'));
    await storage.delete('delete-me.txt');
    expect(await storage.exists('delete-me.txt')).toBe(false);
  });

  it('creates nested directories automatically', async () => {
    await storage.save('a/b/c/deep.txt', Buffer.from('deep'));
    const result = await storage.read('a/b/c/deep.txt');
    expect(result.toString()).toBe('deep');
  });

  it('returns absolute path', () => {
    const abs = storage.getAbsolutePath('photos/img.jpg');
    expect(abs).toBe(path.join(tmpDir, 'photos/img.jpg'));
  });
});
```

- [ ] **Step 3: Implement local storage**

```typescript
// src/storage/local-storage.ts
import fs from 'fs/promises';
import path from 'path';
import { FileStorage } from './interfaces';

export class LocalStorage implements FileStorage {
  constructor(private basePath: string) {}

  getAbsolutePath(relativePath: string): string {
    return path.join(this.basePath, relativePath);
  }

  async save(relativePath: string, data: Buffer): Promise<void> {
    const fullPath = this.getAbsolutePath(relativePath);
    await fs.mkdir(path.dirname(fullPath), { recursive: true });
    await fs.writeFile(fullPath, data);
  }

  async read(relativePath: string): Promise<Buffer> {
    return fs.readFile(this.getAbsolutePath(relativePath));
  }

  async delete(relativePath: string): Promise<void> {
    try {
      await fs.unlink(this.getAbsolutePath(relativePath));
    } catch (err: any) {
      if (err.code !== 'ENOENT') throw err;
    }
  }

  async exists(relativePath: string): Promise<boolean> {
    try {
      await fs.access(this.getAbsolutePath(relativePath));
      return true;
    } catch {
      return false;
    }
  }
}
```

- [ ] **Step 4: Run tests**

Run: `cd server && npx jest tests/storage/local-storage.test.ts`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add server/src/storage/ server/tests/storage/
git commit -m "feat(server): file storage — local filesystem with interface for S3 migration"
```

---

### Task 7: Repository Interfaces & Factory

**Files:**
- Create: `server/src/repositories/interfaces.ts`
- Create: `server/src/repositories/factory.ts`

- [ ] **Step 1: Create repository interfaces**

```typescript
// src/repositories/interfaces.ts
import {
  User, CreateUserInput,
  RateLimit, CreateRateLimitInput, ResolvedRateLimit,
  BannedIdentifier, BanIdentifierType, SharingBan,
  Notification, NotificationType,
  PaginatedResult,
} from '../models';

export interface UserRepository {
  create(input: CreateUserInput): Promise<User>;
  getById(id: string): Promise<User | null>;
  getByGoogleId(googleId: string): Promise<User | null>;
  getByEmail(email: string): Promise<User | null>;
  updateLastSeen(id: string): Promise<void>;
  setAdmin(id: string, isAdmin: boolean): Promise<void>;
  setBanned(id: string, isBanned: boolean, reason?: string, bannedBy?: string): Promise<void>;
  search(query: string, cursor?: string, limit?: number): Promise<PaginatedResult<User>>;
  count(): Promise<number>;
}

export interface RateLimitRepository {
  create(input: CreateRateLimitInput): Promise<RateLimit>;
  getById(id: string): Promise<RateLimit | null>;
  update(id: string, fields: Partial<CreateRateLimitInput>): Promise<RateLimit>;
  delete(id: string): Promise<boolean>;
  getAll(): Promise<RateLimit[]>;
  getByScope(scope: string, scopeId?: string): Promise<RateLimit[]>;
  resolve(userId: string, endpoint: string, userGroupIds: string[]): Promise<ResolvedRateLimit | null>;
}

export interface BanRepository {
  addBannedIdentifier(type: BanIdentifierType, identifier: string, reason?: string, bannedBy?: string): Promise<BannedIdentifier>;
  removeBannedIdentifier(id: string): Promise<boolean>;
  isBanned(type: BanIdentifierType, identifier: string): Promise<boolean>;
  listBannedIdentifiers(): Promise<BannedIdentifier[]>;
  addSharingBan(userId: string, reason: string, bannedBy: string, expiresAt?: Date): Promise<SharingBan>;
  removeSharingBan(id: string): Promise<boolean>;
  hasActiveSharingBan(userId: string): Promise<boolean>;
  listSharingBans(): Promise<SharingBan[]>;
}

export interface NotificationRepository {
  create(userId: string, type: NotificationType, payload: object): Promise<Notification>;
  getUnread(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Notification>>;
  markRead(id: string): Promise<void>;
  markAllRead(userId: string): Promise<void>;
  count(userId: string): Promise<number>;
}

// Repositories for Phase 2+ (interfaces defined now, implementations later)
export interface LocationRepository {
  // Defined in spec — implemented in Phase 2
}

export interface PhotoRepository {
  // Implemented in Phase 2
}

export interface FriendRepository {
  // Implemented in Phase 5
}

export interface GroupRepository {
  // Implemented in Phase 5
}

export interface ShareRepository {
  // Implemented in Phase 6
}

export interface SuggestionRepository {
  // Implemented in Phase 6
}

export interface DeepLinkRepository {
  // Implemented in Phase 6
}
```

- [ ] **Step 2: Create repository factory**

```typescript
// src/repositories/factory.ts
import { Knex } from 'knex';
import { config } from '../config';
import { UserRepository, RateLimitRepository, BanRepository, NotificationRepository } from './interfaces';

export interface Repositories {
  users: UserRepository;
  rateLimits: RateLimitRepository;
  bans: BanRepository;
  notifications: NotificationRepository;
}

export function createRepositories(knex: Knex): Repositories {
  switch (config.dbBackend) {
    case 'postgres': {
      const { PostgresUserRepo } = require('./postgres/user-repo');
      const { PostgresRateLimitRepo } = require('./postgres/rate-limit-repo');
      const { PostgresBanRepo } = require('./postgres/ban-repo');
      const { PostgresNotificationRepo } = require('./postgres/notification-repo');
      return {
        users: new PostgresUserRepo(knex),
        rateLimits: new PostgresRateLimitRepo(knex),
        bans: new PostgresBanRepo(knex),
        notifications: new PostgresNotificationRepo(knex),
      };
    }
    case 'mysql': {
      const { MysqlUserRepo } = require('./mysql/user-repo');
      const { MysqlRateLimitRepo } = require('./mysql/rate-limit-repo');
      const { MysqlBanRepo } = require('./mysql/ban-repo');
      const { MysqlNotificationRepo } = require('./mysql/notification-repo');
      return {
        users: new MysqlUserRepo(knex),
        rateLimits: new MysqlRateLimitRepo(knex),
        bans: new MysqlBanRepo(knex),
        notifications: new MysqlNotificationRepo(knex),
      };
    }
    default:
      throw new Error(`Unsupported DB_BACKEND: ${config.dbBackend}`);
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add server/src/repositories/
git commit -m "feat(server): repository interfaces and factory — abstracts DB backend selection"
```

---

### Task 8: PostgreSQL Repository Implementations

**Files:**
- Create: `server/src/repositories/postgres/connection.ts`
- Create: `server/src/repositories/postgres/user-repo.ts`
- Create: `server/src/repositories/postgres/rate-limit-repo.ts`
- Create: `server/src/repositories/postgres/ban-repo.ts`
- Create: `server/src/repositories/postgres/notification-repo.ts`
- Create: `server/tests/repositories/user-repo.test.ts`
- Create: `server/tests/repositories/rate-limit-repo.test.ts`
- Create: `server/tests/repositories/ban-repo.test.ts`
- Create: `server/tests/repositories/notification-repo.test.ts`
- Create: `server/tests/setup.ts`

The implementer should:
1. Create `tests/setup.ts` that initialises an in-memory SQLite database via Knex (using the `test` config from knexfile), runs all migrations, and exports the knex instance
2. Write tests against the PostgreSQL repo classes using SQLite (the SQL is compatible for basic CRUD via Knex's query builder)
3. Implement all 4 PostgreSQL repo classes with full Knex query builder usage (NOT raw SQL — Knex abstracts dialect differences)
4. Each repo receives a `Knex` instance in its constructor
5. Use `uuid` package for generating IDs
6. Map snake_case DB columns to camelCase model properties in row-mapping helper functions

Key implementation details:
- `UserRepository.search` should use `LIKE` on email and display_name
- `RateLimitRepository.resolve` implements the tiered resolution: check USER scope for userId, then GROUP scope for any userGroupIds, then GLOBAL scope. Return the first match (user > group > global). For GROUP, return the most restrictive (lowest maxRequests).
- `BanRepository.hasActiveSharingBan` should check `expires_at IS NULL OR expires_at > NOW()`
- `NotificationRepository.getUnread` should return notifications where `is_read = false`, ordered by `created_at DESC`
- All paginated methods use cursor-based pagination: cursor is the `id` of the last item, query uses `WHERE id < cursor ORDER BY id DESC`

Tests should cover: create, read, update, delete, search, pagination, edge cases (not found, duplicate, etc.)

- [ ] **Step 1: Create test setup**

```typescript
// tests/setup.ts
import knex, { Knex } from 'knex';
import knexConfig from '../knexfile';

let db: Knex;

export async function setupTestDb(): Promise<Knex> {
  // Force test config (SQLite in-memory)
  process.env.NODE_ENV = 'test';
  db = knex({
    client: 'better-sqlite3',
    connection: { filename: ':memory:' },
    useNullAsDefault: true,
    migrations: {
      directory: './migrations',
      extension: 'ts',
    },
  });
  await db.migrate.latest();
  return db;
}

export async function teardownTestDb(): Promise<void> {
  if (db) await db.destroy();
}

export function getTestDb(): Knex {
  return db;
}
```

- [ ] **Step 2: Implement all 4 PostgreSQL repos and their tests**

The implementer writes the full implementations and tests. This is the largest task — expect ~400 lines of repo code and ~300 lines of test code.

- [ ] **Step 3: Run all repo tests**

Run: `cd server && npx jest tests/repositories/ --forceExit`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add server/src/repositories/postgres/ server/tests/
git commit -m "feat(server): PostgreSQL repository implementations with tests"
```

---

### Task 9: MySQL Repository Implementations

**Files:**
- Create: `server/src/repositories/mysql/connection.ts`
- Create: `server/src/repositories/mysql/user-repo.ts`
- Create: `server/src/repositories/mysql/rate-limit-repo.ts`
- Create: `server/src/repositories/mysql/ban-repo.ts`
- Create: `server/src/repositories/mysql/notification-repo.ts`

The MySQL repos use the same Knex query builder as PostgreSQL. Since Knex abstracts dialect differences, the implementations are nearly identical — the main difference is the connection setup.

The implementer should:
1. Copy the PostgreSQL repos as a starting point
2. Adjust any PostgreSQL-specific syntax (there should be minimal differences since we use Knex query builder, not raw SQL)
3. Tests already pass against SQLite, which validates the Knex query builder logic — no separate MySQL tests needed for Phase 1

- [ ] **Step 1: Create MySQL connection**

```typescript
// src/repositories/mysql/connection.ts
import knex, { Knex } from 'knex';

export function createMysqlConnection(connectionString: string): Knex {
  return knex({
    client: 'mysql2',
    connection: connectionString,
  });
}
```

- [ ] **Step 2: Create all 4 MySQL repos (same logic as PostgreSQL)**

- [ ] **Step 3: Verify compilation**

Run: `cd server && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add server/src/repositories/mysql/
git commit -m "feat(server): MySQL repository implementations"
```

---

### Task 10: Error Handling & Request Logger Middleware

**Files:**
- Create: `server/src/middleware/error-handler.ts`
- Create: `server/src/middleware/request-logger.ts`
- Create: `server/tests/middleware/error-handler.test.ts`

- [ ] **Step 1: Write error handler test**

```typescript
// tests/middleware/error-handler.test.ts
import express from 'express';
import request from 'supertest';
import { errorHandler, AppError } from '../../src/middleware/error-handler';

function createTestApp() {
  const app = express();
  app.get('/error', () => { throw new Error('unexpected'); });
  app.get('/app-error', () => { throw new AppError(422, 'validation failed'); });
  app.get('/not-found', () => { throw new AppError(404, 'resource not found'); });
  app.use(errorHandler);
  return app;
}

describe('errorHandler', () => {
  const app = createTestApp();

  it('returns 500 for unexpected errors', async () => {
    const res = await request(app).get('/error');
    expect(res.status).toBe(500);
    expect(res.body.error).toBe('Internal server error');
  });

  it('returns status code for AppError', async () => {
    const res = await request(app).get('/app-error');
    expect(res.status).toBe(422);
    expect(res.body.error).toBe('validation failed');
  });

  it('returns 404 for not found AppError', async () => {
    const res = await request(app).get('/not-found');
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('resource not found');
  });
});
```

- [ ] **Step 2: Implement error handler**

```typescript
// src/middleware/error-handler.ts
import { Request, Response, NextFunction } from 'express';

export class AppError extends Error {
  constructor(public statusCode: number, message: string) {
    super(message);
    this.name = 'AppError';
  }
}

export function errorHandler(err: Error, req: Request, res: Response, _next: NextFunction): void {
  if (err instanceof AppError) {
    res.status(err.statusCode).json({ error: err.message });
    return;
  }

  console.error('Unhandled error:', err);
  res.status(500).json({ error: 'Internal server error' });
}
```

- [ ] **Step 3: Create request logger**

```typescript
// src/middleware/request-logger.ts
import morgan from 'morgan';
import { config } from '../config';

export const requestLogger = config.logLevel === 'debug'
  ? morgan('dev')
  : morgan('combined');
```

- [ ] **Step 4: Run tests**

Run: `cd server && npx jest tests/middleware/error-handler.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/src/middleware/error-handler.ts server/src/middleware/request-logger.ts server/tests/middleware/
git commit -m "feat(server): error handling and request logging middleware"
```

---

### Task 11: Rate Limiting Middleware

**Files:**
- Create: `server/src/middleware/rate-limiter.ts`
- Create: `server/tests/middleware/rate-limiter.test.ts`

- [ ] **Step 1: Write rate limiter test**

```typescript
// tests/middleware/rate-limiter.test.ts
import express from 'express';
import request from 'supertest';
import { createRateLimiter } from '../../src/middleware/rate-limiter';
import { MemoryCache } from '../../src/cache/memory-cache';
import { RateLimitRepository } from '../../src/repositories/interfaces';
import { ResolvedRateLimit } from '../../src/models';

// Mock rate limit repo that returns a fixed limit
function createMockRateLimitRepo(limit: ResolvedRateLimit | null): RateLimitRepository {
  return {
    resolve: jest.fn().mockResolvedValue(limit),
    create: jest.fn(),
    getById: jest.fn(),
    update: jest.fn(),
    delete: jest.fn(),
    getAll: jest.fn(),
    getByScope: jest.fn(),
  } as any;
}

function createTestApp(repo: RateLimitRepository) {
  const app = express();
  const cache = new MemoryCache();
  // Simulate auth middleware setting req.userId
  app.use((req: any, _res, next) => {
    req.userId = 'test-user';
    req.userGroupIds = [];
    next();
  });
  app.use(createRateLimiter(cache, repo));
  app.get('/test', (_req, res) => res.json({ ok: true }));
  return app;
}

describe('rateLimiter', () => {
  it('allows requests when no rate limit configured', async () => {
    const repo = createMockRateLimitRepo(null);
    const app = createTestApp(repo);
    const res = await request(app).get('/test');
    expect(res.status).toBe(200);
  });

  it('allows requests within limit', async () => {
    const repo = createMockRateLimitRepo({
      maxRequests: 5,
      windowSeconds: 60,
      source: 'GLOBAL',
      sourceId: null,
    });
    const app = createTestApp(repo);

    for (let i = 0; i < 5; i++) {
      const res = await request(app).get('/test');
      expect(res.status).toBe(200);
    }
  });

  it('blocks requests exceeding limit', async () => {
    const repo = createMockRateLimitRepo({
      maxRequests: 2,
      windowSeconds: 60,
      source: 'GLOBAL',
      sourceId: null,
    });
    const app = createTestApp(repo);

    await request(app).get('/test');
    await request(app).get('/test');
    const res = await request(app).get('/test');
    expect(res.status).toBe(429);
    expect(res.body.error).toContain('Rate limit exceeded');
  });

  it('includes rate limit headers', async () => {
    const repo = createMockRateLimitRepo({
      maxRequests: 10,
      windowSeconds: 60,
      source: 'GLOBAL',
      sourceId: null,
    });
    const app = createTestApp(repo);

    const res = await request(app).get('/test');
    expect(res.headers['x-ratelimit-limit']).toBe('10');
    expect(res.headers['x-ratelimit-remaining']).toBeDefined();
  });
});
```

- [ ] **Step 2: Implement rate limiter**

```typescript
// src/middleware/rate-limiter.ts
import { Request, Response, NextFunction } from 'express';
import { Cache } from '../cache/interfaces';
import { RateLimitRepository } from '../repositories/interfaces';

export function createRateLimiter(cache: Cache, rateLimitRepo: RateLimitRepository) {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const userId = (req as any).userId;
      if (!userId) { next(); return; }

      const userGroupIds: string[] = (req as any).userGroupIds || [];
      const endpoint = `${req.method} ${req.path}`;

      const limit = await rateLimitRepo.resolve(userId, endpoint, userGroupIds);
      if (!limit) { next(); return; }

      const cacheKey = `ratelimit:${userId}:${endpoint}`;
      const current = await cache.increment(cacheKey, limit.windowSeconds);

      res.setHeader('X-RateLimit-Limit', limit.maxRequests.toString());
      res.setHeader('X-RateLimit-Remaining', Math.max(0, limit.maxRequests - current).toString());

      if (current > limit.maxRequests) {
        res.status(429).json({
          error: 'Rate limit exceeded. Try again later.',
          retryAfter: limit.windowSeconds,
        });
        return;
      }

      next();
    } catch (err) {
      // Rate limiting should not block requests on error — fail open
      console.error('Rate limiter error:', err);
      next();
    }
  };
}
```

- [ ] **Step 3: Run tests**

Run: `cd server && npx jest tests/middleware/rate-limiter.test.ts`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add server/src/middleware/rate-limiter.ts server/tests/middleware/rate-limiter.test.ts
git commit -m "feat(server): tiered rate limiting middleware with cache-backed counters"
```

---

### Task 12: Health Route

**Files:**
- Create: `server/src/routes/health.ts`
- Create: `server/tests/routes/health.test.ts`

- [ ] **Step 1: Write health test**

```typescript
// tests/routes/health.test.ts
import express from 'express';
import request from 'supertest';
import { healthRouter } from '../../src/routes/health';

describe('GET /health', () => {
  const app = express();
  app.use(healthRouter);

  it('returns 200 with status ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
    expect(res.body.timestamp).toBeDefined();
  });
});
```

- [ ] **Step 2: Implement health route**

```typescript
// src/routes/health.ts
import { Router } from 'express';

export const healthRouter = Router();

healthRouter.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});
```

- [ ] **Step 3: Run test**

Run: `cd server && npx jest tests/routes/health.test.ts`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add server/src/routes/health.ts server/tests/routes/
git commit -m "feat(server): health check endpoint"
```

---

### Task 13: Express App Factory & Server Entrypoint

**Files:**
- Create: `server/src/app.ts`
- Create: `server/src/server.ts`
- Create: `server/tests/integration/app.test.ts`

- [ ] **Step 1: Create app factory**

```typescript
// src/app.ts
import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import { requestLogger } from './middleware/request-logger';
import { errorHandler } from './middleware/error-handler';
import { healthRouter } from './routes/health';

export function createApp(): express.Express {
  const app = express();

  // Security and parsing
  app.use(helmet());
  app.use(cors());
  app.use(express.json());

  // Logging
  app.use(requestLogger);

  // Routes
  app.use(healthRouter);

  // Error handling (must be last)
  app.use(errorHandler);

  return app;
}
```

- [ ] **Step 2: Create server entrypoint**

```typescript
// src/server.ts
import { createApp } from './app';
import { config } from './config';

const app = createApp();

app.listen(config.port, () => {
  console.log(`PEVPlugShare server listening on port ${config.port}`);
  console.log(`Database backend: ${config.dbBackend}`);
  console.log(`Log level: ${config.logLevel}`);
});
```

- [ ] **Step 3: Write integration test**

```typescript
// tests/integration/app.test.ts
import request from 'supertest';
import { createApp } from '../../src/app';

describe('App Integration', () => {
  const app = createApp();

  it('responds to health check', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });

  it('returns 404 for unknown routes', async () => {
    const res = await request(app).get('/nonexistent');
    expect(res.status).toBe(404);
  });

  it('sets security headers (helmet)', async () => {
    const res = await request(app).get('/health');
    expect(res.headers['x-content-type-options']).toBe('nosniff');
  });

  it('supports CORS', async () => {
    const res = await request(app)
      .options('/health')
      .set('Origin', 'http://example.com')
      .set('Access-Control-Request-Method', 'GET');
    expect(res.status).toBe(204);
  });

  it('parses JSON body', async () => {
    const res = await request(app)
      .post('/nonexistent')
      .send({ test: true })
      .set('Content-Type', 'application/json');
    // Should not crash on JSON body (404 is fine)
    expect(res.status).toBe(404);
  });
});
```

- [ ] **Step 4: Run all tests**

Run: `cd server && npx jest --forceExit`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add server/src/app.ts server/src/server.ts server/tests/integration/
git commit -m "feat(server): Express app factory and server entrypoint"
```

---

### Task 14: Dockerfile

**Files:**
- Create: `server/Dockerfile`

- [ ] **Step 1: Create Dockerfile**

```dockerfile
FROM node:20-slim

WORKDIR /app

COPY package*.json ./
RUN npm ci --production

COPY dist/ ./dist/
COPY migrations/ ./migrations/

EXPOSE 8080

CMD ["node", "dist/server.js"]
```

- [ ] **Step 2: Verify build compiles for Docker**

Run: `cd server && npm run build`
Expected: `dist/` directory created with compiled JS

- [ ] **Step 3: Commit**

```bash
git add server/Dockerfile
git commit -m "feat(server): Dockerfile for production deployment"
```

---

### Task 15: Final Integration — Run All Tests & Verify

- [ ] **Step 1: Run full test suite**

Run: `cd server && npm test`
Expected: All tests pass, no warnings

- [ ] **Step 2: Run TypeScript type check**

Run: `cd server && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Run build**

Run: `cd server && npm run build`
Expected: `dist/` directory with compiled JS, no errors

- [ ] **Step 4: Start server in dev mode (smoke test)**

Run: `cd server && npm run dev`
Expected: Server starts, logs "PEVPlugShare server listening on port 8080"
Stop with Ctrl+C

- [ ] **Step 5: Final commit**

```bash
git add -A server/
git commit -m "feat(server): Phase 1 complete — foundation, repos, cache, storage, rate limiting, Docker"
```
