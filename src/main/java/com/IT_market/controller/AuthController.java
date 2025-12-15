package com.IT_market.controller;

import com.IT_market.dto.LoginRequest;
import com.IT_market.dto.RegisterRequest;
import com.IT_market.dto.UserResponse;
import com.IT_market.model.User;
import com.IT_market.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin // allow React later
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse register(@RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return mapToResponse(user);
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request) {
        User user = authService.login(request);
        return mapToResponse(user);
    }

    // 🔒 NEVER return User entity directly
    private UserResponse mapToResponse(User user) {
        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole().name());
        return res;
    }
}
