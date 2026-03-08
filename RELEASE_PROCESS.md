# Release Process for PEV PlugShare

This document describes the release management process for PEV PlugShare, including versioning strategy, release workflows, and Google Play publishing.

## Table of Contents

1. [Overview](#overview)
2. [Version Numbering](#version-numbering)
3. [Release Script Usage](#release-script-usage)
4. [Pre-Release Workflow](#pre-release-workflow)
5. [Stable Release Workflow](#stable-release-workflow)
6. [Google Play Publishing](#google-play-publishing)
7. [Rollback Procedures](#rollback-procedures)
8. [Troubleshooting](#troubleshooting)

## Overview

PEV PlugShare uses an automated release management system with:

- **Centralized versioning** via `version.properties`
- **Automated release script** (`release.sh`) for building and tagging releases
- **Pre-release support** for testing (e.g., `v1.2.2-rc.0`)
- **Automatic version bumping** and git tagging
- **Release notes generation** for Play Store submission
- **Optional automatic publishing** to Google Play Internal Testing

## Version Numbering

### Semantic Versioning

PEV PlugShare follows [Semantic Versioning 2.0.0](https://semver.org/):

```
MAJOR.MINOR.PATCH[-PRERELEASE]

Examples:
- 1.2.1       (stable release)
- 1.2.2-rc.0  (pre-release candidate 0)
- 1.2.2-rc.1  (pre-release candidate 1)
- 1.2.2       (stable release, graduated from rc.1)
```

### Version Components

- **MAJOR**: Incremented for incompatible API changes or major rewrites
- **MINOR**: Incremented for new features (backwards-compatible)
- **PATCH**: Incremented for bug fixes (backwards-compatible)
- **VERSION_CODE**: Android version code, auto-incremented on every release
- **PRERELEASE**: Optional suffix for pre-releases (e.g., `rc.0`, `beta.1`, `alpha.1`)

### Version Configuration

All version numbers are stored in `version.properties`:

```properties
VERSION_MAJOR=1
VERSION_MINOR=2
VERSION_PATCH=1
VERSION_CODE=6

# Uncomment for pre-releases
# VERSION_PRERELEASE=rc.0
```

**IMPORTANT**: Do not edit `version.properties` manually. Always use `release.sh` to bump versions.

## Release Script Usage

### Basic Syntax

```bash
./release.sh [patch|minor|major] [version_code] [--pre-release] [--publish-internal-testing]
```

### Arguments

- **Bump type** (optional, defaults to `patch`):
  - `patch`: Bug fixes (1.2.1 → 1.2.2)
  - `minor`: New features (1.2.1 → 1.3.0)
  - `major`: Breaking changes (1.2.1 → 2.0.0)

- **version_code** (optional): Manually specify the version code instead of auto-incrementing

- **--pre-release** (optional): Create a pre-release version (e.g., `v1.2.2-rc.0`)

- **--publish-internal-testing** (optional): Automatically publish to Google Play Internal Testing (requires `play-service-account.json`)

### Examples

#### Patch Release (Bug Fixes)
```bash
./release.sh patch
# Result: 1.2.1 → 1.2.2
```

#### Minor Release (New Features)
```bash
./release.sh minor
# Result: 1.2.1 → 1.3.0
```

#### Major Release (Breaking Changes)
```bash
./release.sh major
# Result: 1.2.1 → 2.0.0
```

#### Pre-Release (Release Candidate)
```bash
./release.sh patch --pre-release
# Result: 1.2.1 → 1.2.2-rc.0

# Subsequent pre-releases increment the rc number
./release.sh patch --pre-release
# Result: 1.2.2-rc.0 → 1.2.2-rc.1
```

#### Graduate Pre-Release to Stable
```bash
# After testing 1.2.2-rc.1, release stable version
./release.sh patch
# Result: 1.2.2-rc.1 → 1.2.2 (removes pre-release suffix)
```

#### Release with Auto-Publishing
```bash
./release.sh patch --publish-internal-testing
# Builds, tags, and publishes to Google Play Internal Testing
```

## Pre-Release Workflow

Pre-releases are used for internal testing before stable releases.

### Step 1: Create Pre-Release

```bash
# Start a new pre-release cycle
./release.sh patch --pre-release
# Creates: v1.2.2-rc.0
```

### Step 2: Test Pre-Release

1. The script creates an AAB at `app/build/outputs/bundle/release/app-release.aab`
2. Upload to Google Play Console → Internal Testing track
3. Share with internal testers
4. Collect feedback and make fixes

### Step 3: Iterate (if needed)

If bugs are found during testing:

```bash
# Make fixes, commit them, then create next RC
./release.sh patch --pre-release
# Creates: v1.2.2-rc.1
```

### Step 4: Graduate to Stable

When testing is complete and no issues found:

```bash
# Remove pre-release suffix
./release.sh patch
# Creates: v1.2.2 (stable)
```

### Step 5: Push to Remote

```bash
git push origin master
git push origin --tags
```

## Stable Release Workflow

For releases that don't need pre-release testing:

### Step 1: Ensure Clean Working Directory

```bash
git status
# Must show no uncommitted changes
```

### Step 2: Run Release Script

```bash
# Choose bump type based on changes
./release.sh patch    # For bug fixes
./release.sh minor    # For new features
./release.sh major    # For breaking changes
```

The script will:
1. ✅ Verify git working directory is clean
2. 📖 Read current version from `version.properties`
3. 🔢 Calculate new version and increment version code
4. ✅ Prompt for confirmation
5. 📝 Update `version.properties`
6. 💾 Commit version bump
7. 🏷️ Create git tag (e.g., `v1.2.2`)
8. 🏗️ Build signed AAB (`./gradlew clean bundleRelease`)
9. 📄 Generate `RELEASE_NOTES.txt`
10. 📄 Generate Play Store release notes

### Step 3: Review Build Artifacts

```bash
# Check AAB file
ls -lh app/build/outputs/bundle/release/app-release.aab

# Review release notes
cat RELEASE_NOTES.txt
```

### Step 4: Edit Release Notes (Optional)

```bash
nano RELEASE_NOTES.txt
# Edit the "PLAY STORE RELEASE NOTES" section
# Maximum 500 characters
```

### Step 5: Push to Remote

```bash
git push origin master
git push origin --tags
```

### Step 6: Upload to Google Play

#### Option A: Manual Upload

1. Go to [Google Play Console](https://play.google.com/console)
2. Select **PEV PlugShare**
3. Navigate to **Release** → **Testing** → **Internal testing**
4. Click **Create new release**
5. Upload `app/build/outputs/bundle/release/app-release.aab`
6. Copy release notes from `RELEASE_NOTES.txt` (Play Store section)
7. Review and save

#### Option B: Automatic Publishing

If `play-service-account.json` is configured:

```bash
./release.sh patch --publish-internal-testing
```

Or manually trigger publishing after build:

```bash
./gradlew publishReleaseBundle --track=internal
```

## Google Play Publishing

### Setup for Automatic Publishing

To enable automatic publishing to Google Play Internal Testing:

#### Step 1: Create Service Account

1. Go to [Google Play Console](https://play.google.com/console) → **API Access**
2. Click **Create new service account**
3. Follow link to Google Cloud Console
4. Create service account with name: `pev-plugshare-release-bot`
5. Grant permissions: **Service Account User**
6. Create and download JSON key

#### Step 2: Grant Play Console Access

1. Return to Play Console → **API Access**
2. Find your new service account
3. Click **Grant access**
4. Select **PEV PlugShare** app
5. Grant permissions:
   - **Release to testing tracks**: Yes
   - **Release to production**: No (optional)
   - **Release apps to testing tracks**: Yes
6. Save changes

#### Step 3: Configure Project

```bash
# Save JSON key to project root
cp ~/Downloads/service-account-key.json play-service-account.json

# Verify it's ignored by git
git check-ignore play-service-account.json
# Should output: play-service-account.json
```

#### Step 4: Add Gradle Play Publisher Plugin

The plugin is already configured in `app/build.gradle` from OSM-EUC-World reference.

If you need to add it manually:

```kotlin
// In build.gradle.kts (project level)
plugins {
    id("com.github.triplet.play") version "3.8.6" apply false
}

// In app/build.gradle.kts
plugins {
    id("com.github.triplet.play")
}

play {
    val serviceAccountFile = rootProject.file("play-service-account.json")
    if (serviceAccountFile.exists()) {
        serviceAccountCredentials.set(serviceAccountFile)
    }
    track.set("internal")
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
    defaultToAppBundles.set(true)
}
```

### Publishing Commands

```bash
# Publish to Internal Testing
./gradlew publishReleaseBundle --track=internal

# Publish to Alpha
./gradlew publishReleaseBundle --track=alpha

# Publish to Beta
./gradlew publishReleaseBundle --track=beta

# Publish to Production (requires approval)
./gradlew publishReleaseBundle --track=production
```

## Rollback Procedures

### If Build Fails

The `release.sh` script automatically rolls back on build failure:

1. Deletes the git tag
2. Resets git HEAD to previous commit
3. Exits with error

**Manual rollback** (if needed):

```bash
# Remove tag
git tag -d v1.2.2

# Reset to previous commit
git reset --hard HEAD~1

# If already pushed to remote
git push origin :refs/tags/v1.2.2  # Delete remote tag
git push origin master --force     # Force push reset (DANGEROUS)
```

### If Wrong Version Released

```bash
# Create a new corrected release
./release.sh patch [correct_version_code]
```

### If Pre-Release Should Be Abandoned

```bash
# Manually edit version.properties to remove VERSION_PRERELEASE
nano version.properties

# Create stable release
./release.sh patch
```

## Troubleshooting

### Issue: "Git working directory is not clean"

**Cause**: Uncommitted changes in working directory

**Solution**:
```bash
# Review changes
git status

# Commit or stash changes
git add .
git commit -m "Your commit message"

# Or stash them
git stash
```

### Issue: "AAB file not found"

**Cause**: Build failed but error wasn't caught

**Solution**:
```bash
# Manually run build
./gradlew clean bundleRelease

# Check for errors in output
# Fix any build issues, then retry
./release.sh patch
```

### Issue: "Publishing failed"

**Cause**: Service account not configured or lacks permissions

**Solution**:
1. Verify `play-service-account.json` exists
2. Check service account has "Release to testing tracks" permission
3. Manually upload AAB to Play Console

### Issue: Version already exists on Play Store

**Cause**: Version code already used in Play Console

**Solution**:
```bash
# Specify higher version code manually
./release.sh patch 999
```

### Issue: Release script won't run

**Cause**: Script not executable

**Solution**:
```bash
chmod +x release.sh
```

## Best Practices

### Before Every Release

- [ ] Run all tests: `./gradlew test`
- [ ] Run lint: `./gradlew lint`
- [ ] Test on physical device
- [ ] Verify OAuth flow works
- [ ] Check location permissions
- [ ] Test map functionality
- [ ] Review recent commits for security issues

### For Pre-Releases

- [ ] Always test with internal testers
- [ ] Create at least one RC before stable release
- [ ] Document known issues in RELEASE_NOTES.txt
- [ ] Test upgrade path from previous version

### For Stable Releases

- [ ] Update CHANGELOG.md with user-facing changes
- [ ] Update app screenshots if UI changed
- [ ] Notify users in release notes
- [ ] Monitor crash reports after release
- [ ] Be ready to hotfix if critical issues found

## Version History Example

```
v1.2.2-rc.0  → Pre-release created for testing
v1.2.2-rc.1  → Fixed bug found in rc.0
v1.2.2       → Stable release (graduated from rc.1)
v1.2.3       → Hotfix release
v1.3.0       → Minor release with new features
v2.0.0       → Major release with breaking changes
```

## Additional Resources

- [Semantic Versioning](https://semver.org/)
- [Android App Versioning](https://developer.android.com/studio/publish/versioning)
- [Google Play Console](https://play.google.com/console)
- [Gradle Play Publisher Plugin](https://github.com/Triple-T/gradle-play-publisher)

## Support

For questions or issues with the release process:
1. Check this documentation first
2. Review git history: `git log --oneline`
3. Check release tags: `git tag -l -n`
4. Contact: philg@a42r.com
