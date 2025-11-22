#!/usr/bin/env python3
"""
Test script for local Lambda development environment
"""
import requests
import hmac
import hashlib
import time
import json

BASE_URL = "http://localhost:3000"
SECRET = "test-secret-change-in-production"


def generate_signature(provider: str, timestamp: int) -> str:
    """Generate HMAC-SHA256 signature"""
    message = f"{provider.lower()}|{timestamp}"
    return hmac.new(
        SECRET.encode('utf-8'),
        message.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()


def test_local_deployment():
    """Test the local Docker deployment"""
    print("🧪 Testing Local OAuth Lambda Deployment")
    print("=" * 60)

    # Test 1: Health check
    print("\n1️⃣  Testing health endpoint...")
    try:
        response = requests.get(f"{BASE_URL}/health")
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            print("   ✅ Health check passed")
        else:
            print("   ❌ Health check failed")
    except Exception as e:
        print(f"   ❌ Error: {e}")
        return

    # Test 2: Register session
    print("\n2️⃣  Testing session registration...")
    timestamp = int(time.time())
    signature = generate_signature("openrouter", timestamp)

    payload = {
        "provider": "openrouter",
        "timestamp": timestamp,
        "signature": signature
    }

    print(f"   Request: {json.dumps(payload, indent=2)}")

    try:
        response = requests.post(
            f"{BASE_URL}/oauth/register",
            json=payload,
            headers={"Content-Type": "application/json"}
        )
        print(f"   Status: {response.status_code}")

        if response.status_code == 200:
            result = response.json()
            print(f"   ✅ Registration successful!")
            print(f"   Response: {json.dumps(result, indent=2)}")

            session_id = result.get('session_id')
            callback_url = result.get('callback_url')

            # Test 3: Callback endpoint
            print("\n3️⃣  Testing callback endpoint...")
            test_code = "test_auth_code_123"
            callback_test_url = f"{BASE_URL}/oauth/callback/{session_id}?code={test_code}"

            print(f"   URL: {callback_test_url}")

            response = requests.get(callback_test_url)
            print(f"   Status: {response.status_code}")

            if response.status_code == 200:
                print("   ✅ Callback successful!")
                if 'text/html' in response.headers.get('content-type', ''):
                    print("   ✅ HTML redirect page returned")
                    if f"?code={test_code}" in response.text:
                        print("   ✅ Deep link contains authorization code")
            else:
                print(f"   ❌ Failed: {response.text}")
        else:
            print(f"   ❌ Failed: {response.text}")
    except Exception as e:
        print(f"   ❌ Error: {e}")

    print("\n" + "=" * 60)
    print("✅ Local test suite complete!")
    print("\n📊 Access DynamoDB Admin: http://localhost:8001")
    print("📡 LocalStack Dashboard: http://localhost:4566")


if __name__ == "__main__":
    test_local_deployment()
