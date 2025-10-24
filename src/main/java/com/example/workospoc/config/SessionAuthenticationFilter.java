package com.example.workospoc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;

/**
 * Session-based authentication filter to handle SSO users alongside JWT authentication
 * Follows WorkOS example pattern of storing profile in session
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SessionAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Check for existing authentication (JWT filter runs first)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Check for SSO profile in session
            HttpSession session = request.getSession(false);
            if (session != null) {
                Boolean isAuthenticated = (Boolean) session.getAttribute("user_authenticated");
                String userEmail = (String) session.getAttribute("user_email");
                String userRole = (String) session.getAttribute("user_role");
                
                if (Boolean.TRUE.equals(isAuthenticated) && userEmail != null) {
                    logger.debug("🔐 Found SSO session authentication for user: {}", userEmail);
                    
                    // Create authentication from session
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userRole);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userEmail, 
                            null, 
                            Collections.singletonList(authority)
                        );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.debug("✅ SSO session authentication set for: {} with role: {}", userEmail, userRole);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}