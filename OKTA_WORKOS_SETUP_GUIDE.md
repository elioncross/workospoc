# Okta + WorkOS SSO Integration Setup Guide

This guide walks you through setting up Okta as your Identity Provider (IdP) with WorkOS as the SSO broker for your application.

## 🏗️ Architecture Overview

```
Your Application ←→ WorkOS (SSO Broker) ←→ Okta (Identity Provider)
```

- **Your App**: Receives JWT tokens with custom attributes (corpId, role)
- **WorkOS**: Handles SSO protocol complexity and attribute mapping
- **Okta**: Authenticates users and provides SAML assertions with custom attributes

## 📋 Prerequisites

- [ ] Okta Admin Console access
- [ ] WorkOS account (staging or production)
- [ ] This WorkOS POC application running locally
- [ ] Users in Okta with custom attribute values

## 🔧 Step 1: Configure Okta Application

### 1.1 Create SAML Application

1. **Login to Okta Admin Console**
2. Navigate to **Applications** → **Create App Integration**
3. Select:
   - **Sign-in method**: `SAML 2.0`
   - **Application type**: `Web`
4. Click **Next**

### 1.2 General Settings

- **App name**: `Your Company App (via WorkOS)`
- **App logo**: Upload your company logo (optional)
- **App visibility**: Check "Do not display application icon to users"
- Click **Next**

### 1.3 SAML Settings - Part 1

#### Basic SAML Configuration:
```
Single sign on URL: https://api.workos.com/sso/saml/acs/PLACEHOLDER
Audience URI (SP Entity ID): https://api.workos.com
Default RelayState: (leave empty)
Name ID format: EmailAddress
Application username: Email
```

> ⚠️ **Note**: We'll update the SSO URL after creating the WorkOS connection

#### Advanced Settings:
- **Response**: `Signed`
- **Assertion Signature**: `Signed`
- **Signature Algorithm**: `RSA_SHA256`
- **Digest Algorithm**: `SHA256`
- **Assertion Encryption**: `Unencrypted`

### 1.4 SAML Settings - Part 2 (Attribute Statements)

Add these attribute statements for custom data:

| **Name** | **Name format** | **Value** | **Purpose** |
|----------|-----------------|-----------|-------------|
| `customer_corpid` | `Basic` | `user.department` | Corporate hierarchy/scoping |
| `custom_role` | `Basic` | `user.userType` | Application role [SMA, MA, MC, SU] |
| `email` | `Basic` | `user.email` | User identification |
| `firstName` | `Basic` | `user.firstName` | Display name |
| `lastName` | `Basic` | `user.lastName` | Display name |

#### Custom Attribute Setup:
If you need to create custom fields in Okta:
1. **Directory** → **Profile Editor** → **User (default)**
2. **Add Attribute**:
   - **Data type**: `string`
   - **Display name**: `Corporate ID` / `Application Role`
   - **Variable name**: `corporateId` / `applicationRole`
   - **Description**: Purpose of the field

### 1.5 Feedback Settings
- **I'm an Okta customer adding an internal app**
- **This is an internal app that we have created**

Click **Finish**

## 🔗 Step 2: Configure WorkOS Connection

### 2.1 Create Organization

1. **Login to WorkOS Dashboard**
2. **Organizations** → **Create Organization**
3. Fill in details:
   ```
   Name: Your Company Name
   Domains: yourcompany.com (add all email domains for your users)
   ```

### 2.2 Create SAML Connection

1. **Go to your Organization**
2. **Connections** → **Add Connection**
3. **Connection Type**: `Generic SAML`
4. **Connection Name**: `Okta SAML`

### 2.3 Get Okta SAML Metadata

Back in Okta:
1. **Applications** → **Your SAML App** → **Sign On**
2. **View Setup Instructions**
3. Copy these values:

```xml
Identity Provider SSO URL: https://your-org.okta.com/app/your-app-id/sso/saml
Identity Provider Issuer: http://www.okta.com/your-app-id
X.509 Certificate: [Full certificate including BEGIN/END lines]
```

### 2.4 Configure WorkOS Connection

Paste the Okta values into WorkOS:

```
Identity Provider SSO URL: [From Okta Setup Instructions]
Identity Provider Issuer: [From Okta Setup Instructions]  
X.509 Certificate: [Full certificate from Okta]
```

**Save the connection** and note the **Connection ID** (e.g., `conn_01ABC123...`)

## 🔄 Step 3: Update Okta with WorkOS Details

### 3.1 Update SAML Configuration

Go back to Okta:
1. **Applications** → **Your SAML App** → **General** → **Edit**
2. Update **SAML Settings**:

```
Single sign on URL: https://api.workos.com/sso/saml/acs/conn_01YOUR_CONNECTION_ID
Audience URI (SP Entity ID): https://api.workos.com
```

Replace `conn_01YOUR_CONNECTION_ID` with your actual WorkOS connection ID.

3. **Save** the configuration

## 🎯 Step 4: Configure Your Application

### 4.1 Update Environment Variables

Create or update your `.env` file:

```bash
# WorkOS Configuration
WORKOS_API_KEY=sk_live_your_production_key  # or sk_test_ for staging
WORKOS_CLIENT_ID=client_01YOUR_CLIENT_ID
WORKOS_ORGANIZATION_ID=org_01YOUR_ORG_ID

# Application Configuration
WORKOS_ENVIRONMENT=production  # or staging
SERVER_PORT=8081

# JWT Configuration
JWT_SECRET=your-secure-jwt-secret-key
JWT_EXPIRATION=86400000
```

### 4.2 Update WorkOS Dashboard Redirect URI

In WorkOS Dashboard → **Configuration** → **Redirect URIs**:
```
http://localhost:8081/auth/workos/callback
https://your-production-domain.com/auth/workos/callback
```

## 👥 Step 5: Configure Okta Users

### 5.1 Assign Users to Application

1. **Applications** → **Your SAML App** → **Assignments**
2. **Assign** → **Assign to People**
3. **Select users** and assign them

### 5.2 Set Custom Attribute Values

For each assigned user:
1. **Directory** → **People** → **Select User**
2. **Profile** → **Edit**
3. Set custom values:

```
Corporate ID: CORP_001, CORP_002, etc.
Application Role: SMA, MA, MC, or SU
```

#### Role Mapping Reference:
- **SMA**: Super Manager Admin (Full access)
- **MA**: Manager (Manager access)  
- **MC**: Member/Customer (Limited access)
- **SU**: Support (Support access)

## 🧪 Step 6: Test the Integration

### 6.1 Start Your Application

```bash
# Terminal 1: Start Backend
cd /Users/rleon/Projects/workospoc
mvn spring-boot:run

# Terminal 2: Start Frontend  
cd /Users/rleon/Projects/workospoc/frontend
npm start
```

### 6.2 Test SSO Flow

1. **Navigate to**: `http://localhost:4200`
2. **Click**: "Login with SSO"
3. **Should redirect to**: Okta login page
4. **Enter credentials**: Okta username/password
5. **After authentication**: Should redirect back to app dashboard

### 6.3 Verify Custom Attributes

After successful login, test these endpoints:

```bash
# Check what attributes were received
curl -s http://localhost:8081/debug/workos-attributes | python3 -m json.tool

# Check current user data
curl -s http://localhost:8081/api/me | python3 -m json.tool
```

Expected response should include:
```json
{
  "rawAttributes": {
    "customer_corpid": "CORP_001",
    "custom_role": "SMA", 
    "email": "user@yourcompany.com",
    "firstName": "John",
    "lastName": "Doe"
  },
  "corpId": "CORP_001",
  "role": "SMA"
}
```

## 🔍 Step 7: Troubleshooting

### Common Issues:

#### Issue 1: "SAML Response Invalid"
**Solution**: 
- Verify ACS URL matches exactly between Okta and WorkOS
- Check that certificate was copied correctly (no extra spaces)
- Ensure Audience URI is exactly `https://api.workos.com`

#### Issue 2: Custom Attributes Not Appearing
**Solution**:
- Verify attribute statements in Okta SAML configuration
- Check that users have values set for custom fields
- Confirm attribute names match exactly: `customer_corpid`, `custom_role`

#### Issue 3: WorkOS Connection Not Active
**Solution**:
- Ensure WorkOS connection status is "Active"
- Verify organization domains match user email domains
- Check that connection ID is correct in Okta ACS URL

#### Issue 4: Application Not Receiving Attributes
**Solution**:
- Check backend logs for extraction errors
- Verify JWT token includes custom claims
- Test debug endpoints to see raw SAML data

### Debug Commands:

```bash
# Check application logs
mvn spring-boot:run | grep -E "(WorkOS|SAML|Profile)"

# Test WorkOS configuration
curl -s http://localhost:8081/api/test/workos | python3 -m json.tool

# Check session data
curl -s http://localhost:8081/debug/workos-attributes | python3 -m json.tool
```

## 🚀 Step 8: Production Deployment

### 8.1 Environment Configuration

For production deployment:

```bash
# Production .env
WORKOS_API_KEY=sk_live_your_production_key
WORKOS_CLIENT_ID=client_01YOUR_PROD_CLIENT_ID
WORKOS_ENVIRONMENT=production
```

### 8.2 Update Redirect URIs

In both Okta and WorkOS, update callback URLs:
```
https://your-production-domain.com/auth/workos/callback
```

### 8.3 SSL Certificate

Ensure your production domain has valid SSL certificate for SAML security.

## 📊 Expected User Flow

1. **User visits app** → `https://yourapp.com`
2. **Clicks "Login with SSO"** → Redirects to WorkOS
3. **WorkOS redirects** → `https://your-org.okta.com/...`
4. **User authenticates in Okta** → Okta sends SAML assertion to WorkOS
5. **WorkOS processes SAML** → Extracts custom attributes
6. **WorkOS redirects** → `https://yourapp.com/auth/workos/callback?code=...`
7. **App processes callback** → Generates JWT with corpId and role
8. **User logged in** → Dashboard with proper authorization

## ✅ Success Criteria

After successful setup, verify:

- [ ] Users can login through Okta SSO
- [ ] Custom attributes (`customer_corpid`, `custom_role`) are extracted
- [ ] JWT tokens include `corpId` and `role` claims
- [ ] Role-based access control works [SMA, MA, MC, SU]
- [ ] Corporate scoping works with `corpId`
- [ ] No authentication errors in logs

## 📞 Support

- **WorkOS Documentation**: [https://workos.com/docs](https://workos.com/docs)
- **Okta SAML Documentation**: [https://developer.okta.com/docs/concepts/saml/](https://developer.okta.com/docs/concepts/saml/)
- **Application Issues**: Check application logs and debug endpoints

---

**Last Updated**: October 29, 2025  
**Version**: 1.0  
**Author**: WorkOS POC Team