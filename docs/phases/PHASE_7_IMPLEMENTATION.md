# Phase 7: Offline Support Implementation Plan

## Objective
Implement complete offline functionality allowing users to add/edit locations while offline, with automatic sync when connectivity returns.

## Requirements
1. **Offline Queue** - Store pending changes locally
2. **Sync Status Tracking** - Track which operations succeeded/failed
3. **Conflict Resolution** - Handle sync conflicts gracefully
4. **Auto-sync** - Detect connectivity changes and auto-sync
5. **Offline Indicator** - Visual feedback on app status
6. **Pending UI** - Show pending operations to user

## Architecture

### Data Model Updates
- Add `SyncStatus` field to Location entity (already exists)
  - SYNCED: Location synchronized with server
  - PENDING: Awaiting sync
  - FAILED: Sync failed, retry available
  - SYNCING: Currently syncing

### Database Changes
- Existing LocationEntity already has syncStatus
- Add pending operations queue table:
  - PendingOperation (id, locationId, operationType, timestamp, retryCount)

### Network Detection
- ConnectivityManager to monitor network changes
- WorkManager for background sync jobs
- Exponential backoff for retries

### Components to Create
1. **SyncManager** - Handles sync logic
2. **OfflineIndicator** - UI component for status
3. **PendingOperationRepository** - Queue management
4. **ConnectivityObserver** - Network monitoring
5. **AutoSyncWorker** - Background sync job

## Implementation Steps

### Step 1: Create Offline Infrastructure
- [ ] PendingOperationEntity & DAO
- [ ] PendingOperationRepository
- [ ] ConnectivityObserver
- [ ] SyncManager

### Step 2: Database Updates
- [ ] Add pending_operations table
- [ ] Migration for new table

### Step 3: Sync Logic
- [ ] Queue add/edit operations when offline
- [ ] Mark locations as PENDING
- [ ] Implement retry logic
- [ ] Handle conflict scenarios

### Step 4: Background Sync
- [ ] AutoSyncWorker for WorkManager
- [ ] Connectivity change listeners
- [ ] Backoff strategy
- [ ] Error logging

### Step 5: UI Updates
- [ ] Offline indicator in top bar
- [ ] Pending status on location items
- [ ] Sync status in AboutActivity
- [ ] User feedback during sync

### Step 6: Testing
- [ ] Simulate offline mode
- [ ] Queue operations offline
- [ ] Sync when online
- [ ] Conflict handling
- [ ] Multiple pending operations

## File Structure
```
data/
  db/
    dao/
      - PendingOperationDao.kt (NEW)
    entity/
      - PendingOperationEntity.kt (NEW)
  repository/
    - PendingOperationRepository.kt (NEW)
    - SyncManager.kt (NEW)
    - ConnectivityObserver.kt (NEW)
ui/
  common/
    - OfflineIndicator.kt (NEW)
ui/list/
    - LocationListViewModel.kt (UPDATE)
    - activity_location_list.xml (UPDATE)
worker/
  - AutoSyncWorker.kt (NEW)
```

## Implementation Priority
1. ConnectivityObserver (foundation)
2. PendingOperationEntity & Repository
3. SyncManager core logic
4. AutoSyncWorker
5. Offline indicator UI
6. UI updates to show pending status
7. Testing & refinement

## Expected Outcomes
- ✅ Add locations while offline
- ✅ Edit locations while offline
- ✅ Automatic sync when online
- ✅ Retry failed syncs
- ✅ Visual pending indicators
- ✅ User sees sync progress
- ✅ Conflict-free synchronization
