package com.IT_market.controller;

import com.IT_market.dto.*;
import com.IT_market.model.User;
import com.IT_market.security.JwtTokenUtil;
import com.IT_market.security.SessionManager;
import com.IT_market.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ui.Model;

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        try {
            // Validate input
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(errors);
            }
            
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
                    user.isBusinessAccount()
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
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
        try {
            // Validate input
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(errors);
            }
            
            User user = userService.registerUser(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("accountType", request.getAccountType());
            
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
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && !token.isEmpty()) {
            sessionManager.invalidateSession(token);
        }
        
        System.out.println("User logged out");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && !token.isEmpty() && sessionManager.isValidSession(token)) {
            User user = sessionManager.getUserFromSession(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("businessAccount", user.isBusinessAccount());
            response.put("roles", user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toArray(String[]::new));
            
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", false);
        response.put("message", "Invalid or expired token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @GetMapping("/login")
    public String login() {
        return "redirect:/";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "redirect:/";
    }
}