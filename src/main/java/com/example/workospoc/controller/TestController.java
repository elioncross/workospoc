package com.example.workospoc.controller;

import com.example.workospoc.config.WorkOSConfig;
import com.workos.WorkOS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    private final WorkOS workOS;
    private final WorkOSConfig workOSConfig;
    
    @Value("${workos.api-key}")
    private String apiKey;

    public TestController(WorkOS workOS, WorkOSConfig workOSConfig) {
        this.workOS = workOS;
        this.workOSConfig = workOSConfig;
    }

    @GetMapping("/api/test/workos")
    public Map<String, Object> testWorkOS() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Environment information
            result.put("environment", workOSConfig.getEnvironment());
            result.put("is_staging", workOSConfig.isStagingEnvironment());
            result.put("is_production", workOSConfig.isProductionEnvironment());
            
            // WorkOS instance status
            result.put("workos_instance", workOS != null ? "configured" : "null");
            result.put("status", "success");
            
            // Environment-specific configuration
            if (workOSConfig.isStagingEnvironment()) {
                Map<String, Object> stagingConfig = new HashMap<>();
                stagingConfig.put("fallback_email", workOSConfig.getStagingFallbackEmail());
                stagingConfig.put("fallback_role", workOSConfig.getStagingFallbackRole());
                stagingConfig.put("fallback_name", workOSConfig.getStagingFallbackFirstName() + " " + workOSConfig.getStagingFallbackLastName());
                stagingConfig.put("fallback_org", workOSConfig.getStagingFallbackOrgName());
                stagingConfig.put("fallback_org_id", workOSConfig.getStagingFallbackOrgId());
                result.put("staging_config", stagingConfig);
            } else if (workOSConfig.isProductionEnvironment()) {
                Map<String, Object> productionConfig = new HashMap<>();
                productionConfig.put("validate_api_key", workOSConfig.isProductionValidateApiKey());
                productionConfig.put("require_real_profiles", workOSConfig.isProductionRequireRealProfiles());
                result.put("production_config", productionConfig);
            }
            
            // Validate API key format
            Map<String, Object> keyValidation = validateApiKey(apiKey);
            result.put("api_key_validation", keyValidation);
            
            result.put("message", String.format("WorkOS configured for %s environment", workOSConfig.getEnvironment()));
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
        }
        
        return result;
    }

    private Map<String, Object> validateApiKey(String apiKey) {
        Map<String, Object> validation = new HashMap<>();
        
        if (apiKey == null || apiKey.isEmpty()) {
            validation.put("valid", false);
            validation.put("issue", "API key is null or empty");
            return validation;
        }
        
        if (!apiKey.startsWith("sk_test_") && !apiKey.startsWith("sk_live_")) {
            validation.put("valid", false);
            validation.put("issue", "API key does not start with sk_test_ or sk_live_");
            return validation;
        }
        
        // Check if the key part after sk_test_ is base64 encoded placeholder data
        try {
            String keyPart = apiKey.substring(8); // Remove "sk_test_" prefix
            byte[] decoded = Base64.getDecoder().decode(keyPart);
            String decodedStr = new String(decoded);
            
            validation.put("format", "base64_encoded");
            validation.put("decoded_preview", decodedStr.length() > 50 ? decodedStr.substring(0, 50) + "..." : decodedStr);
            
            // Check for common placeholder patterns
            if (decodedStr.contains("key_") && decodedStr.contains(",")) {
                validation.put("valid", false);
                validation.put("issue", "Appears to be base64-encoded placeholder data, not a real WorkOS API key");
                validation.put("recommendation", "Please obtain a real API key from your WorkOS dashboard at https://dashboard.workos.com/");
            } else {
                validation.put("valid", true);
                validation.put("note", "API key format appears valid, but actual authorization depends on WorkOS account");
            }
            
        } catch (Exception e) {
            // Not base64, might be a direct key
            validation.put("format", "direct_key");
            validation.put("valid", true);
            validation.put("note", "API key format appears to be a direct key (not base64 encoded)");
        }
        
        return validation;
    }

    @GetMapping("/api/test/workos/profile")
    public Map<String, Object> testWorkOSProfile() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test the getProfile method with a fake code to see what error we get
            // This will help us understand the "Unauthorized" error
            result.put("message", "Testing WorkOS getProfile method...");
            
            // This should fail but will give us insight into the error
            try {
                workOS.sso.getProfile("test_code_123");
                result.put("profile_test", "unexpected_success");
            } catch (Exception profileError) {
                result.put("profile_error", profileError.getMessage());
                result.put("profile_error_type", profileError.getClass().getSimpleName());
                result.put("profile_test", "failed_as_expected");
            }
            
            result.put("status", "success");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
        }
        
        return result;
    }
}