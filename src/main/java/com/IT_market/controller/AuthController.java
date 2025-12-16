package com.IT_market.controller;

import com.IT_market.dto.LoginRequest;
import com.IT_market.dto.LoginResponse;
import com.IT_market.dto.RegisterRequest;
import com.IT_market.model.User;
import com.IT_market.security.JwtTokenUtil;
import com.IT_market.security.SessionManager;
import com.IT_market.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    private final SessionManager sessionManager;
    private final JwtTokenUtil jwtTokenUtil;
    
    public AuthController(UserService userService, SessionManager sessionManager, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.sessionManager = sessionManager;
        this.jwtTokenUtil = jwtTokenUtil;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.authenticateUser(request.getUsername(), request.getPassword());
            String token = sessionManager.createSession(user);
            
            String[] roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toArray(String[]::new);
            
            LoginResponse response = new LoginResponse(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    roles,
                    user.isBusiness()
            );
            
            System.out.println("User logged in successfully: " + user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.err.println("Login failed: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            
            System.out.println("User registered successfully: " + user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            System.err.println("Registration failed: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        sessionManager.invalidateSession(token);
        
        System.out.println("User logged out");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        if (sessionManager.isValidSession(token)) {
            User user = sessionManager.getUserFromSession(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", false);
        response.put("message", "Invalid or expired token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}