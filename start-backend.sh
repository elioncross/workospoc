#!/bin/bash

echo "Starting WorkOS POC Backend..."
echo "Setting WorkOS environment variables for staging..."

# WorkOS Environment Variables for Staging
export WORKOS_API_KEY='YOUR_WORKOS_API_KEY_HERE'
export WORKOS_CLIENT_ID='YOUR_WORKOS_CLIENT_ID_HERE'
export WORKOS_BASE_URL='https://api.workos.dev'

echo "✅ WorkOS environment variables set:"
echo "   WORKOS_API_KEY: ${WORKOS_API_KEY:0:10}..."
echo "   WORKOS_CLIENT_ID: $WORKOS_CLIENT_ID"
echo "   WORKOS_BASE_URL: $WORKOS_BASE_URL"
echo ""

echo "Building Spring Boot application..."

mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful. Starting application on port 8081..."
    mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
else
    echo "Build failed. Please check the errors above."
    exit 1
fi
