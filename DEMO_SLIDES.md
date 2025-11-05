# WorkOS SSO Integration POC - Demo Introduction

---

## Slide 1: Title Slide
**WorkOS Enterprise SSO Integration POC**  
*Multi-IdP SAML2 SSO with Dynamic Customer Mapping*

---

## Slide 2: Problem Statement
**The Challenge**
- Support multiple enterprise customers, each with their own Identity Provider (IdP)
- Each customer needs to be automatically mapped to their account ID (corpId)
- Must work with Okta, Azure Entra ID, and other SAML providers

**The Goal**
- Automatic customer identification via WorkOS connection
- Minimal application-side configuration to add new customers
- Scalable solution for supporting multiple IdPs

---

## Slide 3: Solution Architecture
```
Customer IdP (Okta/Azure) 
    ↓
WorkOS (SAML Broker)
    ↓
Our Application (Spring Boot + Angular)
    ↓
Customer Dashboard with Context
```

**Key Components**
- **WorkOS**: Handles all SAML complexity and IdP communication
- **Connection Mapping**: Automatically maps IdP connection to customer account
- **JWT Tokens**: Secure authentication with customer context

---

## Slide 4: How It Works
**IdP-Initiated SSO Flow**

1. User initiates SSO from their IdP dashboard (Okta/Azure)
2. WorkOS processes SAML and identifies which customer's IdP
3. Application receives connection ID and maps to customer account (corpId)
4. User lands on dashboard with correct customer context

**Connection Mapping Example**
```yaml
Okta Connection → CORP_PROD_001
Azure Connection → CORP_PROD_002
```

---

## Slide 5: What We Achieved
✅ **Multi-IdP Support**: Okta and Azure Entra ID working seamlessly  
✅ **Dynamic Customer Mapping**: Connection ID → Customer ID automatically  
✅ **Minimal Application Configuration**: Simple YAML mapping to add new customers  
✅ **Role-Based Access**: WorkOS roles mapped to system permissions  
✅ **Production Ready**: Staging and production environments configured  

**Key Innovation**
- WorkOS connection ID automatically identifies customer
- Simple YAML configuration to add new customers
- Once customer IdP is configured in WorkOS, only one line needed in application config

---

## Slide 6: Demo Flow
**Live Demonstration**

1. **Okta SSO**: User logs in via Okta → Dashboard shows CORP_PROD_001
2. **Azure SSO**: User logs in via Azure → Dashboard shows CORP_PROD_002
3. **Configuration**: Show how easy it is to add new customers

**Dashboard Display**
- User Email
- User Role (from WorkOS)
- Customer ID (corpId) - automatically mapped

---

## Slide 7: Benefits & Value
**Business Benefits**
- 🚀 **Fast Onboarding**: New customers added in seconds (one line of config)
- 🔒 **Secure**: WorkOS handles all SAML complexity
- 🎯 **Flexible**: Support any SAML-compliant IdP
- 📊 **Scalable**: Easy to add new customers without code changes

**Technical Excellence**
- Clean, maintainable architecture
- Configuration-driven approach
- Production-ready with error handling

---

## Slide 8: Next Steps
**Immediate**
- Production deployment
- Add more IdP connections as needed

**Future Enhancements**
- Dynamic role/corpId lookup via system API
- Additional IdP providers (Google Workspace, etc.)
- Enhanced user management features

**Status: ✅ Ready for Production**

---

## Slide 9: Q&A
**Questions?**

Thank you!

