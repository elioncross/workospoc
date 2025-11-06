package com.example.workospoc.config;

import com.workos.WorkOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WorkOSConfig {

    private static final Logger logger = LoggerFactory.getLogger(WorkOSConfig.class);
    
    // Constants
    private static final String REDIRECT_URI = "http://localhost:8081/auth/workos/callback";

    @Value("${workos.environment:staging}")
    private String environment;

    @Value("${workos.api-key}")
    private String apiKey;

    @Value("${workos.client-id}")
    private String clientId;

    /**
     * Connection ID - kept for reference/logging only
     * 
     * Note: For IdP-initiated flows, this is NOT needed for authorization URLs.
     * WorkOS automatically determines the connection from the SAML assertion.
     * The connection ID is provided by WorkOS in the profile object after authentication.
     */
    @Value("${workos.connection-id:conn_01K841NX4X1AM7TCQ22J7RFDYR}")
    private String connectionId;

    @Autowired
    private Environment springEnvironment;

    /**
     * Connection to CorpId Mapping
     * Maps WorkOS connection IDs to internal corpId (account ID)
     * Each customer has their own WorkOS connection to their IdP
     */
    private Map<String, String> connectionToCorpIdMapping;

    /**
     * Connection to Logo Mapping
     * Maps WorkOS connection IDs to IdP logo filenames
     */
    private Map<String, String> connectionToLogoMapping;

    /**
     * Connection to IdP Name Mapping
     * Maps WorkOS connection IDs to IdP display names
     */
    private Map<String, String> connectionToNameMapping;

    /**
     * Initialize and load connection mapping from YAML after bean construction
     */
    @PostConstruct
    public void initConnectionMapping() {
        logger.info("=== Connection Mapping Initialization ===");
        
        connectionToCorpIdMapping = new HashMap<>();
        
        // Load connection mappings from environment properties
        // Spring Boot converts YAML map to properties like: workos.connection-mapping.conn_01...=CORP_PROD_001
        String prefix = "workos.connection-mapping.";
        
        // Get all property keys that start with the prefix
        // We'll check for common connection IDs or iterate through all properties
        // Since we know the connection IDs, let's try loading them directly
        
        // Try to load known connection IDs from configuration
        String[] knownConnections = {
            "conn_01K8R9BKTPJWV123532JYJ5T6H",  // Okta
            "conn_01K953TWV92J9M1F1J0CR85QB6"    // Azure Entra ID
        };
        
        for (String connId : knownConnections) {
            String propertyKey = prefix + connId;
            String corpId = springEnvironment.getProperty(propertyKey);
            if (corpId != null && !corpId.isEmpty()) {
                connectionToCorpIdMapping.put(connId, corpId);
                logger.info("  Loaded: {} -> {}", connId, corpId);
            } else {
                logger.warn("  Could not load property: {} (value was null or empty)", propertyKey);
            }
        }
        
        // Also try to discover any other connection mappings dynamically
        // This is a fallback to catch any we might have missed
        try {
            // Get all property keys and filter for our prefix
            // Note: This approach works but may not catch all nested properties
            // A more robust approach would use @ConfigurationProperties, but this should work for POC
            if (springEnvironment instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) springEnvironment;
                for (org.springframework.core.env.PropertySource<?> ps : configurableEnv.getPropertySources()) {
                    if (ps.getSource() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sourceMap = (Map<String, Object>) ps.getSource();
                        for (String key : sourceMap.keySet()) {
                            if (key.startsWith(prefix)) {
                                String connId = key.substring(prefix.length());
                                String corpId = springEnvironment.getProperty(key);
                                if (corpId != null && !corpId.isEmpty() && !connectionToCorpIdMapping.containsKey(connId)) {
                                    connectionToCorpIdMapping.put(connId, corpId);
                                    logger.info("  Discovered: {} -> {}", connId, corpId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not discover additional connection mappings dynamically: {}", e.getMessage());
        }
        
        if (!connectionToCorpIdMapping.isEmpty()) {
            logger.info("✅ Connection mapping loaded successfully!");
            logger.info("Map size: {}", connectionToCorpIdMapping.size());
            logger.info("Map contents:");
            connectionToCorpIdMapping.forEach((key, value) -> 
                logger.info("  {} -> {}", key, value));
        } else {
            logger.error("❌ Connection mapping is EMPTY!");
            logger.error("   Could not load any connection mappings from application.yml");
            logger.error("   Check that workos.connection-mapping section exists in application.yml");
            logger.error("   Expected format:");
            logger.error("   workos:");
            logger.error("     connection-mapping:");
            logger.error("       conn_01K8R9BKTPJWV123532JYJ5T6H: CORP_PROD_001");
        }
        logger.info("========================================");
    }

    /**
     * Initialize and load connection logo mapping from YAML after bean construction
     */
    @PostConstruct
    public void initLogoMapping() {
        logger.info("=== Connection Logo Mapping Initialization ===");
        
        connectionToLogoMapping = new HashMap<>();
        
        // Load connection logo mappings from environment properties
        String prefix = "workos.connection-logos.";
        
        // Known connection IDs
        String[] knownConnections = {
            "conn_01K8R9BKTPJWV123532JYJ5T6H",  // Okta
            "conn_01K953TWV92J9M1F1J0CR85QB6"    // Azure Entra ID
        };
        
        for (String connId : knownConnections) {
            String propertyKey = prefix + connId;
            String logo = springEnvironment.getProperty(propertyKey);
            if (logo != null && !logo.isEmpty()) {
                connectionToLogoMapping.put(connId, logo);
                logger.info("  Loaded logo: {} -> {}", connId, logo);
            } else {
                logger.warn("  Could not load logo property: {} (value was null or empty)", propertyKey);
            }
        }
        
        // Also try to discover any other logo mappings dynamically
        try {
            if (springEnvironment instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) springEnvironment;
                for (org.springframework.core.env.PropertySource<?> ps : configurableEnv.getPropertySources()) {
                    if (ps.getSource() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sourceMap = (Map<String, Object>) ps.getSource();
                        for (String key : sourceMap.keySet()) {
                            if (key.startsWith(prefix)) {
                                String connId = key.substring(prefix.length());
                                String logo = springEnvironment.getProperty(key);
                                if (logo != null && !logo.isEmpty() && !connectionToLogoMapping.containsKey(connId)) {
                                    connectionToLogoMapping.put(connId, logo);
                                    logger.info("  Discovered logo: {} -> {}", connId, logo);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not discover additional logo mappings dynamically: {}", e.getMessage());
        }
        
        if (!connectionToLogoMapping.isEmpty()) {
            logger.info("✅ Connection logo mapping loaded successfully!");
            logger.info("Logo map size: {}", connectionToLogoMapping.size());
            logger.info("Logo map contents:");
            connectionToLogoMapping.forEach((key, value) -> 
                logger.info("  {} -> {}", key, value));
        } else {
            logger.warn("⚠️ Connection logo mapping is EMPTY!");
            logger.warn("   No logo mappings found in application.yml");
        }
        logger.info("========================================");
    }

    /**
     * Initialize and load connection name mapping from YAML after bean construction
     */
    @PostConstruct
    public void initNameMapping() {
        logger.info("=== Connection Name Mapping Initialization ===");
        
        connectionToNameMapping = new HashMap<>();
        
        // Load connection name mappings from environment properties
        String prefix = "workos.connection-names.";
        
        // Known connection IDs
        String[] knownConnections = {
            "conn_01K8R9BKTPJWV123532JYJ5T6H",  // Okta
            "conn_01K953TWV92J9M1F1J0CR85QB6"    // Azure Entra ID
        };
        
        for (String connId : knownConnections) {
            String propertyKey = prefix + connId;
            String name = springEnvironment.getProperty(propertyKey);
            if (name != null && !name.isEmpty()) {
                connectionToNameMapping.put(connId, name);
                logger.info("  Loaded name: {} -> {}", connId, name);
            } else {
                logger.warn("  Could not load name property: {} (value was null or empty)", propertyKey);
            }
        }
        
        // Also try to discover any other name mappings dynamically
        try {
            if (springEnvironment instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) springEnvironment;
                for (org.springframework.core.env.PropertySource<?> ps : configurableEnv.getPropertySources()) {
                    if (ps.getSource() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sourceMap = (Map<String, Object>) ps.getSource();
                        for (String key : sourceMap.keySet()) {
                            if (key.startsWith(prefix)) {
                                String connId = key.substring(prefix.length());
                                String name = springEnvironment.getProperty(key);
                                if (name != null && !name.isEmpty() && !connectionToNameMapping.containsKey(connId)) {
                                    connectionToNameMapping.put(connId, name);
                                    logger.info("  Discovered name: {} -> {}", connId, name);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not discover additional name mappings dynamically: {}", e.getMessage());
        }
        
        if (!connectionToNameMapping.isEmpty()) {
            logger.info("✅ Connection name mapping loaded successfully!");
            logger.info("Name map size: {}", connectionToNameMapping.size());
            logger.info("Name map contents:");
            connectionToNameMapping.forEach((key, value) -> 
                logger.info("  {} -> {}", key, value));
        } else {
            logger.warn("⚠️ Connection name mapping is EMPTY!");
            logger.warn("   No name mappings found in application.yml");
        }
        logger.info("========================================");
    }

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

    // Corp Mapping API Configuration
    @Value("${corp.mapping.api.base-url:http://localhost:8082}")
    private String corpMappingApiBaseUrl;

    @Value("${corp.mapping.api.key:}")
    private String corpMappingApiKey;

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

    public String getApiKey() {
        return apiKey;
    }

    public String getClientId() {
        return clientId;
    }

    /**
     * Get connection ID (for reference/logging only)
     * 
     * Note: This is not used for IdP-initiated flows. WorkOS provides
     * the connection ID in the profile object after authentication.
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Get corpId by WorkOS connection ID
     * 
     * @param connectionId WorkOS connection ID from profile
     * @return corpId if mapping exists, null otherwise
     */
    public String getCorpIdByConnectionId(String connectionId) {
        if (connectionId == null || connectionId.isEmpty()) {
            logger.debug("ConnectionId is null or empty, cannot lookup corpId");
            return null;
        }
        
        String corpId = connectionToCorpIdMapping != null ? 
            connectionToCorpIdMapping.get(connectionId) : null;
        
        if (corpId != null && !corpId.isEmpty()) {
            logger.debug("✅ Found corpId mapping: {} -> {}", connectionId, corpId);
            return corpId;
        }
        
        logger.warn("⚠️ No corpId mapping found for connectionId: {}", connectionId);
        logger.warn("   Available connection mappings: {}", 
                   connectionToCorpIdMapping != null ? connectionToCorpIdMapping.keySet() : "none");
        return null;
    }

    /**
     * Get logo filename by WorkOS connection ID
     * 
     * @param connectionId WorkOS connection ID from profile
     * @return logo filename if mapping exists, null otherwise
     */
    public String getLogoByConnectionId(String connectionId) {
        if (connectionId == null || connectionId.isEmpty()) {
            logger.debug("ConnectionId is null or empty, cannot lookup logo");
            return null;
        }
        
        String logo = connectionToLogoMapping != null ? 
            connectionToLogoMapping.get(connectionId) : null;
        
        if (logo != null && !logo.isEmpty()) {
            logger.debug("✅ Found logo mapping: {} -> {}", connectionId, logo);
            return logo;
        }
        
        logger.debug("⚠️ No logo mapping found for connectionId: {}", connectionId);
        return null;
    }

    /**
     * Get IdP display name by WorkOS connection ID
     * 
     * @param connectionId WorkOS connection ID from profile
     * @return IdP display name if mapping exists, null otherwise
     */
    public String getIdpNameByConnectionId(String connectionId) {
        if (connectionId == null || connectionId.isEmpty()) {
            logger.debug("ConnectionId is null or empty, cannot lookup IdP name");
            return null;
        }
        
        String name = connectionToNameMapping != null ? 
            connectionToNameMapping.get(connectionId) : null;
        
        if (name != null && !name.isEmpty()) {
            logger.debug("✅ Found IdP name mapping: {} -> {}", connectionId, name);
            return name;
        }
        
        logger.debug("⚠️ No IdP name mapping found for connectionId: {}", connectionId);
        return null;
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

    // Corp Mapping API getters
    public String getCorpMappingApiBaseUrl() {
        return corpMappingApiBaseUrl;
    }

    public String getCorpMappingApiKey() {
        return corpMappingApiKey;
    }

    /**
     * Generate the authorization URL - DEPRECATED
     * 
     * This method is no longer used since SP-initiated SSO flow has been removed.
     * This application only supports IdP-initiated flows where:
     * - Users initiate SSO from their IdP dashboard (Okta, Azure AD, etc.)
     * - WorkOS automatically determines the connection from the SAML assertion
     * - The app receives the authorization code via callback without needing connection ID
     * 
     * @deprecated SP-initiated flow is not supported. Only IdP-initiated flows are enabled.
     */
    @Deprecated
    public String getAuthorizationUrl() {
        logger.warn("getAuthorizationUrl() called - SP-initiated flow is not supported");
        logger.warn("This application only supports IdP-initiated SSO flows.");
        throw new UnsupportedOperationException(
            "SP-initiated SSO is not supported. Please use IdP-initiated flow by starting SSO from your IdP dashboard.");
    }
}