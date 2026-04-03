# Central Location Server — Phase 5 Implementation Plan (Friends & Groups)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the friend request/accept/decline flow and friend groups with admin roles, membership management, and group invite via QR code/deep link — both server-side (repositories, routes, tests) and Android-side (API integration, UI screens).

**Architecture:** Server: FriendRepository and GroupRepository implementations (PostgreSQL + MySQL via Knex), friendship and group route handlers, notification creation on friend events. Android: new Retrofit endpoints in ServerApiService, new FriendsActivity and GroupsActivity with ViewModels, deep link handling for group invites.

**Tech Stack:** Server: existing Express/TypeScript/Knex stack. Android: existing Retrofit/Hilt/Room stack, QR code via ZXing library for generation.

**Spec:** `docs/features/CENTRAL_LOCATION_SERVER_SPEC.md` — Friendships, Friend Groups, Group Invitations sections

**Prerequisite:** Server Phases 1-3, Android Phase 4 complete.

---

## Scope

### Server-Side (Node.js)

1. **FriendRepository** — interface + PostgreSQL/MySQL implementations + tests
2. **GroupRepository** — interface + PostgreSQL/MySQL implementations + tests (includes group members, invitations)
3. **Friend service** — business logic for friend requests, notifications
4. **Group service** — business logic for groups, members, invitations
5. **Friendship routes** — 7 endpoints
6. **Group routes** — 14 endpoints (replace stubs in admin console too)
7. **Repository factory update** — add friend + group repos
8. **App factory update** — wire new routes
9. **Server tests**

### Android-Side

10. **Server API service update** — add friend + group endpoints to Retrofit
11. **Friends screen** — list friends, search/add, manage requests
12. **Groups screen** — create/manage groups, members, invites
13. **Deep link handling** — group invite via `pevplugshare://group/{token}`
14. **Settings navigation** — add Friends & Groups links

---

## File Structure

### Server — New Files

```
server/src/
  repositories/
    postgres/
      friend-repo.ts
      group-repo.ts
    mysql/
      friend-repo.ts
      group-repo.ts

  services/
    friend-service.ts
    group-service.ts

  routes/
    friends.ts
    groups.ts

server/tests/
  repositories/
    friend-repo.test.ts
    group-repo.test.ts
  routes/
    friends.test.ts
    groups.test.ts
```

### Server — Modified Files

```
server/src/repositories/interfaces.ts       # Add FriendRepository, GroupRepository methods
server/src/repositories/factory.ts          # Add friend + group repos
server/src/app.ts                           # Wire friend + group routes
server/src/admin/api/groups.ts              # Replace stubs with real implementations
```

### Android — New Files

```
app/src/main/java/com/a42r/pevplugshare/
  ui/friends/
    FriendsActivity.kt
    FriendsViewModel.kt
  ui/groups/
    GroupsActivity.kt
    GroupsViewModel.kt
    GroupDetailActivity.kt
    GroupDetailViewModel.kt

app/src/main/res/layout/
  activity_friends.xml
  item_friend.xml
  item_friend_request.xml
  activity_groups.xml
  item_group.xml
  activity_group_detail.xml
  item_group_member.xml
```

### Android — Modified Files

```
app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiService.kt    # Add friend/group endpoints
app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiModels.kt     # Add friend/group models
app/src/main/java/com/a42r/pevplugshare/ui/settings/SettingsActivity.kt  # Add friends/groups nav
app/src/main/AndroidManifest.xml                                         # Register activities + deep links
app/src/main/res/values/strings.xml                                      # New strings
```

---

### Task 1: Server — Friend Repository

**Files:**
- Modify: `server/src/repositories/interfaces.ts`
- Create: `server/src/repositories/postgres/friend-repo.ts`
- Create: `server/src/repositories/mysql/friend-repo.ts`
- Create: `server/tests/repositories/friend-repo.test.ts`

- [ ] **Step 1: Define FriendRepository interface**

Replace the empty `FriendRepository` in `interfaces.ts`:

```typescript
export interface FriendRepository {
  sendRequest(requesterId: string, addresseeId: string): Promise<Friendship>;
  getRequestById(id: string): Promise<Friendship | null>;
  acceptRequest(id: string): Promise<Friendship>;
  declineRequest(id: string): Promise<Friendship>;
  removeFriend(userId: string, friendId: string): Promise<boolean>;
  getFriends(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<User>>;
  getIncomingRequests(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Friendship>>;
  getOutgoingRequests(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<Friendship>>;
  areFriends(userId1: string, userId2: string): Promise<boolean>;
}
```

Add import for `Friendship` from models.

- [ ] **Step 2: Implement PostgreSQL friend repo**

Key implementation details:
- `sendRequest()`: Insert with uuid, status 'PENDING', requested_at now. Check no existing friendship in either direction first (throw if exists).
- `acceptRequest()`: UPDATE status = 'ACCEPTED', responded_at = now WHERE id AND status = 'PENDING'
- `declineRequest()`: UPDATE status = 'DECLINED', responded_at = now
- `removeFriend()`: DELETE WHERE (requester_id = userId AND addressee_id = friendId) OR (requester_id = friendId AND addressee_id = userId)
- `getFriends()`: SELECT users.* WHERE accepted friendships involving userId (join users table on the other party). Cursor pagination.
- `getIncomingRequests()`: WHERE addressee_id = userId AND status = 'PENDING'
- `getOutgoingRequests()`: WHERE requester_id = userId AND status = 'PENDING'
- `areFriends()`: EXISTS check for ACCEPTED friendship in either direction

- [ ] **Step 3: Copy to MySQL (identical via Knex)**

- [ ] **Step 4: Write tests**

Test: send request, accept, decline, remove, list friends (paginated), list requests, areFriends, duplicate request rejection, self-request rejection.

- [ ] **Step 5: Run tests, commit**

Run: `cd server && npx jest tests/repositories/friend-repo.test.ts --forceExit`
Commit: "feat(server): friend repository — send/accept/decline/remove with tests"

---

### Task 2: Server — Group Repository

**Files:**
- Modify: `server/src/repositories/interfaces.ts`
- Create: `server/src/repositories/postgres/group-repo.ts`
- Create: `server/src/repositories/mysql/group-repo.ts`
- Create: `server/tests/repositories/group-repo.test.ts`

- [ ] **Step 1: Define GroupRepository interface**

Replace empty `GroupRepository`:

```typescript
export interface GroupRepository {
  // Groups
  create(ownerId: string, name: string, description?: string): Promise<FriendGroup>;
  getById(id: string): Promise<FriendGroup | null>;
  update(id: string, fields: { name?: string; description?: string }): Promise<FriendGroup>;
  delete(id: string): Promise<boolean>;
  getByUser(userId: string, cursor?: string, limit?: number): Promise<PaginatedResult<FriendGroup>>;
  listAll(cursor?: string, limit?: number): Promise<PaginatedResult<FriendGroup>>;

  // Members
  addMember(groupId: string, userId: string, role: GroupMemberRole): Promise<GroupMember>;
  removeMember(groupId: string, userId: string): Promise<boolean>;
  setMemberRole(groupId: string, userId: string, role: GroupMemberRole): Promise<GroupMember>;
  getMembers(groupId: string): Promise<GroupMember[]>;
  isMember(groupId: string, userId: string): Promise<boolean>;
  isAdmin(groupId: string, userId: string): Promise<boolean>;
  getUserGroupIds(userId: string): Promise<string[]>;

  // Invitations
  createInvitation(groupId: string, invitedBy: string, token: string, expiresAt?: Date): Promise<GroupInvitation>;
  getInvitationByToken(token: string): Promise<GroupInvitation | null>;
  getInvitationById(id: string): Promise<GroupInvitation | null>;
  getPendingInvitations(groupId: string): Promise<GroupInvitation[]>;
  updateInvitationStatus(id: string, status: GroupInvitationStatus, respondedBy?: string): Promise<GroupInvitation>;
}
```

Add imports for `FriendGroup`, `GroupMember`, `GroupMemberRole`, `GroupInvitation`, `GroupInvitationStatus`.

- [ ] **Step 2: Implement PostgreSQL group repo**

Key details:
- `getByUser()`: Groups where ownerId = userId OR userId in friend_group_members. Deduplicate.
- `isAdmin()`: Check if userId is owner OR has role 'ADMIN' in members table.
- `getUserGroupIds()`: Return array of group_id where user is owner or member.
- `createInvitation()`: Generate token, insert into group_invitations.

- [ ] **Step 3: Copy to MySQL**

- [ ] **Step 4: Write comprehensive tests**

Test: CRUD groups, add/remove members, role management, invitation lifecycle, getUserGroupIds, isAdmin checks (owner implicit admin).

- [ ] **Step 5: Run tests, commit**

Commit: "feat(server): group repository — CRUD, members, roles, invitations with tests"

---

### Task 3: Server — Friend & Group Services

**Files:**
- Create: `server/src/services/friend-service.ts`
- Create: `server/src/services/group-service.ts`

- [ ] **Step 1: Create friend service**

```typescript
export class FriendService {
  constructor(
    private friendRepo: FriendRepository,
    private userRepo: UserRepository,
    private notificationRepo: NotificationRepository
  ) {}

  async sendRequest(requesterId: string, addresseeEmail?: string, addresseeId?: string): Promise<Friendship> {
    // Resolve addressee by email or ID
    // Check not self-request
    // Check not already friends
    // Send request
    // Create FRIEND_REQUEST notification for addressee
  }

  async acceptRequest(requestId: string, userId: string): Promise<Friendship> {
    // Verify request exists and addressee is userId
    // Accept
    // Notify requester
  }

  // ... declineRequest, removeFriend
}
```

- [ ] **Step 2: Create group service**

```typescript
export class GroupService {
  constructor(
    private groupRepo: GroupRepository,
    private friendRepo: FriendRepository,
    private notificationRepo: NotificationRepository
  ) {}

  async createGroup(ownerId: string, name: string, description?: string): Promise<FriendGroup> { ... }
  async addMembers(groupId: string, userIds: string[], adminUserId: string): Promise<void> { ... }
  async generateInvite(groupId: string, userId: string): Promise<{ token: string; url: string }> { ... }
  async claimInvite(token: string, userId: string): Promise<GroupInvitation> { ... }
  async approveInvitation(invitationId: string, adminUserId: string): Promise<void> { ... }
  async declineInvitation(invitationId: string, adminUserId: string): Promise<void> { ... }
}
```

- [ ] **Step 3: Commit**

Commit: "feat(server): friend and group services with notification creation"

---

### Task 4: Server — Friendship Routes

**Files:**
- Create: `server/src/routes/friends.ts`
- Create: `server/tests/routes/friends.test.ts`

- [ ] **Step 1: Implement all 7 friendship endpoints**

Export `createFriendRouter(friendService: FriendService, repos: Repositories): Router`

- `POST /api/friends/request` — body: `{ email?: string, userId?: string }`. Must provide one.
- `GET /api/friends/requests/incoming` — paginated
- `GET /api/friends/requests/outgoing` — paginated
- `PUT /api/friends/requests/:id/accept`
- `PUT /api/friends/requests/:id/decline`
- `DELETE /api/friends/:userId` — remove friend
- `GET /api/friends` — list accepted friends, paginated

IMPORTANT: Static segments (`requests`) before dynamic (`:id`, `:userId`).

- [ ] **Step 2: Write tests**

Test all endpoints with two test users. Test: send request, list incoming/outgoing, accept, list friends, remove, decline flow, error cases (self-request, duplicate).

- [ ] **Step 3: Commit**

Commit: "feat(server): friendship API — request, accept, decline, remove, list"

---

### Task 5: Server — Group Routes

**Files:**
- Create: `server/src/routes/groups.ts`
- Create: `server/tests/routes/groups.test.ts`

- [ ] **Step 1: Implement all 14 group endpoints**

Export `createGroupRouter(groupService: GroupService, repos: Repositories): Router`

Route ordering matters — static before dynamic:
```
POST   /api/groups
GET    /api/groups
GET    /api/groups/invite/:token          (before :id)
POST   /api/groups/invite/:token/accept   (before :id)
GET    /api/groups/:id
PUT    /api/groups/:id
DELETE /api/groups/:id
GET    /api/groups/:id/members
POST   /api/groups/:id/members
DELETE /api/groups/:id/members/:userId
PUT    /api/groups/:id/members/:userId/role
POST   /api/groups/:id/invite
GET    /api/groups/:id/invitations
PUT    /api/groups/:id/invitations/:inviteId/approve
PUT    /api/groups/:id/invitations/:inviteId/decline
```

Key auth checks:
- Members list: must be member or owner
- Add/remove members: must be admin or owner
- Change role: must be owner
- Generate invite: must be member (any member can nominate)
- View invitations: must be admin or owner
- Approve/decline invitation: must be admin or owner

- [ ] **Step 2: Write comprehensive tests**

- [ ] **Step 3: Commit**

Commit: "feat(server): group API — CRUD, members, roles, invitations"

---

### Task 6: Server — Wire Up & Update Admin

**Files:**
- Modify: `server/src/repositories/factory.ts`
- Modify: `server/src/app.ts`
- Modify: `server/src/admin/api/groups.ts`

- [ ] **Step 1: Update factory**

Add `friends: FriendRepository` and `groups: GroupRepository` to `Repositories` interface. Instantiate in `createRepositories()`.

- [ ] **Step 2: Update app.ts**

Create FriendService and GroupService in createApp, mount friend and group routers.

- [ ] **Step 3: Replace admin group stubs**

Update `admin/api/groups.ts` to use real GroupRepository methods instead of returning 501.

- [ ] **Step 4: Run all server tests**

Run: `cd server && npm test`
Expected: All pass

- [ ] **Step 5: Commit**

Commit: "feat(server): wire friends and groups into app factory, update admin stubs"

---

### Task 7: Android — API Models & Service Update

**Files:**
- Modify: `app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiModels.kt`
- Modify: `app/src/main/java/com/a42r/pevplugshare/data/api/ServerApiService.kt`

- [ ] **Step 1: Add friend/group models**

```kotlin
data class ServerFriendship(
    @SerializedName("id") val id: String,
    @SerializedName("requesterId") val requesterId: String,
    @SerializedName("addresseeId") val addresseeId: String,
    @SerializedName("status") val status: String,
    @SerializedName("requestedAt") val requestedAt: String?,
    @SerializedName("respondedAt") val respondedAt: String?
)

data class ServerFriendGroup(
    @SerializedName("id") val id: String,
    @SerializedName("ownerId") val ownerId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class ServerGroupMember(
    @SerializedName("groupId") val groupId: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("role") val role: String,
    @SerializedName("addedAt") val addedAt: String?
)

data class ServerGroupInvitation(
    @SerializedName("id") val id: String,
    @SerializedName("groupId") val groupId: String,
    @SerializedName("token") val token: String,
    @SerializedName("status") val status: String
)

data class FriendRequestBody(@SerializedName("email") val email: String? = null, @SerializedName("userId") val userId: String? = null)
data class CreateGroupBody(@SerializedName("name") val name: String, @SerializedName("description") val description: String? = null)
data class AddMembersBody(@SerializedName("userIds") val userIds: List<String>)
data class ChangeRoleBody(@SerializedName("role") val role: String)
data class InviteResponse(@SerializedName("token") val token: String, @SerializedName("url") val url: String)
```

- [ ] **Step 2: Add endpoints to ServerApiService**

```kotlin
// Friends
@POST("api/friends/request")
suspend fun sendFriendRequest(@Body body: FriendRequestBody): Response<ServerFriendship>

@GET("api/friends")
suspend fun getFriends(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null): Response<PaginatedResponse<ServerUser>>

@GET("api/friends/requests/incoming")
suspend fun getIncomingRequests(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerFriendship>>

@GET("api/friends/requests/outgoing")
suspend fun getOutgoingRequests(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerFriendship>>

@PUT("api/friends/requests/{id}/accept")
suspend fun acceptFriendRequest(@Path("id") id: String): Response<ServerFriendship>

@PUT("api/friends/requests/{id}/decline")
suspend fun declineFriendRequest(@Path("id") id: String): Response<ServerFriendship>

@DELETE("api/friends/{userId}")
suspend fun removeFriend(@Path("userId") userId: String): Response<Unit>

// Groups
@POST("api/groups")
suspend fun createGroup(@Body body: CreateGroupBody): Response<ServerFriendGroup>

@GET("api/groups")
suspend fun getGroups(@Query("cursor") cursor: String? = null): Response<PaginatedResponse<ServerFriendGroup>>

@GET("api/groups/{id}")
suspend fun getGroup(@Path("id") id: String): Response<ServerFriendGroup>

@PUT("api/groups/{id}")
suspend fun updateGroup(@Path("id") id: String, @Body body: CreateGroupBody): Response<ServerFriendGroup>

@DELETE("api/groups/{id}")
suspend fun deleteGroup(@Path("id") id: String): Response<Unit>

@GET("api/groups/{id}/members")
suspend fun getGroupMembers(@Path("id") id: String): Response<List<ServerGroupMember>>

@POST("api/groups/{id}/members")
suspend fun addGroupMembers(@Path("id") id: String, @Body body: AddMembersBody): Response<Unit>

@DELETE("api/groups/{id}/members/{userId}")
suspend fun removeGroupMember(@Path("id") id: String, @Path("userId") userId: String): Response<Unit>

@PUT("api/groups/{id}/members/{userId}/role")
suspend fun changeGroupMemberRole(@Path("id") id: String, @Path("userId") userId: String, @Body body: ChangeRoleBody): Response<Unit>

@POST("api/groups/{id}/invite")
suspend fun generateGroupInvite(@Path("id") id: String): Response<InviteResponse>

@GET("api/groups/invite/{token}")
suspend fun getGroupInviteDetails(@Path("token") token: String): Response<ServerGroupInvitation>

@POST("api/groups/invite/{token}/accept")
suspend fun acceptGroupInvite(@Path("token") token: String): Response<Unit>
```

- [ ] **Step 3: Verify compilation, commit**

Commit: "feat: add friend and group API models and Retrofit endpoints"

---

### Task 8: Android — Friends Screen

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/friends/FriendsActivity.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/friends/FriendsViewModel.kt`
- Create: `app/src/main/res/layout/activity_friends.xml`
- Create: `app/src/main/res/layout/item_friend.xml`
- Create: `app/src/main/res/layout/item_friend_request.xml`

- [ ] **Step 1: Create layouts and Activity**

Activity layout: TabLayout (Friends | Requests) + ViewPager2 or just two sections:
- Section 1: "Add Friend" search bar (email input + send button) + friend list (RecyclerView)
- Section 2: "Incoming Requests" list with accept/decline buttons + "Outgoing Requests" list

Friend item: avatar placeholder, display name, email, remove button.
Request item: display name, email, accept/decline buttons (incoming) or "Pending" label (outgoing).

ViewModel: loads friends list, incoming/outgoing requests from ServerApiService. Methods: sendRequest, acceptRequest, declineRequest, removeFriend.

- [ ] **Step 2: Register in manifest, commit**

Commit: "feat: friends screen — list friends, search/add, manage requests"

---

### Task 9: Android — Groups Screen

**Files:**
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/groups/GroupsActivity.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/groups/GroupsViewModel.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/groups/GroupDetailActivity.kt`
- Create: `app/src/main/java/com/a42r/pevplugshare/ui/groups/GroupDetailViewModel.kt`
- Create layouts: `activity_groups.xml`, `item_group.xml`, `activity_group_detail.xml`, `item_group_member.xml`

- [ ] **Step 1: Groups list screen**

Layout: "Create Group" button + RecyclerView of groups. Each group item shows name, member count, role badge (Owner/Admin/Member).

ViewModel: loads groups from API. createGroup method.

- [ ] **Step 2: Group detail screen**

Layout: Group name/description, member list, "Add Member" button (admin only), "Generate Invite Link" button, pending invitations list (admin only).

ViewModel: loads group detail + members. Methods: addMember (select from friends), removeMember, changeRole, generateInvite, approveInvitation, declineInvitation.

Invite link: display as copyable text + option to share via system share sheet.

- [ ] **Step 3: Register activities, commit**

Commit: "feat: groups screen — create, manage, members, invitations"

---

### Task 10: Android — Deep Link Handling & Settings Navigation

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/a42r/pevplugshare/ui/settings/SettingsActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add deep link intent filter**

In AndroidManifest.xml, add to an appropriate activity (or create a DeepLinkActivity):

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="pevplugshare" android:host="group" />
</intent-filter>
```

When the app receives `pevplugshare://group/{token}`, extract the token and call the group invite accept flow.

- [ ] **Step 2: Add Friends & Groups to settings**

Add buttons in SettingsActivity (only visible when server connected):
- "Friends" → FriendsActivity
- "Groups" → GroupsActivity

- [ ] **Step 3: Commit**

Commit: "feat: deep link handling for group invites, settings navigation"

---

### Task 11: Final Verification

- [ ] **Step 1: Run server tests**

```bash
cd server && npm test
```

- [ ] **Step 2: Build Android app**

```bash
cd /home/philg/src/AndroidStudioProjects/PEVPlugShare && ./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: Phase 5 complete — friends and groups (server + Android)"
```
