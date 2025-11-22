#!/usr/bin/env python3
"""
Test script for OAuth callback service
"""
import hmac
import hashlib
import time
import requests
import json
import sys


def generate_signature(provider: str, timestamp: int, secret: str) -> str:
    """Generate HMAC-SHA256 signature"""
    message = f"{provider.lower()}|{timestamp}"
    return hmac.new(
        secret.encode('utf-8'),
        message.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()


def test_service(base_url: str, secret: str, provider: str = "openrouter"):
    """Test the OAuth callback service"""

    print(f"🧪 Testing OAuth Callback Service")
    print(f"📍 Base URL: {base_url}")
    print(f"🔑 Provider: {provider}")
    print("-" * 60)

    # Test 1: Health check
    print("\n1️⃣  Testing health endpoint...")
    try:
        response = requests.get(f"{base_url}/health")
        print(f"   Status: {response.status_code}")
        print(f"   Response: {json.dumps(response.json(), indent=2)}")
    except Exception as e:
        print(f"   ❌ Error: {e}")
        return

    # Test 2: Register session
    print("\n2️⃣  Testing session registration...")
    timestamp = int(time.time())
    signature = generate_signature(provider, timestamp, secret)

    register_data = {
        "provider": provider,
        "timestamp": timestamp,
        "signature": signature
    }

    print(f"   Request: {json.dumps(register_data, indent=2)}")

    try:
        response = requests.post(
            f"{base_url}/oauth/register",
            json=register_data
        )
        print(f"   Status: {response.status_code}")

        if response.status_code == 200:
            result = response.json()
            print(f"   ✅ Success!")
            print(f"   Response: {json.dumps(result, indent=2)}")

            session_id = result.get("session_id")
            callback_url = result.get("callback_url")

            # Test 3: Simulate OAuth callback
            print("\n3️⃣  Testing callback endpoint...")
            test_code = "test_authorization_code_123"
            callback_test_url = f"{base_url}/oauth/callback/{session_id}?code={test_code}"

            print(f"   URL: {callback_test_url}")

            response = requests.get(callback_test_url)
            print(f"   Status: {response.status_code}")

            if response.status_code == 200:
                print(f"   ✅ Callback successful!")
                print(f"   Content-Type: {response.headers.get('content-type')}")
                if 'text/html' in response.headers.get('content-type', ''):
                    print(f"   ✅ HTML redirect page returned")
                    # Check for deep link in HTML
                    if f"?code={test_code}" in response.text:
                        print(f"   ✅ Deep link contains authorization code")
                    if f"provider={provider}" in response.text:
                        print(f"   ✅ Deep link contains provider name")
            else:
                print(f"   ❌ Failed: {response.text}")
        else:
            print(f"   ❌ Failed: {response.text}")

    except Exception as e:
        print(f"   ❌ Error: {e}")
        return

    # Test 4: Invalid signature
    print("\n4️⃣  Testing invalid signature...")
    bad_data = {
        "provider": provider,
        "timestamp": timestamp,
        "signature": "invalid_signature"
    }

    try:
        response = requests.post(
            f"{base_url}/oauth/register",
            json=bad_data
        )
        print(f"   Status: {response.status_code}")
        if response.status_code == 400:
            print(f"   ✅ Correctly rejected invalid signature")
            print(f"   Response: {json.dumps(response.json(), indent=2)}")
        else:
            print(f"   ⚠️  Unexpected status code")
    except Exception as e:
        print(f"   ❌ Error: {e}")

    # Test 5: Unsupported provider
    print("\n5️⃣  Testing unsupported provider...")
    unsupported_provider = "unsupported_provider"
    bad_timestamp = int(time.time())
    bad_signature = generate_signature(unsupported_provider, bad_timestamp, secret)

    bad_provider_data = {
        "provider": unsupported_provider,
        "timestamp": bad_timestamp,
        "signature": bad_signature
    }

    try:
        response = requests.post(
            f"{base_url}/oauth/register",
            json=bad_provider_data
        )
        print(f"   Status: {response.status_code}")
        if response.status_code == 400:
            print(f"   ✅ Correctly rejected unsupported provider")
            print(f"   Response: {json.dumps(response.json(), indent=2)}")
        else:
            print(f"   ⚠️  Unexpected status code")
    except Exception as e:
        print(f"   ❌ Error: {e}")

    print("\n" + "=" * 60)
    print("✅ Test suite complete!")


if __name__ == "__main__":
    # Default values
    BASE_URL = "http://localhost:8000"
    SECRET = "your-shared-secret"  # Update this
    PROVIDER = "openrouter"

    # Parse command line arguments
    if len(sys.argv) > 1:
        BASE_URL = sys.argv[1]
    if len(sys.argv) > 2:
        SECRET = sys.argv[2]
    if len(sys.argv) > 3:
        PROVIDER = sys.argv[3]

    print(f"""
╔══════════════════════════════════════════════════════════╗
║         OAuth Callback Service Test Suite               ║
╚══════════════════════════════════════════════════════════╝

Usage: python test_service.py [base_url] [secret] [provider]

Defaults:
  base_url: {BASE_URL}
  secret:   {SECRET}
  provider: {PROVIDER}

""")

    if SECRET == "your-shared-secret":
        print("⚠️  WARNING: Using default secret. Update in test script or pass as argument.")
        print()

    test_service(BASE_URL, SECRET, PROVIDER)
