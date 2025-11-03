package com.example.workospoc.controller;

import com.example.workospoc.config.WorkOSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class WorkOSAuthController {

    private static final Logger logger = LoggerFactory.getLogger(WorkOSAuthController.class);

    private final WorkOSConfig workOSConfig;

    public WorkOSAuthController(WorkOSConfig workOSConfig) {
        this.workOSConfig = workOSConfig;
    }

    /**
     * Initiate SSO with WorkOS
     */
    @GetMapping("/sso/workos")
    public void initiateSSO(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.info("=== WorkOSAuthController.initiateSSO() called ===");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Request URL: {}", request.getRequestURL());
        logger.info("Request received at: {}", System.currentTimeMillis());
        
        try {
            String authUrl = workOSConfig.getAuthorizationUrl();
            logger.info("✅ Authorization URL generated successfully");
            logger.info("Authorization URL from WorkOSConfig: {}", authUrl);
            logger.info("Redirecting to WorkOS SSO: {}", authUrl);
            
            // Validate URL before redirecting
            if (authUrl == null || authUrl.isEmpty()) {
                logger.error("❌ ERROR: Authorization URL is null or empty!");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate SSO URL");
                return;
            }
            
            if (!authUrl.startsWith("http://") && !authUrl.startsWith("https://")) {
                logger.error("❌ ERROR: Invalid authorization URL format: {}", authUrl);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid SSO URL format");
                return;
            }
            
            response.sendRedirect(authUrl);
            logger.info("✅ Redirect response sent successfully");
            
        } catch (Exception e) {
            logger.error("❌ ERROR generating authorization URL: {}", e.getMessage(), e);
            logger.error("Exception type: {}", e.getClass().getName());
            logger.error("Stack trace:", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to generate SSO URL: " + e.getMessage());
        }
    }
    
    /**
     * Test endpoint to verify URL generation
     */
    @GetMapping("/test/url")
    public String testUrl() {
        logger.info("=== Test URL endpoint called ===");
        String authUrl = workOSConfig.getAuthorizationUrl();
        logger.info("Test URL: {}", authUrl);
        return authUrl;
    }
}