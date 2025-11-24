# OAuth Implementation - Documentation Update Notice

## ⚠️ IMPORTANT: Secret Management Has Changed

The OAuth implementation documentation in `OPENROUTER_OAUTH_IMPLEMENTATION.md` contains **outdated examples** showing hardcoded secrets in BuildConfig.

**These examples are for reference only and no longer reflect the actual implementation.**

## Current Implementation (Secure)

OAuth secrets are now managed via:

1. **secrets.properties** (gitignored, not committed)
2. **Build-time injection** via Gradle → CMake → Native C++
3. **Native code storage** in `libpevplugshare.so`

## Setup Instructions

See [OAUTH_SECRET_MANAGEMENT.md](OAUTH_SECRET_MANAGEMENT.md) for:
- ✅ How to configure secrets securely
- ✅ Complete setup guide
- ✅ Security model explanation
- ✅ Troubleshooting

## Quick Start

```bash
# 1. Copy template
cp secrets.properties.template secrets.properties

# 2. Edit with your secrets
nano secrets.properties

# 3. Build
./gradlew assembleDebug
```

## What Changed

### Before (INSECURE - shown in old docs)
```kotlin
// ❌ DON'T DO THIS
buildConfigField("String", "OAUTH_SHARED_SECRET", "\"hardcoded-secret\"")
```

### After (SECURE - current implementation)
```properties
# secrets.properties (NOT in git)
oauth.dev.shared.secret=your-secret-here
```

Secrets are read from `secrets.properties` and injected at build time into native code.

## File Inventory

| File | In Git? | Contains Secrets? | Purpose |
|------|---------|-------------------|---------|
| `secrets.properties` | ❌ No (gitignored) | ✅ Yes | Actual secrets (local only) |
| `secrets.properties.template` | ✅ Yes | ❌ No | Template with placeholders |
| `native-secrets.cpp` | ✅ Yes | ❌ No | Uses compile-time injection |
| `OAUTH_SECRET_MANAGEMENT.md` | ✅ Yes | ❌ No | Setup documentation |

## TODO: Update Documentation

The following files still contain **example** hardcoded secrets for reference:
- `OPENROUTER_OAUTH_IMPLEMENTATION.md` (lines ~30-40)

These are **historical examples only** and do not reflect actual secrets or current implementation.

## Verification

To verify no real secrets are committed:

```bash
# Should find NO matches
git grep "9CsxVnJm"

# secrets.properties should be gitignored
git status | grep secrets.properties
# (should show nothing)
```
