package com.example.workospoc.controller;

import com.example.workospoc.config.WorkOSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public void initiateSSO(HttpServletResponse response) throws IOException {
        String authUrl = workOSConfig.getAuthorizationUrl();
        logger.info("Redirecting to WorkOS SSO: {}", authUrl);
        response.sendRedirect(authUrl);
    }
}