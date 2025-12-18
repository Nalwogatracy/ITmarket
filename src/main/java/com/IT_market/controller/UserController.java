package com.IT_market.controller;

import com.IT_market.dto.RegisterRequest;
import com.IT_market.model.User;
import com.IT_market.security.SessionManager;
import com.IT_market.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/users")
public class UserController {
    
    private final UserService userService;
    private final SessionManager sessionManager;
    
    public UserController(UserService userService, SessionManager sessionManager) {
        this.userService = userService;
        this.sessionManager = sessionManager;
    }
    
    // Show login page (handled by JavaScript in index.html)
    @GetMapping("/login")
    public String showLoginPage() {
        return "redirect:/#loginModal";
    }
    
    // Show registration page (handled by JavaScript in index.html)
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "redirect:/#registerModal";
    }
    
    // Process registration form (alternative to API)
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletResponse response) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", getErrorsFromBindingResult(bindingResult));
            redirectAttributes.addFlashAttribute("registerRequest", request);
            return "redirect:/#registerModal";
        }
        
        try {
            // Register user
            User user = userService.registerUser(request);
            
            // Auto-login after registration
            String token = sessionManager.createSession(user);
            
            // Set token as cookie
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("auth_token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(cookie);
            
            // Set success message
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Welcome to TechSphere.");
            
            // Redirect based on account type
            if ("SELLER".equals(request.getAccountType())) {
                return "redirect:/seller/dashboard";
            } else {
                return "redirect:/products";
            }
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", request);
            return "redirect:/#registerModal";
        }
    }
    
    // Show profile page
    @GetMapping("/profile")
    public String showProfile(HttpServletRequest request, Model model) {
        String token = extractTokenFromRequest(request);
        
        if (token == null || !sessionManager.isValidSession(token)) {
            return "redirect:/users/login";
        }
        
        User user = sessionManager.getUserFromSession(token);
        model.addAttribute("user", user);
        return "profile";
    }
    
    // Update profile
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User user,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        
        String token = extractTokenFromRequest(request);
        if (token == null || !sessionManager.isValidSession(token)) {
            return "redirect:/users/login";
        }
        
        try {
            User currentUser = sessionManager.getUserFromSession(token);
            // Update user logic here
            // userService.updateUser(currentUser.getId(), user);
            
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
        }
        
        return "redirect:/users/profile";
    }
    
    // Logout
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractTokenFromRequest(request);
        
        if (token != null) {
            sessionManager.invalidateSession(token);
        }
        
        // Clear cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("auth_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        // Invalidate session
        request.getSession().invalidate();
        
        return "redirect:/?logout";
    }
    
    // Show orders page
    @GetMapping("/orders")
    public String showOrders(HttpServletRequest request, Model model) {
        String token = extractTokenFromRequest(request);
        
        if (token == null || !sessionManager.isValidSession(token)) {
            return "redirect:/users/login";
        }
        
        User user = sessionManager.getUserFromSession(token);
        model.addAttribute("user", user);
        // Add orders to model
        // model.addAttribute("orders", orderService.getUserOrders(user.getId()));
        
        return "orders";
    }
    
    private Map<String, String> getErrorsFromBindingResult(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Check cookie
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // Check session
        Object tokenAttr = request.getSession().getAttribute("auth_token");
        if (tokenAttr != null) {
            return tokenAttr.toString();
        }
        
        return null;
    }
}