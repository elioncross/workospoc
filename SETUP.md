# Setup & Testing Guide

## Prerequisites
- Java 11+, Maven 3.6+, Node.js 16+
- WorkOS account (staging or production)
- Okta or Azure Entra ID account (for SSO testing)

## Quick Start

```bash
# Start both services
./start-backend.sh    # Backend on :8081
./start-frontend.sh   # Frontend on :4200
```

## Environment Variables

Create a `.env` file in the project root:

```bash
WORKOS_API_KEY=sk_test_...
WORKOS_CLIENT_ID=client_...
WORKOS_SESSION_PASSWORD=...
```

The `start-backend.sh` script will automatically load these variables.

## Test Authentication

### Option 1: IdP-Initiated SSO (Recommended)

**Okta SSO:**
1. Log into your Okta dashboard
2. Click on the WorkOS SSO application
3. User is redirected to WorkOS → then to your app
4. ✅ Land on dashboard with customer context (CORP_PROD_001)

**Azure Entra ID SSO:**
1. Log into your Azure Entra ID dashboard
2. Click on the WorkOS SSO application
3. User is redirected to WorkOS → then to your app
4. ✅ Land on dashboard with customer context (CORP_PROD_002)

### Option 2: Internal Login  
1. Open http://localhost:4200
2. Use one of these credentials:
   - `org_super` / `password` (Sentinel Master Administrator)
   - `org_managerplus` / `password` (Sentinel Senior Manager)
   - `org_manager` / `password` (Sentinel Manager)
   - `org_support` / `password` (Sentinel Support User)
   - `org_user` / `password` (Sentinel Standard User)
3. ✅ Test role-based access in dashboard

**Legacy usernames also work:**
- `admin` / `password` → org_super
- `manager` / `password` → org_managerplus
- `user` / `password` → org_manager
- `support` / `password` → org_support

## Configuration

### Connection Mapping

Configure customer mappings in `src/main/resources/application.yml`:

```yaml
workos:
  connection-mapping:
    conn_01K8R9BKTPJWV123532JYJ5T6H: CORP_PROD_001  # Okta connection
    conn_01K953TWV92J9M1F1J0CR85QB6: CORP_PROD_002  # Azure Entra ID connection
```

**Adding a New Customer:**
1. Create WorkOS connection for customer's IdP
2. Get connection ID from WorkOS dashboard
3. Add one line to `application.yml`:
   ```yaml
   conn_01NEWCONNECTIONID: CORP_PROD_003
   ```
4. Restart application
5. Done! ✅

### Environment Settings

The application uses staging environment by default:

```yaml
workos:
  environment: staging  # Options: staging, production
  api:
    staging-base-url: "https://api.workos.dev"
    production-base-url: "https://api.workos.com"
```

## Troubleshooting

**Backend won't start:**
```bash
lsof -ti:8081 | xargs kill -9
```

**SSO fails:**
- Check that WorkOS connection is active in WorkOS dashboard
- Verify connection ID mapping in `application.yml`
- Check backend logs for errors
- Ensure email domain is added to WorkOS organization

**Frontend issues:**
```bash
cd frontend && npm install
```

**Connection mapping not working:**
- Verify connection ID format in `application.yml`
- Check backend logs for connection mapping initialization
- Ensure YAML syntax is correct (no inline comments)

## Demo Users

| Username | Password | Role | Display Name |
|----------|---------|------|--------------|
| `org_super` | `password` | org_super | Sentinel Master Administrator |
| `org_managerplus` | `password` | org_managerplus | Sentinel Senior Manager |
| `org_manager` | `password` | org_manager | Sentinel Manager |
| `org_support` | `password` | org_support | Sentinel Support User |
| `org_user` | `password` | org_user | Sentinel Standard User |

**Legacy usernames:**
- `admin` → org_super
- `manager` → org_managerplus
- `user` → org_manager
- `support` → org_support

## Production Deployment

To move to production:

1. **Set up WorkOS connections** for each customer's IdP
2. **Update `application.yml`**:
   ```yaml
   workos:
     environment: production
     api-key: ${WORKOS_API_KEY}
     connection-mapping:
       # Add all customer connections here
   ```
3. **Set production environment variables**:
   ```bash
   export WORKOS_API_KEY="sk_live_..."
   export WORKOS_CLIENT_ID="client_..."
   export WORKOS_SESSION_PASSWORD="..."
   ```
4. **Deploy with production URLs**

## Status: ✅ FULLY FUNCTIONAL

- ✅ IdP-initiated SSO working with Okta and Azure Entra ID
- ✅ Connection-based customer mapping functional
- ✅ Role-based access control operational
- ✅ Production-ready configuration

## Full Documentation
📖 See [DOCUMENTATION.md](DOCUMENTATION.md) for comprehensive technical details.
