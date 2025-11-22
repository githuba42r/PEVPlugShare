#!/bin/bash
# Initialize LocalStack DynamoDB table

set -e

echo "Waiting for LocalStack to be ready..."
until curl -s http://localhost:4566/_localstack/health | grep -q "\"dynamodb\": \"available\""; do
    sleep 1
done

echo "Creating DynamoDB table..."
aws dynamodb create-table \
    --table-name oauth-sessions \
    --attribute-definitions \
        AttributeName=session_id,AttributeType=S \
    --key-schema \
        AttributeName=session_id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --endpoint-url http://localhost:4566 \
    --region us-east-1

echo "Enabling TTL..."
aws dynamodb update-time-to-live \
    --table-name oauth-sessions \
    --time-to-live-specification "Enabled=true, AttributeName=expires_at" \
    --endpoint-url http://localhost:4566 \
    --region us-east-1

echo "✓ DynamoDB table created and configured"
