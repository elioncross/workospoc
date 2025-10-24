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
        users.put("admin", new UserPrincipal("admin", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "SMA"));
        users.put("manager", new UserPrincipal("manager", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "MA"));
        users.put("user", new UserPrincipal("user", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "MC"));
        users.put("support", new UserPrincipal("support", "$2a$10$i83EBAsRLLxlbamcE5UHn.ZmQ5TOgsbG5RpKyKFwMnWewQCFcu/Ja", "corp1", "SU"));
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
