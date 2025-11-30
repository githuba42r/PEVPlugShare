# Lambda Build Setup - Python Dependencies

## Problem

When running `bash scripts/deploy.sh`, you may encounter Python errors like:
- `ModuleNotFoundError: No module named 'boto3'`
- `pip: command not found`
- Path or permission issues with pip

## Solution

The build script has been **updated** to:

1. ✅ Use `python3` explicitly (not `python`)
2. ✅ Handle boto3 correctly (it's built into Lambda runtime)
3. ✅ Use absolute paths for better reliability
4. ✅ Provide better error messages
5. ✅ No virtual environment needed (automatic handling)

## What the Build Script Does

```bash
bash scripts/build.sh
```

This script:

1. **Checks Python 3 availability**
   - Verifies python3 is installed
   - Shows which Python version is being used

2. **Creates Lambda deployment packages**
   - Copies function code (handler.py)
   - Copies shared utilities (applications.py, security.py, etc.)
   - Filters out boto3 from requirements (Lambda includes it)
   - Creates .zip files ready for Lambda

3. **Output files**
   - `build/register.zip` - OAuth register function
   - `build/callback.zip` - OAuth callback function

## Prerequisites

You need:

```bash
# Python 3.8 or later
python3 --version

# pip for Python 3
pip3 --version

# Unix tools (standard on macOS/Linux)
zip
```

## Installation

### macOS
```bash
brew install python3
```

### Ubuntu/Debian
```bash
sudo apt-get install python3 python3-pip
```

### Windows
Download from https://www.python.org/ or use:
```bash
# Using Windows Package Manager
winget install Python.Python.3.11

# Or using Chocolatey
choco install python
```

## Running the Build

The `deploy.sh` script automatically calls `build.sh`:

```bash
cd oauth-callback-lambda
bash scripts/deploy.sh
```

Or manually:

```bash
bash scripts/build.sh
```

## Troubleshooting

### Error: "Python 3 not found"
```bash
# Check if Python 3 is installed
python3 --version

# If not installed, install it:
# macOS: brew install python3
# Linux: sudo apt-get install python3
# Windows: Download from python.org
```

### Error: "ModuleNotFoundError: No module named 'boto3'"
This is **expected** - boto3 is provided by Lambda runtime, not needed in package.
The script automatically filters it out.

### Error: "zip: command not found"
```bash
# macOS
brew install zip

# Ubuntu/Debian
sudo apt-get install zip

# Windows: Usually pre-installed, or use 7-Zip/WinRAR
```

### Error: "Permission denied"
```bash
# Make script executable
chmod +x scripts/build.sh
chmod +x scripts/deploy.sh

# Then run again
bash scripts/build.sh
```

## How Dependencies Work

### Lambda Runtime Inclusions

Lambda Python 3.12 runtime includes:
- ✅ boto3 (AWS SDK)
- ✅ botocore
- ✅ Python standard library

These are **NOT** packaged in your .zip file.

### Your Application Dependencies

For any additional dependencies:

1. Add to `src/register/requirements.txt` or `src/callback/requirements.txt`
2. Run `bash scripts/build.sh`
3. Dependencies are installed into the package

**Example:** Adding requests library

```bash
# Add to requirements.txt
echo "requests>=2.28.0" >> src/register/requirements.txt

# Build includes it
bash scripts/build.sh
```

## Virtual Environment (Optional)

You do **NOT** need to create a virtual environment for the build script.

The script handles everything:
- Uses system python3
- Installs dependencies into temporary directory
- Creates self-contained .zip packages

However, if you want to develop locally with dependencies:

```bash
# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install boto3

# Run your code
python3 src/register/handler.py

# Deactivate when done
deactivate
```

## Full Deploy Process

```bash
cd oauth-callback-lambda

# 1. Build Lambda packages (auto-called by deploy.sh)
bash scripts/build.sh
# Output: build/register.zip and build/callback.zip created

# 2. Configure Terraform
cd infrastructure
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your applications

# 3. Deploy to AWS
cd ..
bash scripts/deploy.sh
# Deploys Lambda functions with the packages
```

## Verification

After successful build:

```bash
# Check zip files were created
ls -lh build/*.zip

# Expected output:
# -rw-r--r-- ... 6.0K Nov 30 ... build/callback.zip
# -rw-r--r-- ... 6.0K Nov 30 ... build/register.zip

# Verify contents
unzip -l build/register.zip
# Should show: handler.py, shared/, etc.
```

## Support

If you encounter other errors:

1. **Check Python version**
   ```bash
   python3 --version  # Should be 3.8+
   ```

2. **Verify pip
**
   ```bash
   pip3 --version
   ```

3. **Check zip installation**
   ```bash
   which zip  # Should show path
   ```

4. **Run with verbose output**
   ```bash
   # Edit build.sh and remove `--quiet` flag
   python3 -m pip install ... -t "$TMP_DIR"  # Shows install details
   ```

5. **Check available disk space**
   ```bash
   df -h  # Should have at least 100MB free
   ```

