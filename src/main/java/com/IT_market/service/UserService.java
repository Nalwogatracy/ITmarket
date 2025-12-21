package com.IT_market.service;

import com.IT_market.dto.RegisterRequest;
import com.IT_market.model.Role;
import com.IT_market.model.User;
import com.IT_market.repository.RoleRepository;
import com.IT_market.repository.UserRepository;
import com.IT_market.security.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public User registerUser(RegisterRequest request) {
        System.out.println("=== REGISTER USER ===");
        System.out.println("First Name: " + request.getFirstName());
        System.out.println("Last Name: " + request.getLastName());
        System.out.println("Username: " + request.getUsername());
        System.out.println("Email: " + request.getEmail());
        System.out.println("Account Type: " + request.getAccountType());
        System.out.println("Seller Type: " + request.getSellerType());
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username '" + request.getUsername() + "' already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email '" + request.getEmail() + "' already registered");
        }
        
        // Check if passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        System.out.println("Passwords match OK");
        
        // Create user - Use correct constructor
        User user = new User(
            request.getFirstName(),
            request.getLastName(),
            request.getUsername(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword())
        );
        System.out.println("User created. Password encoded: " + (user.getPassword() != null));
        
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEnabled(true); // CORRECTED: Use enabled instead of active
        
        // Handle account type
        String accountType = request.getAccountType() != null ? 
                request.getAccountType().toUpperCase() : "BUYER";
        
        // Set the role field
        user.setRole(accountType);
        
        // Set up roles based on account type
        Set<Role> roles = new HashSet<>();
        
        if ("SELLER".equals(accountType)) {
            // Seller account
            Role sellerRole = roleRepository.findByName(Role.RoleName.ROLE_SELLER)
                .orElseGet(() -> {
                    Role newRole = new Role(Role.RoleName.ROLE_SELLER);
                    return roleRepository.save(newRole);
                });
            roles.add(sellerRole);
            
            // Set seller-specific fields
            user.setSellerType(request.getSellerType());
            
            if ("BUSINESS".equalsIgnoreCase(request.getSellerType())) {
                user.setCompanyName(request.getCompanyName());
                user.setBusinessType(request.getBusinessType());
                user.setTaxId(request.getTaxId());
                user.setBusinessAddress(request.getBusinessAddress());
            } else if ("INDIVIDUAL".equalsIgnoreCase(request.getSellerType())) {
                // Individual seller - optional company name or use their name
                user.setCompanyName(request.getCompanyName() != null ? 
                    request.getCompanyName() : user.getFullName() + " (Individual)");
            }
            
            System.out.println("Registering as SELLER, type: " + request.getSellerType());
            
        } else {
            // Buyer account (default)
            Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseGet(() -> {
                    Role newRole = new Role(Role.RoleName.ROLE_USER);
                    return roleRepository.save(newRole);
                });
            roles.add(userRole);
            user.setRole("BUYER"); // Set role field
            
            System.out.println("Registering as BUYER");
        }
        
        user.setRoles(roles);
        
        // Save user
        User savedUser = userRepository.save(user);
        System.out.println("User registered successfully with ID: " + savedUser.getId());
        
        return savedUser;
    }
    
    @Transactional
    public User authenticateUser(String username, String password) {
        System.out.println("=== AUTHENTICATE USER ===");
        System.out.println("Username/Email: " + username);
        
        // Try to find by username first, then by email
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Invalid username or password")));
        
        System.out.println("User found: " + user.getUsername());
        System.out.println("User enabled: " + user.isEnabled()); // CORRECTED: Use isEnabled()
        
        if (!user.isEnabled()) { // CORRECTED: Use isEnabled()
            throw new RuntimeException("Account is deactivated");
        }
        
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        System.out.println("Password matches: " + passwordMatches);
        
        if (!passwordMatches) {
            throw new RuntimeException("Invalid password");
        }
        
        // Update last login would need to be added to User entity
        // user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        System.out.println("Authentication successful!");
        return user;
    }
    
    // Simple check methods
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setEnabled(!user.isEnabled()); // CORRECTED: Use isEnabled() and setEnabled()
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    // Add a method to get user by token for AdminController
    public User getUserFromToken(String token) {
        // This would normally decode JWT token, but for now we'll use a simple lookup
        // In a real app, you'd use JwtTokenUtil to decode the token
        return userRepository.findByUsername("admin")
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}