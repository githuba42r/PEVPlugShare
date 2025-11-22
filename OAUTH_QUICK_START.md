# 🔐 OAuth Setup - Quick Start

## Before You Build

**REQUIRED**: Copy and configure secrets before building:

```bash
cp secrets.properties.template secrets.properties
nano secrets.properties  # Fill in your actual secrets
```

## What You Need

Edit `secrets.properties` with:

1. **Development callback URL** - Your dev server OAuth endpoint
2. **Development shared secret** - HMAC secret for dev builds
3. **Development deep link scheme** - Your app's deep link (e.g., `yourapp://oauth/callback`)
4. **Production callback URL** - Your production server OAuth endpoint
5. **Production shared secret** - HMAC secret for release builds  
6. **Production deep link scheme** - Production deep link

## Build

```bash
./gradlew assembleDebug    # Uses development secrets
./gradlew assembleRelease  # Uses production secrets
```

## Why?

OAuth secrets are **NO LONGER HARDCODED** in code. They're injected at build time from `secrets.properties` (which is gitignored).

## More Info

- **Complete Setup**: [OAUTH_SECRET_MANAGEMENT.md](OAUTH_SECRET_MANAGEMENT.md)
- **Security Details**: [OAUTH_SECURITY_ANALYSIS.md](OAUTH_SECURITY_ANALYSIS.md)
- **Implementation**: [OPENROUTER_OAUTH_IMPLEMENTATION.md](OPENROUTER_OAUTH_IMPLEMENTATION.md)

## Troubleshooting

**Build fails with "MISSING_SECRET"**:
- You forgot to create `secrets.properties`
- Run: `cp secrets.properties.template secrets.properties`
- Edit the file with your actual secrets

**Git shows secrets.properties**:
- It shouldn't! It's gitignored
- Check: `grep secrets.properties .gitignore`
- If tracked: `git rm --cached secrets.properties`
