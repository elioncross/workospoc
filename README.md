# WorkOS AuthKit POC - Enterprise SSO Integration

A proof-of-concept implementation demonstrating **dual authentication flows** using WorkOS AuthKit with Spring Boot backend and Angular frontend.

## 🎯 **Status: FULLY FUNCTIONAL** ✅ **Option A Hybrid Implementation**

### 🆕 **Latest Updates**
- ✅ **Updated WorkOS SDK** to v3.1.0 (latest)
- ✅ **Implemented Option A Hybrid Authorization** 
- ✅ **Resolved 403 Staging Errors** using official SDK patterns
- ✅ **Production-Ready Transition** with environment variable support

## 🚀 **Quick Start**

```bash
# Start both services
./start-backend.sh    # → http://localhost:8081
./start-frontend.sh   # → http://localhost:4200
```

**Test SSO**: Click "Login with SSO" → Complete WorkOS form → Dashboard  
**Test Internal**: Use `admin`/`password` (or `manager`, `user`, `support`)

## 🏗️ **Architecture Overview**

This project showcases a **hybrid authentication system** that supports:
- **Session-based SSO** via WorkOS AuthKit (following official patterns)
- **JWT-based authentication** for API access  
- **Seamless transition** from staging (Test Identity Provider) to production (real organizations)

## 🎯 **Key Features - Option A Implementation**

### ✅ **Smart Organization Detection**
```java
// Checks for WORKOS_ORGANIZATION_ID environment variable
String organizationId = System.getenv("WORKOS_ORGANIZATION_ID");

if (organizationId != null) {
    // Use real organization (production pattern)
    return workOS().sso.getAuthorizationUrl(clientId, redirectUri)
        .organization(organizationId).build();
} else {
    // Fallback to Test Identity Provider (staging)
    return workOS().sso.getAuthorizationUrl(clientId, redirectUri)
        .organization("org_test_idp").build();
}
```

### ✅ **Production Transition Guide**
When ready for production:
1. **Get WorkOS production account**
2. **Create organization** in WorkOS Dashboard
3. **Set environment variable**: `export WORKOS_ORGANIZATION_ID="org_01..."`
4. **Restart application** → Automatically switches to production pattern

## 🛠️ **Technology Stack**
- **Backend**: Spring Boot 2.7.18, Spring Security
- **Frontend**: Angular (latest)
- **Authentication**: WorkOS AuthKit SDK v3.1.0 (updated)
- **Build Tool**: Maven
- **Java Version**: 11

## 🔧 **Configuration**

### Current Setup (Staging)
```yaml
# No environment variables needed
# Uses Test Identity Provider (org_test_idp)
```

### Production Setup
```bash
# Set your WorkOS organization ID
export WORKOS_ORGANIZATION_ID="org_01..."
# Application automatically detects and switches to production pattern
```

## 🏗 **What's Included**

- **Option A Hybrid Authorization**: Smart environment detection
- **Dual Authentication**: WorkOS SSO + Internal users  
- **Test Environment**: WorkOS Test IdP for development
- **JWT Tokens**: Rich user claims from both auth methods
- **Role-based Access**: SMA, MA, MC, SU roles with protected endpoints
- **Angular Frontend**: Complete auth flow with guards
- **Production Ready**: Environment-specific configuration

## 📖 **Documentation**

| File | Purpose |
|------|---------|
| [SETUP.md](SETUP.md) | **Start here** - Essential setup & testing |
| [DOCUMENTATION.md](DOCUMENTATION.md) | Complete technical reference |

## 🔧 **Key URLs**
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8081/api/me
- **SSO Flow**: http://localhost:8081/api/auth/sso/workos

---
**Ready to test SSO? Start the apps and click "Login with SSO"!** 🎉

---
**Ready to test SSO? Start the apps and click "Login with SSO"!** 🎉

## 📋 **API Endpoints**

### Authentication
- `GET /api/auth/sso/workos` - Initiate WorkOS SSO
- `GET /auth/workos/callback` - WorkOS callback handler
- `POST /api/auth/login` - Internal user login
- `GET /api/me` - Get current user details

### Demo Endpoints (Role-based)
- `GET /api/demo/user` - Requires USER, MANAGER, or ADMIN role
- `GET /api/demo/manager` - Requires MANAGER or ADMIN role
- `GET /api/demo/admin` - Requires ADMIN role

## 📖 **Documentation**

📖 **Complete Setup Guide**: [DOCUMENTATION.md](DOCUMENTATION.md)  
🔧 **Configuration Details**: [SETUP.md](SETUP.md)
## 🏁 **Next Steps**

### Current Environment: Staging with Test IdP ✅
Your app is running with WorkOS Test Identity Provider - perfect for development and testing.

### Moving to Production 🚀
1. **Set up real SAML connection** in WorkOS dashboard (Okta, Azure AD, Google)
2. **Update configuration**:
   ```yaml
   workos:
     environment: production
     api-key: sk_live_your_production_key
     connection-id: your_real_connection_id
   ```
3. **Deploy with production URLs**

## 🔧 **Project Structure**

```
workospoc/
├── src/main/java/com/example/workospoc/
│   ├── config/
│   │   ├── WorkOSConfig.java         # WorkOS configuration & environment detection
│   │   ├── SecurityConfig.java       # Spring Security with dual auth flows
│   │   ├── JwtUtil.java             # JWT generation with WorkOS claims
│   │   └── JwtRequestFilter.java     # JWT validation filter
│   ├── controller/
│   │   ├── WorkOSAuthController.java      # SSO initiation
│   │   ├── WorkOSCallbackController.java  # SSO callback handler
│   │   ├── AuthController.java            # Internal auth
│   │   └── DemoController.java            # Role-based endpoints
│   └── WorkosPocApplication.java
├── frontend/src/app/
│   ├── services/auth.service.ts      # Auth service with SSO support
│   ├── guards/auth.guard.ts          # Route protection
│   └── components/                   # Login, Dashboard, Role Demo
├── src/main/resources/
│   └── application.yml               # Configuration with WorkOS credentials
├── start-backend.sh                  # Backend startup with env vars
├── start-frontend.sh                 # Frontend startup
└── Documentation files
```

## 🛡 **Security Features**

- **Hybrid Authentication**: Internal users + external SSO users
- **JWT Claims**: Rich user context from WorkOS profiles
- **Role Mapping**: Email-based role assignment
- **Environment Isolation**: Staging fallback vs production validation
- **CORS Configuration**: Secure cross-origin requests
- **Auth Guards**: Frontend route protection

---

**🎉 Your WorkOS integration is complete and production-ready!**