# Technical Documentation

## Architecture Overview

**Technology Stack**: Spring Boot 2.7.18 + Spring Security 5.7.11 + Angular 15  
**SSO Provider**: WorkOS AuthKit (SAML2 broker)  
**Authentication**: JWT tokens with comprehensive user claims  
**Flow Type**: IdP-initiated SSO only (users start from their IdP dashboard)

### Authentication Flow

```
User (IdP Dashboard) 
    ↓ (User clicks SSO)
IdP (Okta/Azure Entra ID)
    ↓ (SAML Assertion)
WorkOS (SAML Broker)
    ↓ (Profile + Connection ID)
Backend (/auth/workos/callback)
    ↓ (Extract connectionId → corpId)
    ↓ (Generate JWT with corpId, role)
Frontend (Dashboard with context)
```

### IdP-Initiated Flow Sequence

1. User initiates SSO from their IdP dashboard (Okta or Azure Entra ID)
2. IdP sends SAML assertion to WorkOS
3. WorkOS processes SAML and identifies connection
4. WorkOS redirects to app callback with authorization code
5. App exchanges code for profile (includes `connectionId`, `email`, `role`, etc.)
6. App maps `connectionId` → `corpId` from configuration
7. App generates JWT with `corpId`, `role`, `email`, etc.
8. User lands on dashboard with customer context

## Configuration

### Application Configuration

```yaml
# application.yml
workos:
  environment: staging  # Options: staging, production
  api-key: ${WORKOS_API_KEY}
  client-id: ${WORKOS_CLIENT_ID}
  session-password: ${WORKOS_SESSION_PASSWORD}
  redirect-uri: http://localhost:8081/auth/workos/callback
  
  # Connection to CorpId Mapping
  # Each customer has their own WorkOS connection to their IdP
  connection-mapping:
    conn_01K8R9BKTPJWV123532JYJ5T6H: CORP_PROD_001  # Okta connection
    conn_01K953TWV92J9M1F1J0CR85QB6: CORP_PROD_002  # Azure Entra ID connection
  
  api:
    staging-base-url: "https://api.workos.dev"
    production-base-url: "https://api.workos.com"
  
  frontend:
    base-url: "http://localhost:4200"
    dashboard-path: "/dashboard"
    login-path: "/login"
```

### Environment Variables

Set these in your `.env` file or export before running:

```bash
export WORKOS_API_KEY="sk_test_..."
export WORKOS_CLIENT_ID="client_..."
export WORKOS_SESSION_PASSWORD="..."
```

The `start-backend.sh` script will automatically load these from `.env`.

## Core Components

| Component | Purpose |
|-----------|---------|
| **WorkOSConfig.java** | WorkOS SDK setup, connection mapping, environment detection |
| **WorkOSCallbackController.java** | IdP-initiated SSO callback handling, profile processing, JWT generation |
| **JwtUtil.java** | JWT token creation/validation with custom claims (corpId, role, etc.) |
| **JwtRequestFilter.java** | JWT validation filter, extracts corpId from token claims |
| **SecurityConfig.java** | Spring Security configuration, JWT filters, role-based endpoints |
| **auth.service.ts** | Frontend authentication service and token management |
| **auth.guard.ts** | Route protection and token validation |

## API Reference

### Key Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/auth/workos/callback` | Handle IdP-initiated SSO callback |
| `POST` | `/api/auth/login` | Internal user login |
| `GET` | `/api/me` | Get current user details |
| `GET` | `/api/demo/user` | Requires USER, MANAGER, or ADMIN role |
| `GET` | `/api/demo/manager` | Requires MANAGER or ADMIN role |
| `GET` | `/api/demo/admin` | Requires ADMIN role |

**Note**: SP-initiated SSO endpoint (`/api/auth/sso/workos`) is disabled. Only IdP-initiated flows are supported.

### Sample Responses

#### Internal Login
```json
POST /api/auth/login {"username":"org_super","password":"password"}
→ {
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "username": "org_super",
    "role": "org_super",
    "corpId": "corp1"
  }
}
```

#### User Profile (SSO User)
```json
GET /api/me (with JWT)
→ {
  "username": "user@company.com",
  "corpId": "CORP_PROD_001",
  "role": "org_super",
  "authenticated": true
}
```

## JWT Token Structure

### Internal Users
```json
{
  "sub": "org_super",
  "role": "org_super",
  "corpId": "corp1",
  "source": "internal",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### WorkOS Users (Production)
```json
{
  "sub": "user@company.com",
  "role": "org_super",
  "corpId": "CORP_PROD_001",
  "source": "workos",
  "firstName": "John",
  "lastName": "Doe",
  "profileId": "prof_01H1WXDM8KYNXF8RBVZ3VY9Q7N",
  "connectionId": "conn_01K8R9BKTPJWV123532JYJ5T6H",
  "organizationId": "org_01K8R9B8H3789HWZ7ZBK5VVE9W",
  "iat": 1234567890,
  "exp": 1234654290
}
```

**Key Claims:**
- `sub`: User email (subject)
- `role`: System role (org_super, org_manager, etc.)
- `corpId`: Customer account ID (mapped from connectionId)
- `connectionId`: WorkOS connection ID
- `source`: "workos" for SSO users, "internal" for demo users

## Connection-Based Customer Mapping

### How It Works

1. **WorkOS identifies connection**: When a user initiates SSO from their IdP, WorkOS receives the SAML assertion and identifies which connection (IdP) it came from
2. **Connection ID in profile**: WorkOS includes the `connectionId` in the user profile
3. **Application maps to corpId**: The application looks up the `connectionId` in the `connection-mapping` configuration
4. **Customer context**: User is authenticated with the correct customer account (corpId)

### Configuration Example

```yaml
workos:
  connection-mapping:
    conn_01K8R9BKTPJWV123532JYJ5T6H: CORP_PROD_001  # Okta
    conn_01K953TWV92J9M1F1J0CR85QB6: CORP_PROD_002  # Azure
```

### Adding a New Customer

1. Create WorkOS SAML connection for customer's IdP
2. Get connection ID from WorkOS dashboard
3. Add one line to `application.yml`:
   ```yaml
   conn_01NEWCONNECTIONID: CORP_PROD_003
   ```
4. Restart application
5. Done! ✅

## Role Management

### Supported Roles

| Role Code | Display Name | Description |
|-----------|--------------|-------------|
| `org_super` | Sentinel Master Administrator | Full access |
| `org_managerplus` | Sentinel Senior Manager | Senior management access |
| `org_manager` | Sentinel Manager | Management access |
| `org_support` | Sentinel Support User | Support access |
| `org_user` | Sentinel Standard User | Standard user access |

### Role Extraction

Roles are extracted from WorkOS profile:
1. **Primary**: WorkOS provides role in profile (from organization membership)
2. **Fallback**: System API lookup (if configured)
3. **Default**: `org_user` if no role found

## Environment Configuration

### Staging vs Production

| Environment | API Base URL | WorkOS Environment |
|-------------|--------------|-------------------|
| **Staging** | `https://api.workos.dev` | Staging WorkOS account |
| **Production** | `https://api.workos.com` | Production WorkOS account |

### Environment Detection

The application detects environment from `workos.environment` in `application.yml`:

```yaml
workos:
  environment: staging  # or production
```

## Production Deployment

### Prerequisites

1. WorkOS production account
2. SAML connections configured for each customer's IdP
3. Connection mappings configured in `application.yml`

### Configuration Steps

1. **Set production environment**:
   ```yaml
   workos:
     environment: production
   ```

2. **Set production environment variables**:
   ```bash
   export WORKOS_API_KEY="sk_live_..."
   export WORKOS_CLIENT_ID="client_..."
   export WORKOS_SESSION_PASSWORD="..."
   ```

3. **Configure connection mappings**:
   ```yaml
   workos:
     connection-mapping:
       # Add all customer connections here
       conn_01CUSTOMER1: CORP_PROD_001
       conn_01CUSTOMER2: CORP_PROD_002
       # ...
   ```

4. **Update frontend URLs**:
   ```yaml
   workos:
     frontend:
       base-url: "https://your-production-domain.com"
   ```

5. **Deploy and test**

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| **Connection mapping not working** | Check connection ID format in `application.yml`, verify YAML syntax |
| **corpId showing organization ID** | Verify JWT filter reads `corpId` from token (not `organizationId`) |
| **SSO flow fails** | Check WorkOS connection is active, verify email domain is allowed |
| **Role not extracted** | Check WorkOS profile has role, verify role extraction logic |
| **Environment variables not loaded** | Ensure `.env` file exists or variables are exported before running |

### Debug Logging

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.example.workospoc: DEBUG
    org.springframework.security: DEBUG
```

## Security Considerations

- **JWT Secret**: Use strong, environment-specific secrets
- **HTTPS**: Required for production deployments
- **CORS**: Configured for specific origins
- **Token Expiry**: 24-hour default expiration
- **Input Validation**: OAuth parameter validation in callback
- **Connection Mapping**: Secure customer identification via WorkOS connection ID
- **Role-Based Access**: Enforced at both API and frontend levels

---

**This implementation provides a complete, production-ready WorkOS SSO integration** with IdP-initiated flows, connection-based customer mapping, and role-based access control.
