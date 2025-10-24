package com.example.workospoc.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import com.workos.sso.models.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:mySecretKey}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(Authentication authentication, String corpId, String role) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .claim("corpId", corpId)
                .claim("role", role)
                .claim("authorities", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateTokenForWorkOSUser(String email, String role) {
        return generateTokenForWorkOSUser(email, role, null);
    }

    public String generateTokenForWorkOSUser(String email, String role, Profile profile) {
        Claims claims = Jwts.claims()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs));
        
        claims.put("role", role);
        claims.put("source", "workos");
        
        // Add profile information if available
        if (profile != null) {
            claims.put("profileId", profile.id);
            claims.put("connectionId", profile.connectionId);
            claims.put("connectionType", profile.connectionType);
            
            if (profile.firstName != null && !profile.firstName.trim().isEmpty()) {
                claims.put("firstName", profile.firstName);
            }
            if (profile.lastName != null && !profile.lastName.trim().isEmpty()) {
                claims.put("lastName", profile.lastName);
            }
            
            // Add raw attributes if available - this contains SAML attributes
            if (profile.rawAttributes != null && !profile.rawAttributes.isEmpty()) {
                claims.put("rawAttributes", profile.rawAttributes);
                
                // Log the raw attributes for debugging
                logger.info("WorkOS Profile raw attributes: {}", profile.rawAttributes);
            }
            
            // Add organization ID if available
            if (profile.organizationId != null && !profile.organizationId.trim().isEmpty()) {
                claims.put("organizationId", profile.organizationId);
            }
        }
        
        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateTokenForWorkOSUserStaging(String email, String role) {
        Claims claims = Jwts.claims()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs));
        
        claims.put("role", role);
        claims.put("source", "workos");
        
        // Add mock staging attributes that would normally come from SAML
        // Extract name from email for more realistic staging experience
        if (email.contains("@")) {
            String localPart = email.substring(0, email.indexOf("@"));
            String[] nameParts = localPart.split("\\.");
            if (nameParts.length >= 2) {
                claims.put("firstName", capitalizeFirst(nameParts[0]));
                claims.put("lastName", capitalizeFirst(nameParts[1]));
            } else {
                claims.put("firstName", capitalizeFirst(localPart));
                claims.put("lastName", "Staging");
            }
        } else {
            claims.put("firstName", "John");
            claims.put("lastName", "Doe");
        }
        
        // Add mock organizational attributes
        claims.put("profileId", "staging_profile_" + System.currentTimeMillis());
        claims.put("connectionId", "conn_staging_test_001");
        claims.put("connectionType", "saml");
        claims.put("organizationId", "staging_org_001");
        
        logger.info("Generated staging JWT token for WorkOS user: {} with mock attributes", email);
        
        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String generateTokenForWorkOSUserStaging(String email, String role, 
                                                   String firstName, String lastName,
                                                   String organizationName, String organizationId,
                                                   String connectionId) {
        Claims claims = Jwts.claims()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs));
        
        claims.put("role", role);
        claims.put("source", "workos");
        
        // Add provided user attributes
        claims.put("firstName", firstName);
        claims.put("lastName", lastName);
        
        // Add provided organizational attributes
        claims.put("profileId", "staging_profile_" + System.currentTimeMillis());
        claims.put("connectionId", connectionId);
        claims.put("connectionType", "saml");
        claims.put("organizationId", organizationId);
        claims.put("organizationName", organizationName);
        
        logger.info("Generated dynamic staging JWT token for WorkOS user: {} ({} {}) from org: {}", 
                   email, firstName, lastName, organizationName);
        
        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getClaimFromToken(String token, String claimName) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get(claimName, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }
}
