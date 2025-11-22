"""
Rate limiting utilities
"""
from collections import defaultdict
from datetime import datetime, timedelta
from typing import Dict, Tuple
import threading


class RateLimiter:
    """Simple in-memory rate limiter with minute/hour/day buckets"""

    def __init__(self, per_minute: int, per_hour: int, per_day: int):
        self.per_minute = per_minute
        self.per_hour = per_hour
        self.per_day = per_day

        # Storage: {ip: {bucket: [timestamps]}}
        self.requests: Dict[str, Dict[str, list]] = defaultdict(lambda: {
            'minute': [],
            'hour': [],
            'day': []
        })
        self.lock = threading.Lock()

    def _cleanup_old_requests(self, ip: str):
        """Remove timestamps outside the tracking window"""
        now = datetime.utcnow()

        # Clean up minute bucket (keep last 60 seconds)
        cutoff_minute = now - timedelta(seconds=60)
        self.requests[ip]['minute'] = [
            ts for ts in self.requests[ip]['minute'] if ts > cutoff_minute
        ]

        # Clean up hour bucket (keep last 60 minutes)
        cutoff_hour = now - timedelta(hours=1)
        self.requests[ip]['hour'] = [
            ts for ts in self.requests[ip]['hour'] if ts > cutoff_hour
        ]

        # Clean up day bucket (keep last 24 hours)
        cutoff_day = now - timedelta(days=1)
        self.requests[ip]['day'] = [
            ts for ts in self.requests[ip]['day'] if ts > cutoff_day
        ]

    def is_allowed(self, ip: str) -> Tuple[bool, str]:
        """
        Check if request from IP is allowed

        Returns:
            Tuple of (allowed, error_message)
        """
        with self.lock:
            self._cleanup_old_requests(ip)

            # Check limits
            minute_count = len(self.requests[ip]['minute'])
            hour_count = len(self.requests[ip]['hour'])
            day_count = len(self.requests[ip]['day'])

            if minute_count >= self.per_minute:
                return False, f"Rate limit exceeded: {self.per_minute} requests per minute"

            if hour_count >= self.per_hour:
                return False, f"Rate limit exceeded: {self.per_hour} requests per hour"

            if day_count >= self.per_day:
                return False, f"Rate limit exceeded: {self.per_day} requests per day"

            # Record this request
            now = datetime.utcnow()
            self.requests[ip]['minute'].append(now)
            self.requests[ip]['hour'].append(now)
            self.requests[ip]['day'].append(now)

            return True, ""

    def get_stats(self, ip: str) -> Dict[str, int]:
        """Get current request counts for an IP"""
        with self.lock:
            self._cleanup_old_requests(ip)
            return {
                'minute': len(self.requests[ip]['minute']),
                'hour': len(self.requests[ip]['hour']),
                'day': len(self.requests[ip]['day']),
                'limits': {
                    'minute': self.per_minute,
                    'hour': self.per_hour,
                    'day': self.per_day
                }
            }
