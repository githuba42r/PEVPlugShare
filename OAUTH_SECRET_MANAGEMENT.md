# OAuth Secret Management Setup Guide

## Overview

This project uses build-time secret injection to keep OAuth credentials secure. Secrets are **never** committed to git.

## How It Works

1. **secrets.properties** (local, gitignored) contains actual secret values
2. **Gradle** reads secrets.properties at build time
3. **CMake** receives secrets as compile definitions (`-D` flags)
4. **Native C++ code** uses these compile-time constants
5. **ProGuard/R8** obfuscates the compiled .so file

## Setup Instructions

### First Time Setup

1. Copy the template file:
   ```bash
   cp secrets.properties.template secrets.properties
   ```

2. Edit `secrets.properties` with your actual secrets:
   ```properties
   # Development secrets
   oauth.dev.callback.url=https://pevplugshare.a42r.com
   oauth.dev.shared.secret=your-dev-secret-here
   oauth.dev.deep.link.scheme=pevplugshare://oauth/callback

   # Production secrets
   oauth.prod.callback.url=https://pevplugshare-prod.a42r.com
   oauth.prod.shared.secret=your-prod-secret-here
   oauth.prod.deep.link.scheme=pevplugshare://oauth/callback
   ```

3. Build the app:
   ```bash
   ./gradlew assembleDebug    # Uses dev secrets
   ./gradlew assembleRelease  # Uses prod secrets
   ```

### Important Files

#### Files NOT in Git (Gitignored)
- `secrets.properties` - Your actual secrets (NEVER commit!)

#### Files in Git (Safe to Commit)
- `secrets.properties.template` - Template showing required keys
- `app/src/main/cpp/native-secrets.cpp` - Uses compile-time constants
- `app/src/main/cpp/CMakeLists.txt` - Receives secrets from Gradle
- `app/build.gradle.kts` - Reads secrets.properties and passes to CMake

## Build Process Flow

```
secrets.properties (NOT in git)
         ↓
app/build.gradle.kts reads file
         ↓
Gradle passes to CMake via -D flags
         ↓
CMake defines constants for C++
         ↓
native-secrets.cpp uses constants
         ↓
Compiled into libpevplugshare.so
         ↓
ProGuard/R8 obfuscates
         ↓
Final APK (secrets embedded)
```

## Security Model

### Defense in Depth

1. **Not in Git**: secrets.properties is gitignored
2. **Not in DEX**: Secrets are in native .so file (not Java/Kotlin)
3. **Obfuscated**: ProGuard/R8 shrinks and obfuscates code
4. **Stripped**: Debug symbols removed from release builds
5. **HMAC Signed**: All OAuth requests use HMAC-SHA256 signatures

### What This Protects Against

✅ Accidentally committing secrets to git  
✅ Simple APK decompilation (jadx, apktool)  
✅ BuildConfig secret extraction  
✅ Automated secret scanners  

### What This Does NOT Protect Against

❌ Dedicated reverse engineering (IDA Pro, Ghidra)  
❌ Dynamic analysis (Frida, hooking)  
❌ Root/jailbroken devices with memory dumps  

**Note**: No client-side secret is 100% secure. This is defense in depth - making it harder to extract secrets, but not impossible.

## Troubleshooting

### Build fails with "MISSING_SECRET"

**Problem**: secrets.properties not found or missing keys

**Solution**:
```bash
# Check if file exists
ls -la secrets.properties

# If not, copy template
cp secrets.properties.template secrets.properties

# Edit with your actual secrets
nano secrets.properties  # or your preferred editor
```

### Secrets appear in git status

**Problem**: secrets.properties is being tracked by git

**Solution**:
```bash
# Remove from git tracking (keeps local file)
git rm --cached secrets.properties

# Verify it's in .gitignore
grep secrets.properties .gitignore

# Should see:
# secrets.properties
```

### Wrong secrets in debug build

**Problem**: Build is using production secrets instead of dev

**Solution**: 
- Debug builds use `oauth.dev.*` properties
- Release builds use `oauth.prod.*` properties
- Check that you have both sets defined in secrets.properties

## Production Deployment

### For Production Builds

1. **Update production secrets** in secrets.properties:
   ```properties
   oauth.prod.callback.url=https://your-production-server.com
   oauth.prod.shared.secret=generate-strong-random-secret-here
   oauth.prod.deep.link.scheme=yourapp://oauth/callback
   ```

2. **Generate production secret** (example):
   ```bash
   openssl rand -base64 32
   # or
   python3 -c "import secrets; print(secrets.token_urlsafe(32))"
   ```

3. **Build release APK**:
   ```bash
   ./gradlew assembleRelease
   ```

4. **Update callback service** with matching secret:
   - Deploy callback service with same `oauth.prod.shared.secret`
   - Ensure callback URL matches `oauth.prod.callback.url`

### CI/CD Integration

For GitHub Actions, Jenkins, etc:

```yaml
# GitHub Actions example
- name: Create secrets.properties
  run: |
    cat > secrets.properties << EOF
    oauth.dev.callback.url=${{ secrets.OAUTH_DEV_CALLBACK_URL }}
    oauth.dev.shared.secret=${{ secrets.OAUTH_DEV_SECRET }}
    oauth.dev.deep.link.scheme=${{ secrets.OAUTH_DEV_SCHEME }}
    oauth.prod.callback.url=${{ secrets.OAUTH_PROD_CALLBACK_URL }}
    oauth.prod.shared.secret=${{ secrets.OAUTH_PROD_SECRET }}
    oauth.prod.deep.link.scheme=${{ secrets.OAUTH_PROD_SCHEME }}
    EOF
```

## Team Collaboration

### Sharing Development Secrets

**Option 1**: Secure password manager (1Password, Bitwarden)
- Create shared vault for team
- Store secrets.properties content

**Option 2**: Encrypted file in private repository
- Create separate private repo for secrets
- Encrypt with GPG
- Share decryption key via secure channel

**Option 3**: Environment-specific secrets
- Each developer has their own dev callback URL
- Personal development secrets
- Shared production secrets only for release builds

### Onboarding New Developers

1. Send them `secrets.properties.template`
2. Provide development secret values via secure channel
3. They create local `secrets.properties`
4. Never commit their secrets.properties

## Advanced: Runtime Secret Management

For maximum security (not currently implemented):

1. **Server-side key exchange**: Client requests encryption key from server
2. **Device-specific encryption**: Secrets encrypted with device hardware key
3. **Time-limited tokens**: Secrets expire and require renewal
4. **Remote configuration**: Firebase Remote Config, AWS AppConfig

These approaches move the security boundary to the server, where secrets can be rotated and revoked.

## Quick Reference

```bash
# Setup (first time)
cp secrets.properties.template secrets.properties
nano secrets.properties  # Fill in secrets

# Build
./gradlew assembleDebug    # Dev secrets
./gradlew assembleRelease  # Prod secrets

# Verify secrets not in git
git status  # Should NOT show secrets.properties
git diff    # Should NOT show actual secret values

# Generate random secret
openssl rand -base64 32
```

## Summary

✅ **DO**: Keep secrets.properties local (gitignored)  
✅ **DO**: Use secrets.properties.template for documentation  
✅ **DO**: Generate strong random secrets for production  
✅ **DO**: Update callback service with matching secrets  

❌ **DON'T**: Commit secrets.properties to git  
❌ **DON'T**: Hardcode secrets in source code  
❌ **DON'T**: Share secrets in plain text (Slack, email)  
❌ **DON'T**: Reuse the same secret for dev and prod  
