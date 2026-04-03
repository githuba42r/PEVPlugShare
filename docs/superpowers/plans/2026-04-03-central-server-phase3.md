# Central Location Server — Phase 3 Implementation Plan (Admin Console)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a server-rendered admin console with Google OAuth login, dashboard, user/ban/location/group/rate-limit management, and admin-only API endpoints.

**Architecture:** The admin console has two layers: (1) JSON API under `/admin/api/*` requiring `is_admin = true` — used by the admin UI and testable independently, and (2) server-rendered HTML views using EJS templates at `/admin/*` with Google OAuth web login flow using `express-session`. Admin auth uses the same Google OAuth verification as the app API but with a server-side session for the browser.

**Tech Stack:** EJS (templates), express-session (browser sessions), existing google-auth-library, existing repositories, Tailwind CSS via CDN (minimal styling without build tooling)

**Spec:** `docs/features/CENTRAL_LOCATION_SERVER_SPEC.md` — Admin Console section

**Phase 1-2 foundation:** Config, models, repositories (users, locations, photos, bans, rate-limits, notifications), auth (JWT, Google OAuth), Express app with API routes.

---

## Scope

Phase 3 builds:

1. **Dependencies** — EJS, express-session
2. **Admin auth** — Google OAuth web login flow with session, admin middleware
3. **Admin API** — All `/admin/api/*` endpoints (JSON, tested with supertest)
4. **Admin views** — EJS templates for dashboard, users, bans, sharing-bans, locations, groups, rate-limits
5. **App wiring** — Mount admin routes in Express app

The admin console uses Tailwind CSS via CDN for styling — no build step needed. Views are functional and clean, not pixel-perfect.

## File Structure

### New Files

```
server/src/
  auth/
    admin-middleware.ts            # Checks is_admin on session or JWT
    admin-session.ts               # Express-session config + Google OAuth web flow

  admin/
    api/
      users.ts                     # Admin user management API
      bans.ts                      # Banned identifiers API
      sharing-bans.ts              # Sharing ban API
      locations.ts                 # Admin location management API
      groups.ts                    # Admin group management API
      rate-limits.ts               # Rate limit CRUD API
      stats.ts                     # Dashboard statistics API
    views.ts                       # Server-rendered view routes

  views/
    layout.ejs                     # Base HTML template (head, nav, Tailwind CDN)
    login.ejs                      # Google OAuth login page
    dashboard.ejs                  # Stats + system health
    users/
      list.ejs                     # Paginated user list with search
      detail.ejs                   # User detail with actions
    bans/
      list.ejs                     # Banned identifiers list
    sharing-bans/
      list.ejs                     # Sharing bans list
    locations/
      list.ejs                     # Paginated location list
      detail.ejs                   # Location detail with photos
    groups/
      list.ejs                     # Group list
      detail.ejs                   # Group detail with members
    rate-limits/
      list.ejs                     # Rate limit rules list
      resolve.ejs                  # Effective limits for a user

server/tests/
  admin/
    api/
      users.test.ts
      bans.test.ts
      locations.test.ts
      rate-limits.test.ts
      stats.test.ts
    middleware.test.ts
```

### Modified Files

```
server/package.json                    # Add EJS, express-session, @types
server/src/app.ts                      # Mount admin routes
server/src/config.ts                   # Add SESSION_SECRET env var
```

---

### Task 1: Dependencies & Config

**Files:**
- Modify: `server/package.json`
- Modify: `server/src/config.ts`

- [ ] **Step 1: Install dependencies**

```bash
cd /home/philg/src/AndroidStudioProjects/PEVPlugShare/server
npm install ejs express-session
npm install -D @types/express-session
```

- [ ] **Step 2: Add sessionSecret to config**

In `src/config.ts`, add to the Config interface:
```typescript
sessionSecret: string;
```

Add to the config object:
```typescript
sessionSecret: process.env['SESSION_SECRET'] || process.env['JWT_SECRET'] || 'dev-session-secret',
```

Add to `.env.example`:
```
SESSION_SECRET=your-session-secret
```

- [ ] **Step 3: Verify**

Run: `npx tsc --noEmit`

- [ ] **Step 4: Commit**

```bash
git add package.json package-lock.json src/config.ts .env.example
git commit -m "chore(server): add EJS, express-session dependencies and session config"
```

---

### Task 2: Admin Middleware

**Files:**
- Create: `server/src/auth/admin-middleware.ts`
- Create: `server/tests/admin/middleware.test.ts`

- [ ] **Step 1: Create admin middleware**

Two middlewares:

```typescript
// src/auth/admin-middleware.ts
import { Request, Response, NextFunction } from 'express';

/**
 * Middleware for admin API routes — checks JWT or session for admin status.
 * Used on /admin/api/* routes.
 */
export function requireAdmin(req: Request, res: Response, next: NextFunction): void {
  // Check session first (browser login), then JWT claims (API)
  const isAdmin = (req.session as any)?.isAdmin || req.isAdmin;
  if (!isAdmin) {
    res.status(403).json({ error: 'Admin access required' });
    return;
  }
  next();
}

/**
 * Middleware for admin view routes — redirects to login if no session.
 */
export function requireAdminSession(req: Request, res: Response, next: NextFunction): void {
  const session = req.session as any;
  if (!session?.userId || !session?.isAdmin) {
    res.redirect('/admin/login');
    return;
  }
  next();
}
```

- [ ] **Step 2: Write tests**

Test `requireAdmin`:
- Returns 403 when not admin
- Allows request when session.isAdmin is true
- Allows request when req.isAdmin is true (JWT)

Test `requireAdminSession`:
- Redirects to /admin/login when no session
- Allows request when session has userId and isAdmin

- [ ] **Step 3: Run tests, commit**

```bash
git add src/auth/admin-middleware.ts tests/admin/
git commit -m "feat(server): admin middleware — requireAdmin and requireAdminSession"
```

---

### Task 3: Admin OAuth Login Flow

**Files:**
- Create: `server/src/auth/admin-session.ts`

- [ ] **Step 1: Create admin session/login routes**

```typescript
// src/auth/admin-session.ts
import { Router, Request, Response } from 'express';
import session from 'express-session';
import { verifyGoogleToken } from './google-oauth';
import { config } from '../config';
import { Repositories } from '../repositories/factory';

export function createAdminSessionRouter(repos: Repositories): Router {
  const router = Router();

  // Session middleware (applied to /admin/* only)
  router.use(session({
    secret: config.sessionSecret,
    resave: false,
    saveUninitialized: false,
    cookie: { secure: false, maxAge: 24 * 60 * 60 * 1000 }, // 24h
  }));

  // GET /admin/login — render login page
  router.get('/admin/login', (_req: Request, res: Response) => {
    res.render('login', { googleClientId: config.googleClientId });
  });

  // POST /admin/login — exchange Google token for session
  router.post('/admin/login', async (req: Request, res: Response) => {
    const { credential } = req.body;
    if (!credential) {
      res.status(400).render('login', { googleClientId: config.googleClientId, error: 'No credential provided' });
      return;
    }

    const googleUser = await verifyGoogleToken(credential, config.googleClientId);
    if (!googleUser) {
      res.status(401).render('login', { googleClientId: config.googleClientId, error: 'Invalid Google token' });
      return;
    }

    const user = await repos.users.getByGoogleId(googleUser.googleId);
    if (!user || !user.isAdmin) {
      res.status(403).render('login', { googleClientId: config.googleClientId, error: 'Admin access required' });
      return;
    }

    // Set session
    const sess = req.session as any;
    sess.userId = user.id;
    sess.email = user.email;
    sess.displayName = user.displayName;
    sess.isAdmin = true;

    res.redirect('/admin');
  });

  // GET /admin/logout
  router.get('/admin/logout', (req: Request, res: Response) => {
    req.session.destroy(() => {
      res.redirect('/admin/login');
    });
  });

  return router;
}
```

- [ ] **Step 2: Verify compilation, commit**

```bash
git add src/auth/admin-session.ts
git commit -m "feat(server): admin Google OAuth login flow with session"
```

---

### Task 4: Admin API — Users & Bans

**Files:**
- Create: `server/src/admin/api/users.ts`
- Create: `server/src/admin/api/bans.ts`
- Create: `server/src/admin/api/sharing-bans.ts`
- Create: `server/tests/admin/api/users.test.ts`
- Create: `server/tests/admin/api/bans.test.ts`

- [ ] **Step 1: Create admin user API**

Export `createAdminUserRouter(repos: Repositories): Router`

Endpoints (all under `/admin/api`):
- `GET /admin/api/users` — paginated list with optional `?q=` search. Uses `repos.users.search()`.
- `GET /admin/api/users/:id` — user detail with location count
- `PUT /admin/api/users/:id/admin` — body: `{ isAdmin: boolean }`. Calls `repos.users.setAdmin()`.
- `PUT /admin/api/users/:id/ban` — body: `{ reason: string }`. Calls `repos.users.setBanned(id, true, reason, adminUserId)`. Also adds email + googleId to banned_identifiers.
- `PUT /admin/api/users/:id/unban` — Calls `repos.users.setBanned(id, false)`.

The admin's userId comes from `(req.session as any)?.userId` or `req.userId` (JWT).

- [ ] **Step 2: Create ban API**

Export `createAdminBanRouter(repos: Repositories): Router`

- `GET /admin/api/bans` — `repos.bans.listBannedIdentifiers()`
- `POST /admin/api/bans` — body: `{ type, identifier, reason }`. Calls `repos.bans.addBannedIdentifier()`.
- `DELETE /admin/api/bans/:id` — `repos.bans.removeBannedIdentifier(id)`

- [ ] **Step 3: Create sharing ban API**

Export `createAdminSharingBanRouter(repos: Repositories): Router`

- `GET /admin/api/sharing-bans` — `repos.bans.listSharingBans()`
- `POST /admin/api/users/:id/sharing-ban` — body: `{ reason, expiresAt? }`. Calls `repos.bans.addSharingBan()`.
- `DELETE /admin/api/users/:id/sharing-ban` — find active sharing ban for user, call `repos.bans.removeSharingBan()`.

- [ ] **Step 4: Write tests**

Create test app with admin middleware applied, sign JWT with `isAdmin: true`. Test each endpoint:
- List users (paginated)
- Get user detail
- Grant admin → verify isAdmin changed
- Ban user → verify isBanned + banned_identifiers created
- Unban user → verify cleared
- List bans
- Create ban
- Delete ban
- Create sharing ban
- Remove sharing ban
- Non-admin gets 403

- [ ] **Step 5: Commit**

```bash
git add src/admin/ tests/admin/
git commit -m "feat(server): admin API — user management, bans, and sharing bans"
```

---

### Task 5: Admin API — Locations, Groups, Rate Limits, Stats

**Files:**
- Create: `server/src/admin/api/locations.ts`
- Create: `server/src/admin/api/groups.ts`
- Create: `server/src/admin/api/rate-limits.ts`
- Create: `server/src/admin/api/stats.ts`
- Create: `server/tests/admin/api/locations.test.ts`
- Create: `server/tests/admin/api/rate-limits.test.ts`
- Create: `server/tests/admin/api/stats.test.ts`

- [ ] **Step 1: Create admin location API**

Export `createAdminLocationRouter(repos: Repositories): Router`

- `GET /admin/api/locations` — paginated, optional `?q=` search by name. Use `repos.locations.getByOwner()` is wrong here — need a new method. For now, use a direct knex query or add a `listAll()` method to LocationRepository.
- `GET /admin/api/locations/:id` — detail with photos: `repos.locations.getById()` + `repos.photos.getByLocationId()`
- `DELETE /admin/api/locations/:id` — soft-delete: `repos.locations.softDelete(id, reason)`. Body: `{ reason }`.
- `PUT /admin/api/locations/:id/block-sharing` — add a `blocked_sharing` flag. For now, just soft-delete with reason "Sharing blocked by admin" (simplification for v1 — proper sharing-block can be added when sharing is implemented in Phase 6).

The implementer should add a `listAll(cursor?, limit?): Promise<PaginatedResult<Location>>` method to the LocationRepository interface and implementations.

- [ ] **Step 2: Create admin group API**

Export `createAdminGroupRouter(repos: Repositories): Router`

Groups don't have a full repository yet (Phase 5). Create stub endpoints:
- `GET /admin/api/groups` — return empty list `{ data: [], pagination: { cursor: null, hasMore: false } }`
- `GET /admin/api/groups/:id` — return 404 "Groups not yet implemented"
- `DELETE /admin/api/groups/:id` — return 404 "Groups not yet implemented"

These stubs will be replaced in Phase 5 when group repos are built.

- [ ] **Step 3: Create admin rate limit API**

Export `createAdminRateLimitRouter(repos: Repositories): Router`

- `GET /admin/api/rate-limits` — `repos.rateLimits.getAll()`
- `POST /admin/api/rate-limits` — body: `{ scope, scopeId?, endpoint, maxRequests, windowSeconds }`. Validate required fields. `repos.rateLimits.create()`.
- `PUT /admin/api/rate-limits/:id` — `repos.rateLimits.update(id, fields)`
- `DELETE /admin/api/rate-limits/:id` — `repos.rateLimits.delete(id)`
- `GET /admin/api/rate-limits/resolve/:userId` — Get user, find their group IDs (empty for now), call `repos.rateLimits.resolve()`. Return the resolved limit or a message that no limit applies.

- [ ] **Step 4: Create stats API**

Export `createAdminStatsRouter(repos: Repositories): Router`

- `GET /admin/api/stats` — returns counts:
  ```json
  {
    "users": <repos.users.count()>,
    "locations": <count from locations table>,
    "photos": <count from photos table>,
    "rateLimits": <number of rate limit rules>
  }
  ```

For location and photo counts, add `count()` methods to LocationRepository and PhotoRepository. The implementer should add `count(): Promise<number>` to both interfaces and implementations.

- [ ] **Step 5: Write tests**

Test admin location endpoints (list, detail, soft-delete).
Test rate limit CRUD.
Test stats endpoint returns counts.
All with admin JWT.

- [ ] **Step 6: Commit**

```bash
git add src/admin/ src/repositories/ tests/admin/
git commit -m "feat(server): admin API — locations, groups (stub), rate limits, and stats"
```

---

### Task 6: EJS Layout & Login View

**Files:**
- Create: `server/src/views/layout.ejs`
- Create: `server/src/views/login.ejs`

- [ ] **Step 1: Create layout template**

```ejs
<!-- src/views/layout.ejs -->
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><%= typeof title !== 'undefined' ? title + ' — ' : '' %>PEVPlugShare Admin</title>
  <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900 text-gray-100 min-h-screen">
  <% if (typeof showNav === 'undefined' || showNav) { %>
  <nav class="bg-gray-800 border-b border-gray-700 px-6 py-3 flex items-center justify-between">
    <div class="flex items-center gap-6">
      <a href="/admin" class="text-lg font-bold text-green-400">PEVPlugShare Admin</a>
      <a href="/admin" class="text-sm text-gray-300 hover:text-white">Dashboard</a>
      <a href="/admin/users" class="text-sm text-gray-300 hover:text-white">Users</a>
      <a href="/admin/bans" class="text-sm text-gray-300 hover:text-white">Bans</a>
      <a href="/admin/sharing-bans" class="text-sm text-gray-300 hover:text-white">Sharing Bans</a>
      <a href="/admin/locations" class="text-sm text-gray-300 hover:text-white">Locations</a>
      <a href="/admin/groups" class="text-sm text-gray-300 hover:text-white">Groups</a>
      <a href="/admin/rate-limits" class="text-sm text-gray-300 hover:text-white">Rate Limits</a>
    </div>
    <div class="flex items-center gap-4">
      <span class="text-sm text-gray-400"><%= typeof adminEmail !== 'undefined' ? adminEmail : '' %></span>
      <a href="/admin/logout" class="text-sm text-red-400 hover:text-red-300">Logout</a>
    </div>
  </nav>
  <% } %>
  <main class="max-w-7xl mx-auto px-6 py-8">
    <%- body %>
  </main>
</body>
</html>
```

- [ ] **Step 2: Create login template**

```ejs
<!-- src/views/login.ejs -->
<% layout('layout', { title: 'Login', showNav: false }) %>

<!-- Uses Google Identity Services for one-tap sign-in -->
<div class="flex items-center justify-center min-h-screen">
  <div class="bg-gray-800 rounded-lg p-8 max-w-md w-full">
    <h1 class="text-2xl font-bold text-center mb-6">PEVPlugShare Admin</h1>
    <% if (typeof error !== 'undefined' && error) { %>
    <div class="bg-red-900/50 border border-red-700 rounded p-3 mb-4 text-red-300 text-sm"><%= error %></div>
    <% } %>
    <p class="text-gray-400 text-center mb-6">Sign in with your Google account to access the admin console.</p>
    <div id="g_id_onload"
      data-client_id="<%= googleClientId %>"
      data-login_uri="/admin/login"
      data-auto_prompt="false">
    </div>
    <div class="g_id_signin" data-type="standard" data-size="large" data-theme="filled_blue" data-text="sign_in_with" data-shape="rectangular" data-width="360"></div>
    <script src="https://accounts.google.com/gsi/client" async defer></script>
  </div>
</div>
```

Note: EJS doesn't have built-in layout inheritance. The implementer should use `express-ejs-layouts` package OR structure views to `include` partials. The simplest approach: each view is a complete HTML file that includes a header/nav partial.

- [ ] **Step 3: Commit**

```bash
git add src/views/
git commit -m "feat(server): EJS layout and login templates with Tailwind CSS"
```

---

### Task 7: Admin View Routes & Remaining EJS Templates

**Files:**
- Create: `server/src/admin/views.ts`
- Create: remaining EJS templates (dashboard, users, bans, locations, groups, rate-limits)

- [ ] **Step 1: Create admin view routes**

Export `createAdminViewRouter(repos: Repositories): Router`

Each view route:
1. Fetches data from admin API endpoints (or directly from repos)
2. Renders the EJS template with the data

Routes:
- `GET /admin` → dashboard (renders stats from repos)
- `GET /admin/users` → user list (paginated)
- `GET /admin/users/:id` → user detail
- `GET /admin/bans` → banned identifiers list
- `GET /admin/sharing-bans` → sharing bans list
- `GET /admin/locations` → location list (paginated)
- `GET /admin/locations/:id` → location detail with photos
- `GET /admin/groups` → group list (stub)
- `GET /admin/rate-limits` → rate limit rules list
- `GET /admin/rate-limits/resolve/:userId` → effective limits for user

All view routes use `requireAdminSession` middleware.

- [ ] **Step 2: Create EJS templates**

Each template is a standalone HTML page that includes the nav partial. Templates use Tailwind classes for styling. Data is passed via `res.render('template', data)`.

Dashboard: display stats (users, locations, photos counts) in cards.
User list: table with email, display_name, is_admin, is_banned, link to detail.
User detail: all fields, action buttons (ban/unban/admin toggle).
Ban list: table of banned identifiers with type, identifier, reason, delete button.
Sharing ban list: table with user email, reason, expires_at, remove button.
Location list: table with name, owner, lat/lon, is_deleted.
Location detail: all fields, photos grid, delete button.
Group list: stub "Groups not yet implemented".
Rate limit list: table with scope, endpoint, max_requests, window_seconds, edit/delete buttons.
Rate limit resolve: input for userId, shows resolved limits.

All forms POST to the admin API endpoints via JavaScript `fetch()` calls, then redirect/refresh.

- [ ] **Step 3: Commit**

```bash
git add src/admin/views.ts src/views/
git commit -m "feat(server): admin console EJS views — dashboard, users, bans, locations, rate limits"
```

---

### Task 8: Wire Admin into App Factory

**Files:**
- Modify: `server/src/app.ts`

- [ ] **Step 1: Update app.ts**

Add to `createApp()` when deps are provided:

1. Set EJS as view engine: `app.set('view engine', 'ejs')` and `app.set('views', path.join(__dirname, 'views'))`
2. Add `express.urlencoded({ extended: true })` for form POST bodies
3. Mount admin session router (session + login/logout): `app.use(createAdminSessionRouter(deps.repos))`
4. Mount admin view routes with `requireAdminSession`: `app.use(createAdminViewRouter(deps.repos))`
5. Mount admin API routes with `requireAdmin`: under `/admin/api`, apply `requireAdmin` middleware, then mount all admin API routers

Order matters:
```
app.use(healthRouter)
app.use(createAuthRouter(repos))                    // POST /auth/google (no auth)
app.use(createAdminSessionRouter(repos))            // /admin/login, /admin/logout (sessions)
app.use('/admin', requireAdminSession, createAdminViewRouter(repos))  // /admin/* views
app.use('/admin/api', requireAdmin, createAdminUserRouter(repos))     // /admin/api/* JSON
app.use('/admin/api', requireAdmin, createAdminBanRouter(repos))
app.use('/admin/api', requireAdmin, createAdminSharingBanRouter(repos))
app.use('/admin/api', requireAdmin, createAdminLocationRouter(repos))
app.use('/admin/api', requireAdmin, createAdminGroupRouter(repos))
app.use('/admin/api', requireAdmin, createAdminRateLimitRouter(repos))
app.use('/admin/api', requireAdmin, createAdminStatsRouter(repos))
app.use('/api', createAuthMiddleware(config.jwtSecret))  // JWT auth for /api/*
app.use('/api', createRateLimiter(cache, repos.rateLimits))
app.use(createUserRouter(repos))                    // /api/users/*
app.use(createLocationRouter(repos))                // /api/locations/*
app.use(createPhotoRouter(repos, storage))          // /api/photos/*
app.use(createNotificationRouter(repos))            // /api/notifications/*
```

- [ ] **Step 2: Copy views to dist on build**

Update `package.json` build script to copy EJS files:
```json
"build": "tsc && cp -r src/views dist/views"
```

- [ ] **Step 3: Run all tests**

```bash
npm test
```

- [ ] **Step 4: Commit**

```bash
git add src/app.ts package.json
git commit -m "feat(server): wire admin console into Express app — views, API, session auth"
```

---

### Task 9: Final Verification

- [ ] **Step 1: Run full test suite**

```bash
npm test
```
Expected: All tests pass

- [ ] **Step 2: TypeScript check**

```bash
npx tsc --noEmit
```

- [ ] **Step 3: Build**

```bash
npm run build
```
Verify dist/ includes views/ directory with EJS files

- [ ] **Step 4: Commit**

```bash
git add -A server/
git commit -m "feat(server): Phase 3 complete — admin console with views, API, and Google OAuth login"
```
