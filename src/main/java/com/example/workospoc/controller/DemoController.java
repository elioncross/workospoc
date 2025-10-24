package com.example.workospoc.controller;

import com.example.workospoc.config.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminEndpoint(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is an admin-only endpoint");
        response.put("user", getCurrentUserInfo(authentication));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> managerEndpoint(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a manager endpoint");
        response.put("user", getCurrentUserInfo(authentication));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> userEndpoint(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a user endpoint");
        response.put("user", getCurrentUserInfo(authentication));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/support")
    public ResponseEntity<Map<String, Object>> supportEndpoint(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a support endpoint - accessible to all authenticated users");
        response.put("user", getCurrentUserInfo(authentication));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> getCurrentUserInfo(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            userInfo.put("username", userPrincipal.getUsername());
            userInfo.put("corpId", userPrincipal.getCorpId());
            userInfo.put("role", userPrincipal.getRole());
        }
        
        return userInfo;
    }
}
