"""
Logging filters for uvicorn access logs
"""
import logging


class HealthCheckFilter(logging.Filter):
    """Filter out health check endpoint requests from access logs"""
    
    def filter(self, record: logging.LogRecord) -> bool:
        """
        Return False to filter out the log record, True to keep it
        """
        # Filter out GET /health requests
        message = record.getMessage()
        return '/health' not in message or 'GET /health' not in message
