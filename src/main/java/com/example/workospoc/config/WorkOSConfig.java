package com.example.workospoc.config;

import com.workos.WorkOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class WorkOSConfig {

    private static final Logger logger = LoggerFactory.getLogger(WorkOSConfig.class);
    
    // Constants for logging
    private static final String AUTH_URL_LOG_MESSAGE = "Authorization URL: {}";
    private static final String STAGING_API_KEY_PREFIX = "sk_test_";
    private static final String REDIRECT_URI = "http://localhost:8081/auth/workos/callback";

    @Value("${workos.environment:staging}")
    private String environment;

    @Value("${workos.api-key}")
    private String apiKey;

    @Value("${workos.client-id}")
    private String clientId;

    @Value("${workos.connection-id:conn_01K841NX4X1AM7TCQ22J7RFDYR}")
    private String connectionId;

    // API endpoints
    @Value("${workos.api.staging-base-url:https://api.workos.dev}")
    private String stagingBaseUrl;

    @Value("${workos.api.production-base-url:https://api.workos.com}")
    private String productionBaseUrl;

    // Frontend configuration
    @Value("${workos.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${workos.frontend.dashboard-path:/dashboard}")
    private String frontendDashboardPath;

    @Value("${workos.frontend.login-path:/login}")
    private String frontendLoginPath;

    // Session password
    @Value("${workos.session-password:}")
    private String sessionPassword;

    // Staging configuration
    @Value("${workos.staging.fallback-user.email:staging-fallback@example.com}")
    private String stagingFallbackEmail;

    @Value("${workos.staging.fallback-user.first-name:STAGING}")
    private String stagingFallbackFirstName;

    @Value("${workos.staging.fallback-user.last-name:FALLBACK}")
    private String stagingFallbackLastName;

    @Value("${workos.staging.fallback-user.role:org_manager}")
    private String stagingFallbackRole;

    @Value("${workos.staging.fallback-user.organization-name:STAGING FALLBACK ORGANIZATION}")
    private String stagingFallbackOrgName;

    @Value("${workos.staging.fallback-user.organization-id:staging-fallback-org-001}")
    private String stagingFallbackOrgId;

    @Value("${workos.staging.fallback-user.connection-id:staging-fallback-conn-001}")
    private String stagingFallbackConnectionId;

    // Production configuration
    @Value("${workos.production.validate-api-key:true}")
    private boolean productionValidateApiKey;

    @Value("${workos.production.require-real-profiles:true}")
    private boolean productionRequireRealProfiles;

    @Bean
    public WorkOS workOS() {
        logger.info("=== WorkOS Configuration Debug ===");
        logger.info("Environment value from @Value: '{}'", environment);
        logger.info("isStagingEnvironment(): {}", isStagingEnvironment());
        logger.info("stagingBaseUrl: {}", stagingBaseUrl);
        logger.info("productionBaseUrl: {}", productionBaseUrl);
        logger.info("getCurrentBaseUrl(): {}", getCurrentBaseUrl());
        logger.info("API key: {}", apiKey != null ? apiKey.substring(0, 10) + "..." : "null");
        logger.info("=================================");
        
        // Force staging environment for testing
        if (isStagingEnvironment()) {
            logger.info("✅ STAGING environment detected - using staging base URL");
            System.setProperty("workos.base.url", stagingBaseUrl);
        } else {
            logger.warn("❌ PRODUCTION environment detected - this should not happen with staging config");
            logger.warn("   Environment value: '{}'", environment);
            logger.warn("   Forcing staging base URL for testing");
            System.setProperty("workos.base.url", stagingBaseUrl);
        }
        
        // Create WorkOS instance
        WorkOS workOS = new WorkOS(apiKey);
        logger.info("WorkOS instance created with base URL: {}", System.getProperty("workos.base.url"));
        
        return workOS;
    }
    
    private void validateConfiguration() {
        logger.info("Validating WorkOS configuration...");
        
        if (isStagingEnvironment()) {
            validateStagingConfiguration();
        } else if (isProductionEnvironment() && productionValidateApiKey) {
            validateProductionConfiguration();
        }
    }

    private void validateStagingConfiguration() {
        logger.info("Validating staging configuration:");
        
        if (!apiKey.startsWith(STAGING_API_KEY_PREFIX)) {
            logger.warn("⚠️  WARNING: Non-staging API key in staging environment!");
            logger.warn("   Expected: {}* (staging key)", STAGING_API_KEY_PREFIX);
            logger.warn("   Actual: {}", apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey);
        } else {
            logger.info("✅ API key format validation passed: staging key in staging environment");
        }
        
        logger.info("✅ Configuration validation passed");
        logger.info("   Environment: staging");
        logger.info("   API Key: {} format ✓", apiKey.startsWith(STAGING_API_KEY_PREFIX) ? STAGING_API_KEY_PREFIX + "*" : "non-staging");
        logger.info("   Frontend: {} ✓", frontendBaseUrl);
        logger.info("   Dashboard URL: {} ✓", getFrontendDashboardUrl());
        logger.info("   Login URL: {} ✓", getFrontendLoginUrl());
        
        logger.info("Staging fallback configuration:");
        logger.info("   Email: {}", stagingFallbackEmail);
        logger.info("   Name: {} {}", stagingFallbackFirstName, stagingFallbackLastName);
        logger.info("   Organization: {}", stagingFallbackOrgName);
        logger.info("   Role: {}", stagingFallbackRole);
    }

    private void validateProductionConfiguration() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Production environment requires a valid API key");
        }

        // Check if it's a base64-encoded placeholder (staging key in production)
        if (isBase64EncodedPlaceholder(apiKey)) {
            throw new IllegalStateException(
                "Production environment detected with staging/placeholder API key. " +
                "Please provide a real WorkOS API key for production use."
            );
        }

        logger.info("Production API key validation passed");
    }

    private boolean isBase64EncodedPlaceholder(String key) {
        if (key == null || !key.startsWith(STAGING_API_KEY_PREFIX)) {
            return false;
        }

        try {
            String keyPart = key.substring(STAGING_API_KEY_PREFIX.length()); // Remove staging prefix
            byte[] decoded = Base64.getDecoder().decode(keyPart);
            String decodedStr = new String(decoded);
            
            // Check for placeholder patterns
            return decodedStr.contains("key_") && decodedStr.contains(",");
        } catch (Exception e) {
            return false; // Not base64, likely a real key
        }
    }

    // Environment detection methods
    public boolean isStagingEnvironment() {
        boolean isStaging = "staging".equalsIgnoreCase(environment);
        logger.info("isStagingEnvironment() - environment: '{}', isStaging: {}", environment, isStaging);
        return isStaging;
    }

    public boolean isProductionEnvironment() {
        return "production".equalsIgnoreCase(environment);
    }

    // Getters
    public String getEnvironment() {
        return environment;
    }

    public String getClientId() {
        return clientId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getRedirectUri() {
        return REDIRECT_URI;
    }

    // Staging configuration getters
    public String getStagingFallbackEmail() {
        return stagingFallbackEmail;
    }

    public String getStagingFallbackFirstName() {
        return stagingFallbackFirstName;
    }

    public String getStagingFallbackLastName() {
        return stagingFallbackLastName;
    }

    public String getStagingFallbackRole() {
        return stagingFallbackRole;
    }

    public String getStagingFallbackOrgName() {
        return stagingFallbackOrgName;
    }

    public String getStagingFallbackOrgId() {
        return stagingFallbackOrgId;
    }

    public String getStagingFallbackConnectionId() {
        return stagingFallbackConnectionId;
    }

    // API endpoints getters
    public String getStagingBaseUrl() { return stagingBaseUrl; }
    public String getProductionBaseUrl() { return productionBaseUrl; }
    public String getCurrentBaseUrl() {
        String baseUrl = isStagingEnvironment() ? stagingBaseUrl : productionBaseUrl;
        logger.info("getCurrentBaseUrl() - Environment: {}, isStaging: {}, baseUrl: {}", 
                   environment, isStagingEnvironment(), baseUrl);
        return baseUrl;
    }

    // Frontend configuration getters
    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public String getFrontendDashboardPath() { return frontendDashboardPath; }
    public String getFrontendLoginPath() { return frontendLoginPath; }
    public String getSessionPassword() { return sessionPassword; }

    public String getFrontendDashboardUrl() {
        return frontendBaseUrl + frontendDashboardPath;
    }

    public String getFrontendLoginUrl() {
        return frontendBaseUrl + frontendLoginPath;
    }

    // Production configuration getters
    public boolean isProductionValidateApiKey() {
        return productionValidateApiKey;
    }

    public boolean isProductionRequireRealProfiles() {
        return productionRequireRealProfiles;
    }

    /**
     * Generate the authorization URL - Complete manual approach
     * This completely bypasses the WorkOS SDK to ensure we use the correct staging URL
     */
    public String getAuthorizationUrl() {
        logger.info("=== Generating WorkOS Authorization URL ===");
        
        // Force staging base URL regardless of environment detection
        String baseUrl = "https://api.workos.dev";
        // Use connection ID for direct SAML SSO (bypasses AuthKit)
        String connectionIdValue = this.connectionId; // Use connection ID from config
        
        // Use 'connection' parameter for direct SAML SSO (bypasses AuthKit)
        String authUrl = String.format(
            "%s/sso/authorize?response_type=code&client_id=%s&redirect_uri=%s&connection=%s",
            baseUrl, clientId, REDIRECT_URI, connectionIdValue
        );
        
        logger.info("✅ Generated Authorization URL with connection parameter (direct SAML): {}", authUrl);
        logger.info("   Base URL: {}", baseUrl);
        logger.info("   Connection ID: {}", connectionIdValue);
        logger.info("   Client ID: {}", clientId);
        logger.info("   Redirect URI: {}", REDIRECT_URI);
        
        return authUrl;
    }

    /**
     * Fallback manual URL building (previous approach)
     */
    private String getFallbackAuthorizationUrl() {
        String baseUrl = getCurrentBaseUrl();
        
        if (isStagingEnvironment()) {
            String authUrl = String.format(
                "%s/sso/authorize?response_type=code&client_id=%s&redirect_uri=%s&organization=%s",
                baseUrl, clientId, REDIRECT_URI, "org_test_idp"
            );
            
            logger.info("⚠️  Using fallback manual URL building for staging");
            logger.info(AUTH_URL_LOG_MESSAGE, authUrl);
            return authUrl;
        } else {
            String authUrl = String.format(
                "%s/sso/authorize?response_type=code&client_id=%s&redirect_uri=%s&organization=%s",
                baseUrl, clientId, REDIRECT_URI, getOrganizationIdForProduction()
            );
            
            logger.info(AUTH_URL_LOG_MESSAGE, authUrl);
            return authUrl;
        }
    }

    /**
     * Get organization ID for production environment
     */
    private String getOrganizationIdForProduction() {
        // This will be set when you have production account
        String orgId = System.getenv("WORKOS_ORGANIZATION_ID");
        return orgId != null ? orgId : connectionId; // Fallback to connection ID
    }
}