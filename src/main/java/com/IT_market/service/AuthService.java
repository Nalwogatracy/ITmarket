package com.IT_market.service;

import com.IT_market.PasswordEncoder;
import com.IT_market.dto.LoginRequest;
import com.IT_market.dto.RegisterRequest;
import com.IT_market.model.User;
import com.IT_market.model.User.Role;
import com.IT_market.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    public User register(RegisterRequest req) {

        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.CUSTOMER);

        return userRepo.save(user);
    }

    public User login(LoginRequest req) {

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }

    public void checkAdmin(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Admin access required");
        }
    }
}
