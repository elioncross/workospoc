package com.example.workospoc.controller;

import com.example.workospoc.config.JwtUtil;
import com.example.workospoc.config.UserPrincipal;
import com.example.workospoc.config.WorkOSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WorkOSConfig workOSConfig;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            response.put("username", userPrincipal.getUsername());
            response.put("corpId", userPrincipal.getCorpId());
            response.put("role", userPrincipal.getRole());
            response.put("authenticated", true);
            
            // Extract additional WorkOS profile information from JWT token
            try {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwtToken = authHeader.substring(7);
                    String connectionId = jwtUtil.getClaimFromToken(jwtToken, "connectionId");
                    String firstName = jwtUtil.getClaimFromToken(jwtToken, "firstName");
                    String lastName = jwtUtil.getClaimFromToken(jwtToken, "lastName");
                    String connectionType = jwtUtil.getClaimFromToken(jwtToken, "connectionType");
                    String organizationId = jwtUtil.getClaimFromToken(jwtToken, "organizationId");
                    
                    // Add connection ID and related information
                    if (connectionId != null && !connectionId.isEmpty()) {
                        response.put("connectionId", connectionId);
                        
                        // Get logo from WorkOSConfig
                        String logo = workOSConfig.getLogoByConnectionId(connectionId);
                        if (logo != null && !logo.isEmpty()) {
                            response.put("idpLogo", logo);
                        }
                        
                        // Get IdP name from WorkOSConfig
                        String idpName = workOSConfig.getIdpNameByConnectionId(connectionId);
                        if (idpName != null && !idpName.isEmpty()) {
                            response.put("idpName", idpName);
                        }
                    }
                    
                    // Build full name from firstName and lastName
                    if (firstName != null || lastName != null) {
                        StringBuilder fullName = new StringBuilder();
                        if (firstName != null && !firstName.trim().isEmpty()) {
                            fullName.append(firstName.trim());
                        }
                        if (lastName != null && !lastName.trim().isEmpty()) {
                            if (fullName.length() > 0) {
                                fullName.append(" ");
                            }
                            fullName.append(lastName.trim());
                        }
                        if (fullName.length() > 0) {
                            response.put("fullName", fullName.toString());
                        }
                    }
                    
                    // Add connection type (e.g., "SAML")
                    if (connectionType != null && !connectionType.isEmpty()) {
                        response.put("connectionType", connectionType.toUpperCase());
                    }
                    
                    // Add organization ID
                    if (organizationId != null && !organizationId.isEmpty()) {
                        response.put("organizationId", organizationId);
                    }
                }
            } catch (Exception e) {
                // Log but don't fail the request if extraction fails
                logger.debug("Could not extract WorkOS profile information from JWT: {}", e.getMessage());
            }
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("Login attempt for user: " + loginRequest.getUsername());
            
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            // Get user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
            UserPrincipal userPrincipal = (UserPrincipal) userDetails;

            // Generate JWT token
            String token = jwtUtil.generateJwtToken(authentication, userPrincipal.getCorpId(), userPrincipal.getRole());

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", userPrincipal.getUsername());
            response.put("corpId", userPrincipal.getCorpId());
            response.put("role", userPrincipal.getRole());
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid username or password: " + e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
