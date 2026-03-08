#!/bin/bash

# OAuth Callback Service - Version Manager
# Manages semantic versioning with git integration
# Usage: ./bump-version.sh [major|minor|patch|prerelease] [--no-commit]

set -e  # Exit on error

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
VERSION_FILE="version.json"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Show usage
show_usage() {
    echo "Usage: $0 <bump-type> [OPTIONS]"
    echo ""
    echo "Bump semantic version and update version.json"
    echo ""
    echo "Bump Types:"
    echo "  major        Increment major version (x.0.0)"
    echo "  minor        Increment minor version (0.x.0)"
    echo "  patch        Increment patch version (0.0.x)"
    echo "  prerelease   Add/increment prerelease (0.0.1-alpha.x)"
    echo ""
    echo "Options:"
    echo "  --no-commit  Don't create git commit after bump"
    echo "  --no-tag     Don't create git tag after bump"
    echo "  -h, --help   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 patch                    # 1.0.0 -> 1.0.1 (with commit & tag)"
    echo "  $0 minor                    # 1.0.1 -> 1.1.0"
    echo "  $0 major                    # 1.1.0 -> 2.0.0"
    echo "  $0 prerelease               # 1.0.0 -> 1.0.1-alpha.0"
    echo "  $0 patch --no-commit        # Bump but don't commit"
    echo ""
}

# Parse arguments
BUMP_TYPE=""
NO_COMMIT=false
NO_TAG=false

while [[ $# -gt 0 ]]; do
    case $1 in
        major|minor|patch|prerelease)
            BUMP_TYPE="$1"
            shift
            ;;
        --no-commit)
            NO_COMMIT=true
            shift
            ;;
        --no-tag)
            NO_TAG=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown argument: $1${NC}"
            show_usage
            exit 1
            ;;
    esac
done

# Validate bump type
if [ -z "$BUMP_TYPE" ]; then
    echo -e "${RED}Error: Bump type is required${NC}"
    show_usage
    exit 1
fi

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}OAuth Service Version Manager${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Change to script directory (oauth-callback-service)
cd "$SCRIPT_DIR" || {
    echo -e "${RED}Error: Could not change to script directory${NC}"
    exit 1
}

echo -e "${BLUE}Working directory: $(pwd)${NC}"
echo ""

# Check if version.json exists
if [ ! -f "$VERSION_FILE" ]; then
    echo -e "${RED}Error: $VERSION_FILE not found${NC}"
    echo "Creating initial version file..."
    echo '{"version": "0.1.0", "build_date": "", "git_commit": "", "git_branch": ""}' > "$VERSION_FILE"
fi

# Check for uncommitted changes
if [ "$NO_COMMIT" = false ]; then
    echo -e "${BLUE}Checking for uncommitted changes...${NC}"
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        echo -e "${YELLOW}Warning: Not in a git repository${NC}"
    else
        # Check for changes in oauth-callback-service directory
        if ! git diff --quiet HEAD -- . || ! git diff --cached --quiet HEAD -- .; then
            echo -e "${RED}Error: There are uncommitted changes in oauth-callback-service/${NC}"
            echo ""
            echo "Uncommitted changes:"
            git status --short -- .
            echo ""
            echo "Please commit or stash your changes before bumping version."
            echo "Or use --no-commit to skip git operations."
            exit 1
        fi
        echo -e "${GREEN}✓ No uncommitted changes${NC}"
    fi
fi

# Read current version
CURRENT_VERSION=$(python3 -c "import json; print(json.load(open('$VERSION_FILE'))['version'])")
echo -e "${BLUE}Current version: ${CURRENT_VERSION}${NC}"

# Bump version using Python
NEW_VERSION=$(python3 << EOF
import json
import sys
from datetime import datetime

def bump_version(version, bump_type):
    # Parse version (handle prerelease)
    if '-' in version:
        base_version, prerelease = version.split('-', 1)
    else:
        base_version = version
        prerelease = None
    
    # Parse semantic version
    parts = base_version.split('.')
    if len(parts) != 3:
        print(f"Error: Invalid version format: {version}", file=sys.stderr)
        sys.exit(1)
    
    try:
        major, minor, patch = map(int, parts)
    except ValueError:
        print(f"Error: Invalid version numbers in: {version}", file=sys.stderr)
        sys.exit(1)
    
    # Bump according to type
    if bump_type == 'major':
        major += 1
        minor = 0
        patch = 0
        prerelease = None
    elif bump_type == 'minor':
        minor += 1
        patch = 0
        prerelease = None
    elif bump_type == 'patch':
        patch += 1
        prerelease = None
    elif bump_type == 'prerelease':
        if prerelease:
            # Increment existing prerelease
            if '.' in prerelease:
                pre_name, pre_num = prerelease.rsplit('.', 1)
                try:
                    pre_num = int(pre_num) + 1
                    prerelease = f"{pre_name}.{pre_num}"
                except ValueError:
                    prerelease = f"{prerelease}.1"
            else:
                prerelease = f"{prerelease}.1"
        else:
            # Create new prerelease
            patch += 1
            prerelease = "alpha.0"
    
    # Build new version string
    new_version = f"{major}.{minor}.{patch}"
    if prerelease:
        new_version += f"-{prerelease}"
    
    return new_version

# Load version file
with open('$VERSION_FILE', 'r') as f:
    data = json.load(f)

# Bump version
new_version = bump_version('$CURRENT_VERSION', '$BUMP_TYPE')
print(new_version)
EOF
)

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to bump version${NC}"
    exit 1
fi

echo -e "${GREEN}New version: ${NEW_VERSION}${NC}"
echo ""

# Update version.json
echo -e "${BLUE}Updating $VERSION_FILE...${NC}"
python3 << EOF
import json
from datetime import datetime

with open('$VERSION_FILE', 'r') as f:
    data = json.load(f)

data['version'] = '$NEW_VERSION'
# Clear build-specific metadata - will be set during build
data['build_date'] = ''
data['git_commit'] = ''
data['git_branch'] = ''

with open('$VERSION_FILE', 'w') as f:
    json.dump(data, f, indent=2)
    f.write('\n')

print(f"✓ Updated version to {data['version']}")
print(f"✓ Build metadata cleared (will be set during build)")
EOF

echo ""

# Git operations
if [ "$NO_COMMIT" = false ] && git rev-parse --git-dir > /dev/null 2>&1; then
    echo -e "${BLUE}Creating git commit...${NC}"
    git add "$VERSION_FILE"
    git commit -m "chore: bump version to $NEW_VERSION"
    echo -e "${GREEN}✓ Created commit${NC}"
    
    if [ "$NO_TAG" = false ]; then
        echo -e "${BLUE}Creating git tag...${NC}"
        TAG_NAME="oauth-service/v$NEW_VERSION"
        git tag -a "$TAG_NAME" -m "OAuth Callback Service - Release version $NEW_VERSION"
        echo -e "${GREEN}✓ Created tag $TAG_NAME${NC}"
        echo ""
        echo -e "${YELLOW}Don't forget to push the tag:${NC}"
        echo -e "  git push origin $TAG_NAME"
    fi
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Version Bump Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo -e "${BLUE}Version: ${CURRENT_VERSION} → ${NEW_VERSION}${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "  1. Build new image: ./build.sh --push v$NEW_VERSION"
echo -e "  2. Deploy updated service"
if [ "$NO_COMMIT" = false ] && [ "$NO_TAG" = false ]; then
    echo -e "  3. Push changes: git push && git push origin oauth-service/v$NEW_VERSION"
fi
echo ""
echo -e "${BLUE}Note:${NC} Git tag uses 'oauth-service/v$NEW_VERSION' prefix to distinguish"
echo -e "      from PEVPlugShare app versions."
echo ""
