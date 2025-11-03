#!/bin/bash

echo "Starting WorkOS POC Backend..."
echo "Using direct configuration from application.yml (staging environment)"
echo ""

echo "Building Spring Boot application..."

mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful. Starting application on port 8081..."
    echo "Configuration loaded from application.yml:"
    echo "  Environment: staging"
    echo "  API Key: sk_test_..."
    echo "  Client ID: client_01K11PQC0JVV2WGA8EDPBVGB52"
    echo "  Organization ID: org_01K8R9B8H3789HWZ7ZBK5VVE9W"
    echo ""
    mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
else
    echo "Build failed. Please check the errors above."
    exit 1
fi
