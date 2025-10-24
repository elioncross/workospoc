#!/bin/bash

echo "Starting WorkOS POC Frontend..."
echo "Installing dependencies..."

cd frontend

# Install dependencies
echo "Installing npm packages..."
npm install

if [ $? -eq 0 ]; then
    echo "Dependencies installed successfully."
    echo "Note: Some deprecation warnings are normal for Angular 15."
    echo "Starting Angular development server on http://localhost:4200..."
    npm start
else
    echo "Failed to install dependencies. Please check the errors above."
    exit 1
fi
