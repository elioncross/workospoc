package com.example.workospoc.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // In-memory user store for demo purposes
    private final Map<String, UserPrincipal> users = new HashMap<>();

    public CustomUserDetailsService() {
        // Initialize demo users with pre-encoded BCrypt passwords
        // BCrypt hash for "password" is: $2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja
        // Using role codes as usernames to match frontend display
        users.put("org_super", new UserPrincipal("org_super", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_super"));
        users.put("org_managerplus", new UserPrincipal("org_managerplus", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_managerplus"));
        users.put("org_manager", new UserPrincipal("org_manager", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_manager"));
        users.put("org_support", new UserPrincipal("org_support", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_support"));
        users.put("org_user", new UserPrincipal("org_user", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_user"));
        
        // Legacy username support for backward compatibility
        users.put("admin", new UserPrincipal("admin", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_super"));
        users.put("manager", new UserPrincipal("manager", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_managerplus"));
        users.put("user", new UserPrincipal("user", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_manager"));
        users.put("support", new UserPrincipal("support", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "org_support"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserPrincipal user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }
}
