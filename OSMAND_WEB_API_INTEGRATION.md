# OsmAnd Web API Integration

This document describes the implementation of the OsmAnd web API integration for managing favorites using username/password authentication with persistent cookie storage and automatic session refresh.

## Overview

The OsmAnd web API provides an alternative authentication method to OAuth2, using username/password credentials with cookie-based session management. The implementation includes:

- **Persistent Cookie Storage**: Cookies are encrypted and stored securely across app restarts
- **Automatic Session Refresh**: Background worker refreshes session every 6 hours
- **Secure Credential Storage**: Username/password encrypted using AES-256

## Implementation

### API Endpoints

1. **Login**: `POST https://osmand.net/mapapi/auth/login`
   - Payload: `{ username: "email@example.com", password: "password", lang: "en" }`
   - Returns: Session cookie for subsequent requests
   - Cookie is automatically saved to encrypted storage

2. **List Files**: `GET https://osmand.net/mapapi/list-files`
   - Requires: Session cookie from login (automatically loaded)
   - Returns: JSON object with `uniqueFiles` array
   - Files with `type: "FAVOURITES"` contain favorite categories

### Key Components

#### CookieManager (`CookieManager.kt`)

Manages persistent cookie storage with encryption:
- `saveCookies()`: Encrypts and stores cookies to encrypted shared preferences
- `loadCookies()`: Loads and decrypts cookies, filtering expired ones
- `clearCookies()`: Removes all stored cookies
- `hasCookies()`: Checks if valid cookies exist

Features:
- Two-tier caching: In-memory cache + encrypted persistent storage
- Automatic expiration handling
- Thread-safe cookie serialization/deserialization

#### SessionRefreshWorker (`SessionRefreshWorker.kt`)

Background worker for automatic session refresh:
- Runs every 6 hours using WorkManager
- Only runs when network is available
- Automatically re-authenticates using stored credentials
- Retries on failure with exponential backoff
- Uses Hilt for dependency injection

#### Data Models (`OsmandApiModels.kt`)

- `OsmandLoginRequest`: Login credentials payload
- `OsmandLoginResponse`: Login response with status and user ID
- `OsmandListFilesResponse`: Response containing file list
- `OsmandFileInfo`: Individual file information including type and details
- `OsmandFileDetails` & `OsmandFileAnalysis`: File metadata including waypoint categories
- `SerializableCookie`: Cookie wrapper for JSON serialization

#### API Service (`OsmandApiService.kt`)

Added web API endpoints:
```kotlin
@POST("auth/login")
suspend fun webLogin(@Body loginRequest: OsmandLoginRequest): OsmandLoginResponse

@GET("list-files")
suspend fun listFiles(): OsmandListFilesResponse
```

#### API Client (`OsmandApiClient.kt`)

Enhanced with:
- Cookie jar using CookieManager for persistent storage
- `getWebApiRetrofit()`: Creates Retrofit instance for web API base URL
- `clearCookies()`: Clears both in-memory and persistent cookies

#### Token Manager (`TokenManager.kt`)

Added support for:
- Storing web API credentials securely using encrypted shared preferences
- `saveWebApiCredentials()`: Stores username/password
- `getPassword()`: Retrieves stored password
- `getAuthType()`: Determines authentication method (OAuth vs Web API)

#### Auth Repository (`AuthAndLocationRepositories.kt`)

New methods:
```kotlin
suspend fun loginWithWebApi(username: String, password: String): Result<String>
suspend fun refreshWebApiSession(): Result<Unit>
suspend fun getFavoritesCategories(): Result<List<String>>
```

Session management:
- `loginWithWebApi()`: Performs login, saves cookies, and schedules periodic refresh
- `logout()`: Clears cookies and cancels session refresh worker

## Usage Example

```kotlin
// Login with username/password
val result = authRepository.loginWithWebApi("user@example.com", "password123")
result.onSuccess { userId ->
    // Login successful, session cookie stored and auto-refresh scheduled
    println("Logged in as: $userId")
}

// Get favorites categories (uses stored cookie automatically)
val categories = authRepository.getFavoritesCategories()
categories.onSuccess { categoryList ->
    categoryList.forEach { category ->
        println("Category: $category")
    }
}

// Manually refresh session if needed
authRepository.refreshWebApiSession()

// Logout
authRepository.logout() // Clears cookies and stops auto-refresh
```

## Cookie Lifecycle

1. **Login**: User provides credentials → Login API called → Cookie received and encrypted → Saved to storage → Session refresh worker scheduled
2. **Subsequent Requests**: Cookie loaded from storage → Sent with request → If new cookie received, updated in storage
3. **Background Refresh**: Every 6 hours → Worker re-authenticates → New cookie stored → Session kept alive
4. **Logout**: All cookies cleared → Worker cancelled → Storage wiped

## Security

- **Credentials**: Stored using Android's EncryptedSharedPreferences with AES256 encryption
- **Cookies**: Encrypted in persistent storage using AES256-GCM
- **Keys**: Encrypted using AES256-SIV
- **In-Memory**: Cookies cached for performance, cleared on logout
- **Transport**: All communications over HTTPS

## Session Refresh Configuration

Default settings in `SessionRefreshWorker`:
- **Refresh Interval**: 6 hours
- **Work Policy**: KEEP (existing work not replaced)
- **Constraints**: Requires network connectivity
- **Retry Policy**: Automatic retry on failure

To customize:
```kotlin
// In SessionRefreshWorker.kt
companion object {
    private const val REFRESH_INTERVAL_HOURS = 6L // Change to desired interval
}
```

## Migration from OAuth2

The implementation supports both OAuth2 and web API authentication methods:
- **OAuth2**: Uses bearer token authentication
- **Web API**: Uses cookie-based session authentication with auto-refresh

The `AuthType` enum in `TokenManager` tracks which method is in use.

## Testing

To test the web API integration:

1. Call `loginWithWebApi()` with valid OsmAnd credentials
2. Verify session cookie is received and stored
3. Call `getFavoritesCategories()` to retrieve favorite categories
4. Wait or manually trigger session refresh
5. Verify cookies are updated after refresh

## Troubleshooting

**Session expires quickly**: Check if cookies are being saved correctly
**Worker not running**: Verify network constraint is satisfied
**Cookies not persisting**: Check encrypted storage permissions

## Future Enhancements

Potential improvements:
- Download and parse GPX files for favorite markers
- Upload new favorites to OsmAnd cloud
- Sync favorites bidirectionally
- Adaptive refresh interval based on cookie expiration time
- Foreground notification during sync
