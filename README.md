# PEV PlugShare

Android application for sharing and discovering PEV (Personal Electric Vehicle) charging locations. Integrates with OsmAnd for navigation and provides OAuth-based authentication for cloud sync.

## Features

- **Location Management**: Add, edit, and share charging locations
- **Map Integration**: View locations on an interactive map using osmdroid
- **OsmAnd Integration**: Sync locations with OsmAnd for turn-by-turn navigation
- **Photo Support**: Add photos to locations with EXIF metadata extraction
- **OAuth Authentication**: Secure cloud sync with OAuth 2.0
- **Offline Support**: Works offline with local database
- **Image Gallery**: Full-screen image viewing with gesture support
- **Category System**: Organize locations by category

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Map**: osmdroid
- **Image Loading**: Glide
- **Authentication**: Custom OAuth 2.0 flow with Chrome Custom Tabs
- **Background Tasks**: WorkManager
- **Location Services**: Google Play Services Location API
- **Camera**: CameraX

## Project Structure

```
app/
├── src/main/
│   ├── java/com/a42r/pevplugshare/
│   │   ├── ai/                  # AI-powered location descriptions
│   │   ├── data/
│   │   │   ├── db/             # Room database entities and DAOs
│   │   │   ├── model/          # Data models
│   │   │   ├── remote/         # API clients
│   │   │   └── repository/     # Data repositories
│   │   ├── di/                 # Dependency injection modules
│   │   ├── service/            # Background services
│   │   ├── ui/                 # Activities, ViewModels, and UI components
│   │   └── util/               # Utility classes
│   ├── cpp/                    # Native C++ code for secure OAuth secrets
│   └── res/                    # Resources (layouts, strings, etc.)
├── build.gradle.kts            # App-level Gradle build file
└── version.properties          # Centralized version management
```

## Building the Project

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 34
- JDK 17
- CMake 3.22.1 or later (for native code)

### Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd PEVPlugShare
   ```

2. **Configure OAuth secrets**:
   
   Copy the template and fill in your OAuth credentials:
   ```bash
   cp secrets.properties.template secrets.properties
   ```
   
   Edit `secrets.properties` with your OAuth service details:
   ```properties
   oauth.dev.callback.url=https://your-oauth-service.com/oauth/callback
   oauth.dev.shared.secret=your-shared-secret
   oauth.dev.deep.link.scheme=yourapp://oauth/callback
   
   # For production builds:
   oauth.prod.callback.url=https://your-oauth-service.com/oauth/callback
   oauth.prod.shared.secret=your-shared-secret
   oauth.prod.deep.link.scheme=yourapp://oauth/callback
   ```
   
   **IMPORTANT**: Never commit `secrets.properties` to version control!

3. **Build the app**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

## Release Management

PEV PlugShare uses an automated release management system for Google Play Store releases.

### Quick Start

```bash
# Create a patch release (bug fixes)
./release.sh patch

# Create a minor release (new features)
./release.sh minor

# Create a major release (breaking changes)
./release.sh major

# Create a pre-release for testing
./release.sh patch --pre-release
```

### Documentation

For detailed information about the release process, see [RELEASE_PROCESS.md](RELEASE_PROCESS.md).

The release script automatically:
- ✅ Verifies git working directory is clean
- 📖 Reads current version from `version.properties`
- 🔢 Calculates new version and increments version code
- 📝 Updates `version.properties`
- 💾 Commits version bump
- 🏷️ Creates git tag
- 🏗️ Builds signed AAB
- 📄 Generates release notes

### Version Numbering

The project follows [Semantic Versioning](https://semver.org/):
- **MAJOR**: Incompatible API changes
- **MINOR**: New features (backwards-compatible)
- **PATCH**: Bug fixes (backwards-compatible)
- **PRERELEASE**: Optional suffix for pre-releases (e.g., `rc.0`, `beta.1`)

Current version is managed in `version.properties`.

## OAuth Service

PEV PlugShare uses a custom OAuth 2.0 callback service for authentication. The service is deployed at `https://oauth.a42r.com`.

### OAuth Flow

1. App initiates OAuth flow by opening Chrome Custom Tab
2. User authenticates with OAuth provider
3. OAuth service receives callback and stores session in database
4. App polls OAuth service to retrieve access token
5. App uses access token for authenticated API requests

### OAuth Service Repository

The OAuth callback service is located in `oauth-callback-service/`. See [oauth-callback-service/README.md](oauth-callback-service/README.md) for deployment instructions.

## Development

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Linting

```bash
./gradlew lint
```

### Code Style

The project follows standard Kotlin coding conventions. Use Android Studio's built-in formatter (Ctrl+Alt+L).

## Deployment

### Google Play Store

1. Create a release using the release script:
   ```bash
   ./release.sh patch
   ```

2. Push commits and tags:
   ```bash
   git push origin master
   git push origin --tags
   ```

3. Upload AAB to Google Play Console:
   - Go to [Google Play Console](https://play.google.com/console)
   - Navigate to **Release** → **Testing** → **Internal testing**
   - Upload `app/build/outputs/bundle/release/app-release.aab`
   - Use content from `RELEASE_NOTES.txt` for release notes

### OAuth Service Deployment

See [oauth-callback-service/DEPLOYMENT.md](oauth-callback-service/DEPLOYMENT.md) for instructions on deploying the OAuth callback service with Traefik and Authelia.

## Configuration Files

- **version.properties**: Version numbering (tracked in git)
- **secrets.properties**: OAuth secrets (NOT tracked in git)
- **keystore.properties**: Signing keystore config (NOT tracked in git)
- **play-service-account.json**: Google Play publishing credentials (NOT tracked in git)

## Security

- OAuth secrets are stored in `secrets.properties` (not tracked in git)
- Secrets are compiled into native C++ code at build time
- OAuth shared secret is never exposed in Java/Kotlin code
- Chrome Custom Tabs used for OAuth flow (more secure than WebView)

## Troubleshooting

### Build Issues

**Issue**: "secrets.properties not found"
- **Solution**: Copy `secrets.properties.template` to `secrets.properties` and fill in your credentials

**Issue**: "CMake error"
- **Solution**: Install CMake 3.22.1 or later from Android Studio SDK Manager

**Issue**: "R8 minification error with Hilt"
- **Solution**: Minification is currently disabled in release builds. ProGuard rules need to be updated for Hilt.

### OAuth Issues

**Issue**: "OAuth callback not working"
- **Solution**: Check that `oauth.dev.callback.url` in `secrets.properties` matches your OAuth service URL

**Issue**: "Signature validation failed"
- **Solution**: Ensure `oauth.dev.shared.secret` matches the shared secret configured in the OAuth service database

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and lint
5. Submit a pull request

## License

[Add your license here]

## Contact

- **Developer**: philg@a42r.com
- **OAuth Service**: https://oauth.a42r.com/admin/ (Authelia protected)

## Acknowledgments

- [OsmAnd](https://osmand.net/) for navigation integration
- [osmdroid](https://github.com/osmdroid/osmdroid) for map rendering
- Reference release management from [OSM-EUC-World](https://github.com/a42r/OSM-EUC-World) project
