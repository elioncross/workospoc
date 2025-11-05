package com.example.workospoc.controller;

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

    // No WorkOSConfig needed - SP-initiated flow is disabled

    /**
     * SP-initiated SSO endpoint - DISABLED
     * 
     * This application only supports IdP-initiated SSO flows.
     * Users should initiate SSO from their IdP dashboard (Okta, Azure AD, etc.).
     * WorkOS will automatically route to the correct connection based on the SAML assertion.
     */
    @GetMapping("/sso/workos")
    public void initiateSSO(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.warn("SP-initiated SSO flow is not supported. Only IdP-initiated flows are enabled.");
        logger.warn("Users should initiate SSO from their IdP dashboard instead.");
        response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, 
            "SP-initiated SSO is not supported. Please initiate SSO from your IdP dashboard.");
    }
    
    /**
     * Test endpoint - DISABLED (SP-initiated flow not supported)
     */
    @GetMapping("/test/url")
    public String testUrl() {
        logger.warn("SP-initiated SSO test endpoint called - not supported");
        return "SP-initiated SSO is not supported. This application only supports IdP-initiated flows.";
    }
}