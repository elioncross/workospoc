package com.example.workospoc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.getUserNameFromJwtToken(jwtToken);
            } catch (Exception e) {
                logger.error("Unable to get JWT Token or JWT Token has expired");
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            if (jwtUtil.validateJwtToken(jwtToken)) {
                UserDetails userDetails;
                
                // Check if this is a WorkOS token (has 'source' claim)
                String source = jwtUtil.getClaimFromToken(jwtToken, "source");
                if ("workos".equals(source)) {
                    // For WorkOS users, create anonymous UserDetails from JWT claims
                    String role = jwtUtil.getClaimFromToken(jwtToken, "role");
                    String firstName = jwtUtil.getClaimFromToken(jwtToken, "firstName");
                    String lastName = jwtUtil.getClaimFromToken(jwtToken, "lastName");
                    String organizationId = jwtUtil.getClaimFromToken(jwtToken, "organizationId");
                    String connectionId = jwtUtil.getClaimFromToken(jwtToken, "connectionId");
                    
                    // Create display name from available attributes
                    String displayName = username; // fallback to email
                    if (firstName != null && lastName != null) {
                        displayName = firstName + " " + lastName;
                    } else if (firstName != null) {
                        displayName = firstName;
                    }
                    
                    String corpId = organizationId != null ? organizationId : "workos-external";
                    
                    logger.info("Creating WorkOS user session for: " + displayName + " (" + username + 
                        "), Organization: " + corpId + ", Connection: " + connectionId);
                    
                    userDetails = new UserPrincipal(username, "", corpId, role != null ? role : "org_user");
                } else {
                    // For regular users, load from user store
                    userDetails = this.userDetailsService.loadUserByUsername(username);
                }
                
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        chain.doFilter(request, response);
    }
}
