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

        try {
            // Check for OAuth errors first
            if (error != null) {
                logger.error("OAuth error: {} - {}", error, errorDescription);
                handleOAuthError(error, errorDescription, response);
                return;
            }
            
            if (code == null) {
                logger.error("No authorization code received from WorkOS");
                String errorUrl = workOSConfig.getFrontendLoginUrl() + "?error=no_code";
                response.sendRedirect(errorUrl);
                return;
            }

            logger.info("Received WorkOS callback with code: {}", code);
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
                logger.info("Profile details - ID: {}, Email: {}, First Name: {}, Last Name: {}, Connection ID: {}, Connection Type: {}", 
                    profile.id, profile.email, profile.firstName, profile.lastName, profile.connectionId, profile.connectionType);
                
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

            // Redirect to frontend with token
            String redirectUrl = workOSConfig.getFrontendDashboardUrl() + "?token=" + token;
            logger.info("Redirecting to frontend: {}", redirectUrl);
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
     * Extract role from custom_role SAML attribute with validation
     */
    private String extractUserRole(Profile profile) {
        String role = extractCustomAttribute(profile, "custom_role", "MC");
        
        // Validate that the role is one of our supported values
        switch (role.toUpperCase()) {
            case "SMA": // Super Manager Admin
            case "MA":  // Manager
            case "MC":  // Member/Customer
            case "SU":  // Support
                return role.toUpperCase();
            default:
                logger.warn("Invalid role '{}' extracted from custom_role, defaulting to MC", role);
                return "MC";
        }
    }

}