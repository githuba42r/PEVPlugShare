#!/usr/bin/env python3
"""
Test diagnostic endpoints
"""
import requests
import time

BASE_URL = "http://localhost:8000"


def test_diagnostic_endpoints_disabled():
    """Test that diagnostic endpoints return 404 when disabled"""
    print("🧪 Testing diagnostic endpoints (DISABLED)...")

    # Should return 404 when ENABLE_DIAGNOSTIC_ENDPOINTS=false
    response = requests.get(f"{BASE_URL}/ping")
    assert response.status_code == 404, f"Expected 404, got {response.status_code}"
    print("  ✅ /ping returns 404 when disabled")

    response = requests.get(f"{BASE_URL}/version")
    assert response.status_code == 404, f"Expected 404, got {response.status_code}"
    print("  ✅ /version returns 404 when disabled")


def test_diagnostic_endpoints_enabled():
    """Test that diagnostic endpoints work when enabled"""
    print("\n🧪 Testing diagnostic endpoints (ENABLED)...")
    print("  ⚠️  Set ENABLE_DIAGNOSTIC_ENDPOINTS=true in .env to run this test")

    # Test /ping
    response = requests.get(f"{BASE_URL}/ping")
    if response.status_code == 404:
        print("  ⏭️  Skipping - endpoints not enabled")
        return

    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    data = response.json()
    assert data["status"] == "pong"
    assert "timestamp" in data
    assert "server_time" in data
    print("  ✅ /ping returns correct response")

    # Test /version
    response = requests.get(f"{BASE_URL}/version")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    data = response.json()
    assert data["service"] == "OAuth Callback Service"
    assert data["version"] == "1.0.0"
    assert "python_version" in data
    assert "platform" in data
    assert "uptime_seconds" in data
    assert data["diagnostic_endpoints_enabled"] == True
    assert "rate_limits" in data
    print("  ✅ /version returns correct response")


def test_rate_limiting():
    """Test rate limiting on diagnostic endpoints"""
    print("\n🧪 Testing diagnostic endpoint rate limiting...")
    print("  ⚠️  Set ENABLE_DIAGNOSTIC_ENDPOINTS=true in .env to run this test")

    # Check if enabled first
    response = requests.get(f"{BASE_URL}/ping")
    if response.status_code == 404:
        print("  ⏭️  Skipping - endpoints not enabled")
        return

    # Make 5 requests (should succeed)
    for i in range(5):
        response = requests.get(f"{BASE_URL}/ping")
        assert response.status_code == 200, f"Request {i+1} failed with {response.status_code}"
    print("  ✅ First 5 requests succeeded")

    # 6th request should be rate limited
    response = requests.get(f"{BASE_URL}/ping")
    assert response.status_code == 429, f"Expected 429, got {response.status_code}"
    data = response.json()
    assert "Rate limit exceeded" in data["detail"]
    print("  ✅ 6th request rate limited (429)")

    # Wait 61 seconds and try again
    print("  ⏳ Waiting 61 seconds for rate limit to reset...")
    time.sleep(61)

    response = requests.get(f"{BASE_URL}/ping")
    assert response.status_code == 200, f"Expected 200 after reset, got {response.status_code}"
    print("  ✅ Request succeeds after rate limit window")


def test_health_endpoint():
    """Test that health endpoint still works (always enabled)"""
    print("\n🧪 Testing /health endpoint (always enabled)...")

    response = requests.get(f"{BASE_URL}/health")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    data = response.json()
    assert data["status"] == "healthy"
    assert data["version"] == "1.0.0"
    assert "active_sessions" in data
    print("  ✅ /health endpoint works correctly")


if __name__ == "__main__":
    print("=" * 60)
    print("Diagnostic Endpoints Test Suite")
    print("=" * 60)

    try:
        test_health_endpoint()
        test_diagnostic_endpoints_disabled()
        test_diagnostic_endpoints_enabled()
        test_rate_limiting()

        print("\n" + "=" * 60)
        print("✅ All tests passed!")
        print("=" * 60)
    except AssertionError as e:
        print(f"\n❌ Test failed: {e}")
        exit(1)
    except requests.exceptions.ConnectionError:
        print(f"\n❌ Could not connect to {BASE_URL}")
        print("Make sure the service is running:")
        print("  docker-compose up -d")
        print("  OR")
        print("  uvicorn app.main:app --reload")
        exit(1)
