#!/bin/bash

echo "Starting WorkOS POC Backend..."
echo "Loading configuration from environment variables..."
echo ""

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    echo "Loading .env file..."
    export $(cat .env | grep -v '^#' | xargs)
fi

# Set WorkOS environment variables (can be overridden by .env file)
# Note: These must be set in .env file or exported before running
# Defaults are placeholders - you must provide actual values
export WORKOS_API_KEY="${WORKOS_API_KEY:-your-workos-api-key-here}"
export WORKOS_CLIENT_ID="${WORKOS_CLIENT_ID:-your-workos-client-id-here}"
export WORKOS_SESSION_PASSWORD="${WORKOS_SESSION_PASSWORD:-your-workos-session-password-here}"

# Note: WORKOS_CONNECTION_ID and WORKOS_ORGANIZATION_ID are no longer needed
# - Connection IDs are mapped in application.yml (workos.connection-mapping)
# - Organization ID is provided by WorkOS in the profile during IdP-initiated flow

# Validate that required environment variables are set
if [ "$WORKOS_API_KEY" = "your-workos-api-key-here" ] || [ -z "$WORKOS_API_KEY" ]; then
    echo "ERROR: WORKOS_API_KEY is not set!"
    echo "Please create a .env file or export WORKOS_API_KEY before running this script."
    echo "See .env.example for reference."
    exit 1
fi

echo "Building Spring Boot application..."

mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful. Starting application on port 8081..."
    echo "Configuration:"
    echo "  Environment: staging"
    echo "  API Key: ${WORKOS_API_KEY:0:15}..."
    echo "  Client ID: ${WORKOS_CLIENT_ID}"
    echo "  Connection mappings: configured in application.yml"
    echo ""
    mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
else
    echo "Build failed. Please check the errors above."
    exit 1
fi
