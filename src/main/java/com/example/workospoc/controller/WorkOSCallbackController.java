package com.example.workospoc.controller;

import com.example.workospoc.config.JwtUtil;
import com.example.workospoc.config.WorkOSConfig;
import com.workos.WorkOS;
import com.workos.sso.models.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;

@RestController
public class WorkOSCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(WorkOSCallbackController.class);

    private final WorkOS workOS;
    private final JwtUtil jwtUtil;
    private final WorkOSConfig workOSConfig;

    public WorkOSCallbackController(WorkOS workOS, JwtUtil jwtUtil, WorkOSConfig workOSConfig) {
        this.workOS = workOS;
        this.jwtUtil = jwtUtil;
        this.workOSConfig = workOSConfig;
    }

    @GetMapping("/auth/workos/callback")
    public void handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // ADD COMPREHENSIVE ENTRY-POINT LOGGING
        logger.info("🔔🔔🔔 CALLBACK ENDPOINT HIT 🔔🔔🔔");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Query String: {}", request.getQueryString());
        logger.info("Request Method: {}", request.getMethod());
        logger.info("Remote Address: {}", request.getRemoteAddr());
        logger.info("Request URL: {}", request.getRequestURL());
        logger.info("Code parameter: {}", code != null ? "PRESENT (length: " + code.length() + ")" : "NULL");
        logger.info("Error parameter: {}", error);
        logger.info("Error Description: {}", errorDescription);
        logger.info("All Request Parameters:");
        request.getParameterMap().forEach((key, values) -> {
            logger.info("  {} = {}", key, java.util.Arrays.toString(values));
        });

        try {
            // Check for OAuth errors first
            if (error != null) {
                logger.error("OAuth error detected: {} - {}", error, errorDescription);
                handleOAuthError(error, errorDescription, response);
                return;
            }
            
            if (code == null) {
                logger.error("❌ No authorization code received from WorkOS");
                logger.error("   This might indicate a redirect URI mismatch or missing code parameter");
                String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=no_code";
                response.sendRedirect(errorUrl);
                return;
            }

            logger.info("✅ Received WorkOS callback with code: {} (length: {})", code, code.length());
            logger.debug("WorkOS instance: {}", workOS);
            logger.debug("Client ID: {}", workOSConfig.getClientId());

            // Initialize variables for user data
            String userEmail;
            String userRole;
            String corpId;
            Profile profile = null;

            // Check if this is Test IdP callback (staging environment using Test Identity Provider)
            boolean isTestIdpCallback = workOSConfig.isStagingEnvironment();
            
            // Try to get real user profile from WorkOS API using official SDK method
            try {
                if (isTestIdpCallback) {
                    logger.info("🧪 Detected Test Identity Provider callback in staging environment");
                    logger.info("Attempting WorkOS SDK getProfileAndToken for Test IdP...");
                }
                
                logger.info("🔧 Using official WorkOS SDK getProfileAndToken method");
                
                // Use official WorkOS SDK method - this should work with both staging and production
                com.workos.sso.models.ProfileAndToken profileAndToken = workOS.sso.getProfileAndToken(
                    code, 
                    workOSConfig.getClientId()
                );
                
                profile = profileAndToken.profile;
                userEmail = profile.email;
                
                // Extract custom attributes from SAML
                corpId = extractCorpId(profile);
                userRole = extractUserRole(profile);
                
                // Log extraction results
                logger.info("Extracted custom attributes - corpId: {}, role: {}", corpId, userRole);
                
                logger.info("✅ Successfully retrieved WorkOS profile using SDK: {}", userEmail);
                // Try to get role for logging (may not be directly accessible)
                String roleForLog = "N/A";
                try {
                    if (profile.rawAttributes != null && profile.rawAttributes.containsKey("role")) {
                        Object roleObj = profile.rawAttributes.get("role");
                        roleForLog = roleObj != null ? roleObj.toString() : "N/A";
                    }
                } catch (Exception e) {
                    // Ignore - role might not be accessible this way
                }
                logger.info("Profile details - ID: {}, Email: {}, First Name: {}, Last Name: {}, Connection ID: {}, Connection Type: {}, System Role: {}", 
                    profile.id, profile.email, profile.firstName, profile.lastName, profile.connectionId, profile.connectionType, roleForLog);
                
                // Store profile in session (following WorkOS example pattern)
                HttpSession session = request.getSession();
                session.setAttribute("sso_profile", profile);
                session.setAttribute("user_authenticated", true);
                session.setAttribute("user_email", userEmail);
                session.setAttribute("user_role", userRole);
                session.setAttribute("user_corp_id", corpId);
                
                logger.info("💾 Stored profile in session for user: {} (corpId: {}, role: {})", userEmail, corpId, userRole);
                
                // Log raw attributes to see what SAML attributes are available
                if (profile.rawAttributes != null && !profile.rawAttributes.isEmpty()) {
                    logger.info("WorkOS Profile raw attributes: {}", profile.rawAttributes);
                    for (String key : profile.rawAttributes.keySet()) {
                        logger.info("SAML Attribute: {} = {}", key, profile.rawAttributes.get(key));
                    }
                } else {
                    logger.warn("No raw attributes found in WorkOS profile");
                }
                
            } catch (Exception e) {
                logger.warn("❌ WorkOS SDK ProfileAndToken call failed: {}", e.getMessage());
                logger.debug("Error details: ", e);
                
                if (workOSConfig.isStagingEnvironment()) {
                    // STAGING FALLBACK: Use clear static fallback user
                    logger.warn("=".repeat(60));
                    logger.warn("STAGING FALLBACK USER - NOT REAL DATA FROM WORKOS");
                    logger.warn("=".repeat(60));
                    
                    userEmail = workOSConfig.getStagingFallbackEmail();
                    userRole = workOSConfig.getStagingFallbackRole();
                    corpId = "staging_corp"; // Default corpId for staging
                    profile = null; // No real profile in staging fallback
                    
                    // Store fallback data in session too
                    HttpSession session = request.getSession();
                    session.setAttribute("sso_profile", null);
                    session.setAttribute("user_authenticated", true);
                    session.setAttribute("user_email", userEmail);
                    session.setAttribute("user_role", userRole);
                    session.setAttribute("user_corp_id", corpId);
                    session.setAttribute("is_fallback_user", true);
                    
                    logger.info("Using static fallback user:");
                    logger.info("  Email: {}", userEmail);
                    logger.info("  Name: {} {}", workOSConfig.getStagingFallbackFirstName(), workOSConfig.getStagingFallbackLastName());
                    logger.info("  Role: {}", userRole);
                    logger.info("  Organization: {} ({})", workOSConfig.getStagingFallbackOrgName(), workOSConfig.getStagingFallbackOrgId());
                    logger.info("  Connection ID: {}", workOSConfig.getStagingFallbackConnectionId());
                    logger.warn("THIS IS STAGING FALLBACK DATA - NOT FROM WORKOS API");
                    logger.warn("=".repeat(60));
                    
                } else {
                    // PRODUCTION ERROR: Show proper error page
                    logger.error("Production WorkOS authentication failed: {}", e.getMessage());
                    if (e.getMessage() != null && e.getMessage().contains("Unauthorized")) {
                        String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=api_unauthorized&message=" + 
                            URLEncoder.encode("WorkOS API authentication failed. Check API key configuration.", "UTF-8");
                        response.sendRedirect(errorUrl);
                    } else {
                        String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=sso_failed&message=" + 
                            URLEncoder.encode("Authentication failed: " + e.getMessage(), "UTF-8");
                        response.sendRedirect(errorUrl);
                    }
                    return;
                }
            }

            // Create JWT token with user information
            String token;
            if (profile != null) {
                // Real profile from WorkOS API
                token = jwtUtil.generateTokenForWorkOSUser(userEmail, userRole, corpId, profile);
                logger.info("Generated JWT token from real WorkOS profile for user: {} (corpId: {}, role: {})", userEmail, corpId, userRole);
            } else {
                // Staging fallback - create token with static fallback attributes
                token = jwtUtil.generateTokenForWorkOSUserStaging(
                    workOSConfig.getStagingFallbackEmail(),
                    workOSConfig.getStagingFallbackRole(),
                    corpId, // Use the corpId variable we set in staging fallback
                    workOSConfig.getStagingFallbackFirstName(),
                    workOSConfig.getStagingFallbackLastName(),
                    workOSConfig.getStagingFallbackOrgName(),
                    workOSConfig.getStagingFallbackOrgId(),
                    workOSConfig.getStagingFallbackConnectionId()
                );
                logger.info("Generated JWT token from staging fallback data for user: {} (corpId: {}, role: {})", userEmail, corpId, userRole);
            }

            // Redirect to frontend with token (URL-encoded to handle special characters)
            String encodedToken = URLEncoder.encode(token, "UTF-8");
            String redirectUrl = workOSConfig.getFrontendDashboardUrl() + "?token=" + encodedToken;
            logger.info("Redirecting to frontend with URL-encoded token");
            logger.debug("Token length: {}, Encoded token length: {}", token.length(), encodedToken.length());
            response.sendRedirect(redirectUrl);
            logger.info("Redirect response sent successfully");

        } catch (Exception e) {
            logger.error("Error handling WorkOS callback", e);
            
            // Check if this is an unauthorized error (API key issue)
            if (e.getMessage() != null && e.getMessage().contains("Unauthorized")) {
                logger.error("WorkOS API Unauthorized error - likely invalid API key");
                String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=api_unauthorized&message=" + 
                    URLEncoder.encode("WorkOS API authentication failed. Please check your API key configuration.", "UTF-8");
                response.sendRedirect(errorUrl);
            } else {
                String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=sso_failed&message=" + 
                    URLEncoder.encode("Authentication failed: " + e.getMessage(), "UTF-8");
                response.sendRedirect(errorUrl);
            }
        }
    }

    private void handleOAuthError(String error, String errorDescription, HttpServletResponse response) throws IOException {
        logger.error("❌ OAuth Error: {} - {}", error, errorDescription);
        
        if ("access_denied".equals(error)) {
            String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=access_denied&message=" + 
                URLEncoder.encode("Access denied by user", "UTF-8");
            response.sendRedirect(errorUrl);
        } else if ("invalid_request".equals(error)) {
            String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=invalid_request&message=" + 
                URLEncoder.encode("Invalid request parameters", "UTF-8");
            response.sendRedirect(errorUrl);
        } else if ("domain_not_allowed".equals(error)) {
            String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=domain_not_allowed&message=" + 
                URLEncoder.encode("Please use an email from an allowed domain (e.g., @example.com)", "UTF-8");
            response.sendRedirect(errorUrl);
        } else if ("server_error".equals(error) && errorDescription != null && errorDescription.contains("SAML")) {
            // SAML configuration error - provide specific guidance
            logger.error("❌ SAML Configuration Error: {}", errorDescription);
            logger.error("   This usually means one of the following:");
            logger.error("   1. ACS URL in Okta doesn't match WorkOS ACS URL exactly");
            logger.error("   2. Entity ID (Audience URI) mismatch between Okta and WorkOS");
            logger.error("   3. SAML signing certificate mismatch");
            logger.error("   4. Name ID format mismatch");
            logger.error("   5. Missing required SAML attributes");
            logger.error("   6. SAML response signature validation failed");
            logger.error("   Please verify all SAML settings in Okta match WorkOS configuration");
            
            String detailedMessage = String.format(
                "SAML Configuration Error: %s. Please verify: ACS URL, Entity ID, Certificate, Name ID format, and SAML attributes match WorkOS settings.",
                errorDescription
            );
            String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=saml_config_error&message=" + 
                URLEncoder.encode(detailedMessage, "UTF-8");
            response.sendRedirect(errorUrl);
        } else {
            String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=sso_failed&message=" + 
                URLEncoder.encode(errorDescription != null ? errorDescription : "SSO authentication failed", "UTF-8");
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * Extract custom SAML attribute from WorkOS profile raw attributes
     */
    private String extractCustomAttribute(Profile profile, String attributeName, String defaultValue) {
        if (profile == null || profile.rawAttributes == null || profile.rawAttributes.isEmpty()) {
            logger.debug("No raw attributes available for extraction of {}, using default: {}", attributeName, defaultValue);
            return defaultValue;
        }
        
        Object attributeValue = profile.rawAttributes.get(attributeName);
        if (attributeValue != null) {
            String value = attributeValue.toString().trim();
            if (!value.isEmpty()) {
                logger.info("Extracted {} = {} from WorkOS SAML attributes", attributeName, value);
                return value;
            }
        }
        
        logger.debug("Attribute {} not found or empty in raw attributes, using default: {}", attributeName, defaultValue);
        return defaultValue;
    }
    
    /**
     * Extract corpId from customer_corpid SAML attribute
     */
    private String extractCorpId(Profile profile) {
        return extractCustomAttribute(profile, "customer_corpid", "default_corp");
    }
    
    /**
     * Extract role from WorkOS profile role.slug (assigned in WorkOS)
     * WorkOS returns system roles directly: org_super, org_managerplus, org_manager, org_support, org_user
     * Falls back to customer_role SAML attribute if profile role is not available
     */
    private String extractUserRole(Profile profile) {
        String role = null;
        String roleSource = null;
        
        // First, try to get role from WorkOS profile using reflection (role.slug field)
        try {
            java.lang.reflect.Field roleField = profile.getClass().getDeclaredField("role");
            roleField.setAccessible(true);
            Object roleObj = roleField.get(profile);
            if (roleObj != null) {
                java.lang.reflect.Field slugField = roleObj.getClass().getDeclaredField("slug");
                slugField.setAccessible(true);
                Object slugObj = slugField.get(roleObj);
                if (slugObj != null) {
                    role = slugObj.toString();
                    roleSource = "WorkOS profile role.slug";
                    logger.info("✅ Using role from WorkOS profile: {} (source: {})", role, roleSource);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Role field might not be accessible directly, try rawAttributes or SAML attribute
            logger.debug("Could not access profile.role.slug directly: {}", e.getMessage());
        } catch (Exception e) {
            logger.debug("Unexpected error accessing profile.role.slug: {}", e.getMessage());
        }
        
        // If role not found via reflection, check rawAttributes for role
        if (role == null && profile.rawAttributes != null) {
            Object roleObj = profile.rawAttributes.get("role");
            if (roleObj != null) {
                // Role might be a nested object, try to extract slug
                if (roleObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> roleMap = (java.util.Map<String, Object>) roleObj;
                    Object slugObj = roleMap.get("slug");
                    if (slugObj != null) {
                        role = slugObj.toString();
                        roleSource = "WorkOS profile rawAttributes role.slug";
                        logger.info("✅ Using role from WorkOS rawAttributes: {} (source: {})", role, roleSource);
                    }
                } else {
                    role = roleObj.toString();
                    roleSource = "WorkOS profile rawAttributes role";
                    logger.info("✅ Using role from WorkOS rawAttributes: {} (source: {})", role, roleSource);
                }
            }
        }
        
        // Fallback: Extract from customer_role SAML attribute
        if (role == null || role.isEmpty()) {
            role = extractCustomAttribute(profile, "customer_role", null);
            if (role != null && !role.isEmpty()) {
                roleSource = "SAML customer_role attribute";
                logger.info("Using role from SAML attribute: {} (source: {})", role, roleSource);
            }
        }
        
        // If no role found, use default
        if (role == null || role.isEmpty()) {
            logger.warn("No role found in WorkOS profile or SAML attributes, using default: org_user");
            return "org_user";
        }
        
        // Validate that the role is one of the expected system roles
        switch (role) {
            case "org_super":
            case "org_managerplus":
            case "org_manager":
            case "org_support":
            case "org_user":
                logger.info("✅ Validated system role: {} (source: {})", role, roleSource);
                return role;
            default:
                logger.warn("Unknown system role '{}' (source: {}), assigning default: org_user", role, roleSource);
                return "org_user";
        }
    }

}