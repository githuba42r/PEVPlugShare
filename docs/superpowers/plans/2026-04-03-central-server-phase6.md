# Central Location Server — Phase 6 Implementation Plan (Sharing & Collaborative Editing)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement location sharing (with friends, groups, and via direct links/QR), the "Shared With Me" view, collaborative editing via field-level change suggestions, duplicate detection/hiding, and the Android UI for all sharing and suggestion flows.

**Architecture:** Server: ShareRepository (location_shares + hidden_shares), SuggestionRepository (change_suggestions), DeepLinkRepository (direct_share_tokens), sharing and suggestion services with notification creation, route handlers for 13 new endpoints. The existing `getSharedWithUser` stub in LocationRepository gets a real implementation using location_shares. Android: sharing dialog, shared-with-me screen, suggestion composer (field-level diff), incoming suggestions review screen, direct share via deep links, offline suggestion queueing via PendingOperation.

**Tech Stack:** Server: existing Express/TypeScript/Knex. Android: existing Retrofit/Hilt/Room, PendingOperation for offline queueing.

**Spec:** `docs/features/CENTRAL_LOCATION_SERVER_SPEC.md` — Sharing, Change Suggestions, Direct Share Tokens, Hidden Shares, Duplicate Detection sections

**Prerequisite:** Phases 1-5 complete (server + Android).

---

## Scope

### Server-Side

1. **ShareRepository** — CRUD for location_shares, hidden_shares
2. **SuggestionRepository** — CRUD for change_suggestions
3. **DeepLinkRepository** — CRUD for direct_share_tokens
4. **Sharing service** — share/revoke/hide logic, sharing ban check, soft-delete cascade
5. **Suggestion service** — submit/accept/reject/withdraw, field merge on accept
6. **Sharing routes** — 7 endpoints
7. **Suggestion routes** — 6 endpoints
8. **Update LocationRepository.getSharedWithUser** — real implementation joining location_shares
9. **Update location soft-delete** — mark shares inactive, notify recipients
10. **Repository factory + app wiring**

### Android-Side

11. **API models + Retrofit endpoints** — sharing + suggestion
12. **Share location dialog** — pick friends/groups or generate direct link
13. **Shared With Me screen** — browse, pull to local, hide duplicates
14. **Suggestion composer** — edit shared location, submit field diffs
15. **Incoming suggestions screen** — review, accept/reject
16. **Direct share deep links** — `pevplugshare://location/{token}`
17. **PendingOperation extension** — offline suggestion queueing
18. **Settings/navigation updates**

---

## File Structure

### Server — New Files

```
server/src/
  repositories/
    postgres/
      share-repo.ts
      suggestion-repo.ts
      deep-link-repo.ts
    mysql/
      share-repo.ts
      suggestion-repo.ts
      deep-link-repo.ts

  services/
    sharing-service.ts
    suggestion-service.ts

  routes/
    sharing.ts
    suggestions.ts

server/tests/
  repositories/
    share-repo.test.ts
    suggestion-repo.test.ts
    deep-link-repo.test.ts
  routes/
    sharing.test.ts
    suggestions.test.ts
```

### Android — New Files

```
app/src/main/java/com/a42r/pevplugshare/
  ui/sharing/
    ShareLocationActivity.kt
    ShareLocationViewModel.kt
    SharedWithMeActivity.kt
    SharedWithMeViewModel.kt
  ui/suggestions/
    SuggestionComposerActivity.kt
    SuggestionComposerViewModel.kt
    IncomingSuggestionsActivity.kt
    IncomingSuggestionsViewModel.kt

app/src/main/res/layout/
  activity_share_location.xml
  activity_shared_with_me.xml
  item_shared_location.xml
  activity_suggestion_composer.xml
  activity_incoming_suggestions.xml
  item_suggestion.xml
```

---

### Task 1: Server — Share Repository

**Files:**
- Modify: `server/src/repositories/interfaces.ts` — replace empty ShareRepository
- Create: `server/src/repositories/postgres/share-repo.ts`
- Create: `server/src/repositories/mysql/share-repo.ts`
- Create: `server/tests/repositories/share-repo.test.ts`

- [ ] **Step 1: Define ShareRepository interface**

Read existing interfaces.ts first. Replace empty `ShareRepository`:

```typescript
export interface ShareRepository {
  // Location shares
  shareWithUser(locationId: string, sharedBy: string, sharedWithUser: string): Promise<LocationShare>;
  shareWithGroup(locationId: string, sharedBy: string, sharedWithGroup: string): Promise<LocationShare>;
  revokeShare(shareId: string): Promise<boolean>;
  getSharesForLocation(locationId: string, cursor?: string, limit?: number): Promise<PaginatedResult<LocationShare>>;
  getLocationsSharedWithUser(userId: string, userGroupIds: string[], cursor?: string, limit?: number): Promise<PaginatedResult<Location>>;
  deactivateSharesForLocation(locationId: string): Promise<void>;
  isSharedWithUser(locationId: string, userId: string, userGroupIds: string[]): Promise<boolean>;

  // Hidden shares
  hideShare(userId: string, locationId: string, shareId: string): Promise<HiddenShare>;
  unhideShare(userId: string, shareId: string): Promise<boolean>;
  getHiddenShareIds(userId: string): Promise<string[]>;
  unhideByLocationVersion(locationId: string): Promise<void>;
}
```

Add imports for `LocationShare`, `HiddenShare`, `Location`.

- [ ] **Step 2: Implement PostgreSQL share repo**

Key details:
- `shareWithUser()`: Insert into location_shares with uuid. Validate either user or group, not both.
- `getLocationsSharedWithUser()`: JOIN location_shares with server_locations WHERE (shared_with_user = userId OR shared_with_group IN userGroupIds) AND is_active = true AND location is_deleted = false. Exclude hidden shares (NOT IN hidden_shares for this user). Cursor pagination.
- `deactivateSharesForLocation()`: UPDATE is_active = false WHERE location_id.
- `hideShare()`: Insert into hidden_shares with uuid.
- `unhideByLocationVersion()`: DELETE from hidden_shares WHERE location_id (called when version increments).

- [ ] **Step 3: Copy to MySQL, write tests, commit**

Commit: "feat(server): share repository — location shares, hidden shares with tests"

---

### Task 2: Server — Suggestion Repository

**Files:**
- Modify: `server/src/repositories/interfaces.ts`
- Create: `server/src/repositories/postgres/suggestion-repo.ts`
- Create: `server/src/repositories/mysql/suggestion-repo.ts`
- Create: `server/tests/repositories/suggestion-repo.test.ts`

- [ ] **Step 1: Define SuggestionRepository interface**

```typescript
export interface SuggestionRepository {
  create(locationId: string, suggestedBy: string, changesJson: string, message?: string): Promise<ChangeSuggestion>;
  getById(id: string): Promise<ChangeSuggestion | null>;
  getIncoming(ownerId: string, cursor?: string, limit?: number): Promise<PaginatedResult<ChangeSuggestion>>;
  getOutgoing(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<ChangeSuggestion>>;
  accept(id: string, responseMsg?: string): Promise<ChangeSuggestion>;
  reject(id: string, responseMsg?: string): Promise<ChangeSuggestion>;
  withdraw(id: string): Promise<ChangeSuggestion>;
}
```

- [ ] **Step 2: Implement**

Key details:
- `getIncoming()`: JOIN change_suggestions with server_locations WHERE server_locations.owner_id = ownerId AND status = 'PENDING'. Cursor pagination.
- `getOutgoing()`: WHERE suggested_by = userId. Cursor pagination.
- `accept()`: UPDATE status = 'ACCEPTED', responded_at = now. Return updated.
- `reject()`: UPDATE status = 'REJECTED', responded_at = now, response_msg.
- `withdraw()`: UPDATE status = 'WITHDRAWN' WHERE suggested_by and status = 'PENDING'.

- [ ] **Step 3: Tests, commit**

Commit: "feat(server): suggestion repository — create, accept, reject, withdraw with tests"

---

### Task 3: Server — Deep Link Repository

**Files:**
- Modify: `server/src/repositories/interfaces.ts`
- Create: `server/src/repositories/postgres/deep-link-repo.ts`
- Create: `server/src/repositories/mysql/deep-link-repo.ts`
- Create: `server/tests/repositories/deep-link-repo.test.ts`

- [ ] **Step 1: Define DeepLinkRepository interface**

```typescript
export interface DeepLinkRepository {
  createToken(locationId: string, createdBy: string, token: string, isSingleUse?: boolean, maxUses?: number, expiresAt?: Date): Promise<DirectShareToken>;
  getByToken(token: string): Promise<DirectShareToken | null>;
  incrementUseCount(id: string): Promise<void>;
  delete(id: string): Promise<boolean>;
}
```

- [ ] **Step 2: Implement, test, commit**

Key: `getByToken` should check expiry and use count limits. `incrementUseCount` bumps use_count.

Commit: "feat(server): deep link repository — direct share token management with tests"

---

### Task 4: Server — Sharing & Suggestion Services

**Files:**
- Create: `server/src/services/sharing-service.ts`
- Create: `server/src/services/suggestion-service.ts`

- [ ] **Step 1: Create sharing service**

```typescript
export class SharingService {
  constructor(
    private shareRepo: ShareRepository,
    private banRepo: BanRepository,
    private deepLinkRepo: DeepLinkRepository,
    private locationRepo: LocationRepository,
    private notificationRepo: NotificationRepository,
    private groupRepo: GroupRepository,
    private serverBaseUrl: string
  ) {}

  async shareWithUser(locationId: string, userId: string, sharedBy: string): Promise<LocationShare> {
    // Check sharing ban: banRepo.hasActiveSharingBan(sharedBy) → throw if banned
    // Verify location exists and sharedBy is owner
    // Create share
    // Notify recipient (SHARE_RECEIVED)
    // Check for duplicates near this location for the recipient
  }

  async shareWithGroup(locationId: string, groupId: string, sharedBy: string): Promise<LocationShare> {
    // Same checks + share with group
    // Notify all group members
  }

  async revokeShare(shareId: string, userId: string): Promise<void> {
    // Verify userId is the sharer (location owner)
    // Revoke
  }

  async generateDirectLink(locationId: string, userId: string, options?: { singleUse?: boolean; maxUses?: number; expiresAt?: Date }): Promise<{ token: string; url: string }> {
    // Verify owner
    // Generate token (uuid)
    // Create via deepLinkRepo
    // Build URL
  }

  async resolveDirectLink(token: string): Promise<Location> {
    // Get token record → 404 if not found
    // Check expiry/use count
    // Increment use count
    // Return location
  }

  async onLocationSoftDelete(locationId: string, reason: string): Promise<void> {
    // Deactivate all shares
    // Get all shared-with users
    // Notify each (LOCATION_DELETED with reason)
  }
}
```

- [ ] **Step 2: Create suggestion service**

```typescript
export class SuggestionService {
  constructor(
    private suggestionRepo: SuggestionRepository,
    private locationRepo: LocationRepository,
    private shareRepo: ShareRepository,
    private notificationRepo: NotificationRepository
  ) {}

  async submit(locationId: string, userId: string, changesJson: string, message?: string): Promise<ChangeSuggestion> {
    // Verify location exists
    // Verify user has access (is shared with them)
    // Create suggestion
    // Notify location owner (SUGGESTION_RECEIVED)
  }

  async accept(suggestionId: string, userId: string, responseMsg?: string): Promise<void> {
    // Get suggestion → 404
    // Get location → verify userId is owner
    // Apply field changes: parse changesJson, call locationRepo.update(locationId, fields)
    // Version increments automatically in locationRepo.update
    // Accept suggestion in repo
    // Unhide hidden shares for this location (version changed)
    // Notify suggester (SUGGESTION_ACCEPTED)
  }

  async reject(suggestionId: string, userId: string, responseMsg?: string): Promise<void> {
    // Verify owner
    // Reject in repo
    // Notify suggester (SUGGESTION_REJECTED)
  }

  async withdraw(suggestionId: string, userId: string): Promise<void> {
    // Verify suggester
    // Withdraw in repo
  }
}
```

- [ ] **Step 3: Commit**

Commit: "feat(server): sharing and suggestion services with ban check and notifications"

---

### Task 5: Server — Sharing Routes

**Files:**
- Create: `server/src/routes/sharing.ts`
- Create: `server/tests/routes/sharing.test.ts`

- [ ] **Step 1: Implement 7 sharing endpoints**

Export `createSharingRouter(sharingService: SharingService, shareRepo: ShareRepository): Router`

```
POST   /api/locations/:id/share           — body: { userId?, groupId? }
DELETE /api/locations/:id/share/:shareId   — revoke
GET    /api/locations/:id/shares           — list shares (paginated)
POST   /api/locations/:id/share/direct     — generate direct link. Body: { singleUse?, maxUses?, expiresAt? }
GET    /api/share/:token                   — resolve direct share token (NOT under /api/locations)
POST   /api/locations/:shareId/hide        — hide duplicate
DELETE /api/locations/:shareId/hide        — unhide
```

Route ordering: `/api/locations/:id/share/direct` before `/api/locations/:id/share/:shareId`.

Also update the existing `GET /api/locations/shared-with-me` route in `routes/locations.ts` to call `shareRepo.getLocationsSharedWithUser()` instead of returning empty.

- [ ] **Step 2: Write tests, commit**

Commit: "feat(server): sharing API — share, revoke, direct links, hide/unhide"

---

### Task 6: Server — Suggestion Routes

**Files:**
- Create: `server/src/routes/suggestions.ts`
- Create: `server/tests/routes/suggestions.test.ts`

- [ ] **Step 1: Implement 6 suggestion endpoints**

Export `createSuggestionRouter(suggestionService: SuggestionService, suggestionRepo: SuggestionRepository): Router`

```
POST   /api/locations/:id/suggestions      — body: { changesJson, message? }
GET    /api/suggestions/incoming            — paginated (before /:id)
GET    /api/suggestions/outgoing            — paginated (before /:id)
PUT    /api/suggestions/:id/accept          — body: { responseMsg? }
PUT    /api/suggestions/:id/reject          — body: { responseMsg? }
PUT    /api/suggestions/:id/withdraw
```

- [ ] **Step 2: Write tests, commit**

Commit: "feat(server): suggestion API — submit, accept, reject, withdraw"

---

### Task 7: Server — Wire Up, Update Location Repo & Soft Delete

**Files:**
- Modify: `server/src/repositories/factory.ts`
- Modify: `server/src/app.ts`
- Modify: `server/src/routes/locations.ts` — update shared-with-me
- Modify: `server/src/repositories/postgres/location-repo.ts` — real getSharedWithUser

- [ ] **Step 1: Update factory**

Add `shares: ShareRepository`, `suggestions: SuggestionRepository`, `deepLinks: DeepLinkRepository` to Repositories interface and factory.

- [ ] **Step 2: Update getSharedWithUser in location repo**

Replace the stub with a real implementation that joins `location_shares` with `server_locations`.

To do this properly, the LocationRepository needs access to the user's group IDs. The simplest approach: change the method signature to accept `userGroupIds: string[]` as a parameter, OR have the route handler pass them.

Since the route handler already has `req.userGroupIds`, update the `shared-with-me` route to pass them through.

- [ ] **Step 3: Update shared-with-me route**

In `routes/locations.ts`, replace the empty stub:
```typescript
router.get('/api/locations/shared-with-me', async (req, res) => {
    const result = await repos.shares.getLocationsSharedWithUser(
        req.userId!, req.userGroupIds || [], cursor, limit
    );
    res.json(result);
});
```

- [ ] **Step 4: Wire services and routes in app.ts**

Create SharingService and SuggestionService in createApp. Mount sharing and suggestion routers.

- [ ] **Step 5: Run all tests, commit**

Commit: "feat(server): wire sharing and suggestions, real shared-with-me implementation"

---

### Task 8: Android — API Models + Retrofit Endpoints

**Files:**
- Modify: `app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiModels.kt`
- Modify: `app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiService.kt`

- [ ] **Step 1: Add sharing/suggestion models and endpoints**

Models:
```kotlin
data class ServerLocationShare(id, locationId, sharedBy, sharedWithUser?, sharedWithGroup?, isActive, sharedAt)
data class ServerChangeSuggestion(id, locationId, suggestedBy, status, changesJson, message?, responseMsg?, createdAt, respondedAt?)
data class ShareWithBody(userId: String? = null, groupId: String? = null)
data class DirectShareBody(singleUse: Boolean? = null, maxUses: Int? = null)
data class DirectShareResponse(token: String, url: String)
data class SubmitSuggestionBody(changesJson: String, message: String? = null)
data class SuggestionResponseBody(responseMsg: String? = null)
```

Endpoints:
```kotlin
// Sharing
@POST("api/locations/{id}/share")
suspend fun shareLocation(@Path("id") id: String, @Body body: ShareWithBody): Response<ServerLocationShare>

@DELETE("api/locations/{id}/share/{shareId}")
suspend fun revokeShare(@Path("id") id: String, @Path("shareId") shareId: String): Response<Unit>

@GET("api/locations/{id}/shares")
suspend fun getLocationShares(@Path("id") id: String): Response<PaginatedResponse<ServerLocationShare>>

@POST("api/locations/{id}/share/direct")
suspend fun generateDirectShare(@Path("id") id: String, @Body body: DirectShareBody): Response<DirectShareResponse>

@GET("api/share/{token}")
suspend fun resolveDirectShare(@Path("token") token: String): Response<ServerLocation>

// Suggestions
@POST("api/locations/{id}/suggestions")
suspend fun submitSuggestion(@Path("id") id: String, @Body body: SubmitSuggestionBody): Response<ServerChangeSuggestion>

@GET("api/suggestions/incoming")
suspend fun getIncomingSuggestions(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerChangeSuggestion>>

@GET("api/suggestions/outgoing")
suspend fun getOutgoingSuggestions(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerChangeSuggestion>>

@PUT("api/suggestions/{id}/accept")
suspend fun acceptSuggestion(@Path("id") id: String, @Body body: SuggestionResponseBody): Response<ServerChangeSuggestion>

@PUT("api/suggestions/{id}/reject")
suspend fun rejectSuggestion(@Path("id") id: String, @Body body: SuggestionResponseBody): Response<ServerChangeSuggestion>

@PUT("api/suggestions/{id}/withdraw")
suspend fun withdrawSuggestion(@Path("id") id: String): Response<ServerChangeSuggestion>
```

Commit: "feat: add sharing and suggestion API models and Retrofit endpoints"

---

### Task 9: Android — Share Location Dialog + Shared With Me

**Files:**
- Create: share location activity/viewmodel + layouts
- Create: shared-with-me activity/viewmodel + layouts

- [ ] **Step 1: Share Location Activity**

Launched from location detail/edit with location ID as intent extra. Shows:
- "Share with Friend" — dropdown/autocomplete of friends list
- "Share with Group" — dropdown of user's groups
- "Generate Direct Link" — button that creates a link, shows copy/share dialog
- List of current shares with revoke buttons

ViewModel calls API to share, revoke, generate direct link.

- [ ] **Step 2: Shared With Me Activity**

Shows paginated list of locations shared with the user. Each item: location name, shared by, actions (view detail, hide). Pull-to-local creates a copy in the local Room database.

ViewModel calls `getSharedLocations()` API.

- [ ] **Step 3: Register activities, add layouts, commit**

Commit: "feat: share location dialog and shared-with-me screen"

---

### Task 10: Android — Suggestion Composer + Incoming Suggestions

**Files:**
- Create: suggestion composer activity/viewmodel + layout
- Create: incoming suggestions activity/viewmodel + layout

- [ ] **Step 1: Suggestion Composer**

Launched from a shared location. Shows the current location fields side-by-side with editable inputs. User modifies fields, writes optional message, taps "Submit Suggestion".

ViewModel builds a JSON diff of changed fields and calls `submitSuggestion()`.

- [ ] **Step 2: Incoming Suggestions Screen**

Shows list of pending suggestions on the user's locations. Each item shows: location name, suggester, changed fields preview. Tap to expand: side-by-side current vs proposed values for each changed field. Accept/reject buttons with optional response message.

ViewModel calls `getIncomingSuggestions()`, `acceptSuggestion()`, `rejectSuggestion()`.

- [ ] **Step 3: Register, commit**

Commit: "feat: suggestion composer and incoming suggestions review screen"

---

### Task 11: Android — Deep Links, PendingOperation, Navigation

**Files:**
- Modify: AndroidManifest.xml — deep link for `pevplugshare://location/{token}`
- Modify: PendingOperationEntity — add suggestion types
- Modify: SettingsActivity — add sharing/suggestions navigation

- [ ] **Step 1: Location deep link handler**

Add intent filter for `pevplugshare://location` scheme. When received, extract token, call `resolveDirectShare(token)` API, show location detail.

- [ ] **Step 2: Extend PendingOperation for offline suggestions**

Add to PendingOperationType enum:
```kotlin
SUBMIT_SUGGESTION,
```

When submitting a suggestion offline, queue it as a PendingOperation with the changesJson as operationData.

- [ ] **Step 3: Settings navigation**

Add "Shared With Me" and "Suggestions" buttons to settings (only visible when server connected).

- [ ] **Step 4: Commit**

Commit: "feat: location deep links, offline suggestion queueing, settings navigation"

---

### Task 12: Final Verification

- [ ] **Step 1: Run server tests**

```bash
cd server && npm test
```

- [ ] **Step 2: Build Android**

```bash
cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: Phase 6 complete — sharing and collaborative editing (server + Android)"
```
