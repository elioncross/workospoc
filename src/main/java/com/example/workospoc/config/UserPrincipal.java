package com.example.workospoc.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    private String username;
    private String password;
    private String corpId;
    private String role;
    private Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String username, String password, String corpId, String role) {
        this.username = username;
        this.password = password;
        this.corpId = corpId;
        this.role = role;
        this.authorities = mapRoleToAuthorities(role);
    }
    
    private Collection<? extends GrantedAuthority> mapRoleToAuthorities(String role) {
        // Map new system roles to Spring Security authorities
        switch (role) {
            case "org_super": // Super Manager Admin
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "org_managerplus": // Manager Plus
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"));
            case "org_manager": // Manager
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"));
            case "org_support": // Support
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPPORT"));
            case "org_user": // Regular User
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            // Legacy role support (for backward compatibility)
            case "SMA": // Super Manager Admin (legacy)
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "MA": // Manager (legacy)
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"));
            case "MC": // Member/Customer (legacy)
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            case "SU": // Support (legacy)
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPPORT"));
            default:
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getCorpId() {
        return corpId;
    }

    public String getRole() {
        return role;
    }
}
