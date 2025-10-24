#!/bin/bash

echo "Starting WorkOS POC Backend..."
echo "Loading environment variables from .env file..."

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    echo "📄 Loading .env file..."
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Environment variables loaded from .env"
else
    echo "⚠️  No .env file found. Please create one from .env.example"
    echo "   cp .env.example .env"
    echo "   Then edit .env with your actual WorkOS credentials"
    exit 1
fi

echo "✅ WorkOS environment variables:"
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
