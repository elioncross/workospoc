# Setup & Testing Guide

## Prerequisites
- Java 11+, Maven 3.6+, Node.js 16+
- WorkOS account (free tier available)

## Quick Start

```bash
# Start both services
./start-backend.sh    # Backend on :8081
./start-frontend.sh   # Frontend on :4200
```

## Test Authentication

### Option 1: WorkOS SSO (Recommended)
1. Open http://localhost:4200
2. Click **"Login with SSO"**
3. Complete WorkOS Test Identity Provider form
4. ✅ Land on dashboard as "STAGING FALLBACK" user

### Option 2: Internal Login  
1. Open http://localhost:4200
2. Use: `admin`/`password` (or `manager`, `user`, `support`)
3. ✅ Test role-based access in dashboard

## Current Configuration

**Environment**: Staging with WorkOS Test IdP  
**SSO Flow**: `organization=org_test_idp`  
**Fallback User**: staging-fallback@example.com (MC role)

Environment variables (auto-set by start-backend.sh):
```bash
WORKOS_API_KEY=sk_test_a2V5...
WORKOS_CLIENT_ID=client_01K11PQC0JVV2WGA8EDPBVGB52  
WORKOS_BASE_URL=https://api.workos.dev
```

## Production Deployment

To move to production with real SAML:

1. **Get real WorkOS connection** (Okta, Azure AD, Google)
2. **Update application.yml**:
   ```yaml
   workos:
     environment: production
     api-key: sk_live_your_production_key
     connection-id: your_real_connection_id
   ```

## Troubleshooting

**Backend won't start**: `lsof -ti:8081 | xargs kill -9`  
**SSO fails**: Check logs for WorkOS environment variables  
**Frontend issues**: `cd frontend && npm install`

## Demo Users
- `admin`/`password` → SMA (Super Admin)
- `manager`/`password` → MA (Manager)  
- `user`/`password` → MC (Member)
- `support`/`password` → SU (Support)

---
**Status: ✅ FULLY FUNCTIONAL**  
Test IdP + Staging fallback working perfectly!
- **Node.js version**: Use Node.js 16+
- **Maven issues**: Try `mvn clean install -U`

## Full Documentation
📖 See [DOCUMENTATION.md](DOCUMENTATION.md) for comprehensive setup and configuration details.