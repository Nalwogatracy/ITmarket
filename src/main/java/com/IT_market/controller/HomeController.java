package com.IT_market.controller;

import com.IT_market.model.User;
import com.IT_market.security.SessionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    private final SessionManager sessionManager;
    
    public HomeController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        // Get token from cookie or request
        String token = extractTokenFromRequest(request);
        
        if (token != null && sessionManager.isValidSession(token)) {
            User user = sessionManager.getUserFromSession(token);
            model.addAttribute("user", user);
            
            // Add role-specific attributes
            boolean isSeller = user.getRoles().stream()
                .anyMatch(role -> "SELLER".equals(role.getName().name()) || "ADMIN".equals(role.getName().name()));
            
            boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName().name()));
            
            model.addAttribute("isSeller", isSeller);
            model.addAttribute("isAdmin", isAdmin);
        }
        
        return "index";
    }
    
    @GetMapping("/products")
    public String products(HttpServletRequest request, Model model) {
        // Same logic to check user session
        String token = extractTokenFromRequest(request);
        
        if (token != null && sessionManager.isValidSession(token)) {
            User user = sessionManager.getUserFromSession(token);
            model.addAttribute("user", user);
        }
        
        return "index"; // Same page, JavaScript handles product display
    }
    
    @GetMapping("/profile")
    public String profile(HttpServletRequest request, Model model) {
        String token = extractTokenFromRequest(request);
        
        if (token == null || !sessionManager.isValidSession(token)) {
            return "redirect:/";
        }
        
        User user = sessionManager.getUserFromSession(token);
        model.addAttribute("user", user);
        return "profile"; // You'll need a profile.html template
    }
    
    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        String token = extractTokenFromRequest(request);
        
        if (token == null || !sessionManager.isValidSession(token)) {
            return "redirect:/";
        }
        
        User user = sessionManager.getUserFromSession(token);
        model.addAttribute("user", user);
        
        // Check role and redirect accordingly
        boolean isSeller = user.getRoles().stream()
            .anyMatch(role -> "SELLER".equals(role.getName().name()));
        
        boolean isAdmin = user.getRoles().stream()
            .anyMatch(role -> "ADMIN".equals(role.getName().name()));
        
        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isSeller) {
            return "redirect:/seller/dashboard";
        } else {
            return "redirect:/products";
        }
    }
    
    
    @GetMapping("/seller/dashboard")
    public String sellerDashboard(HttpServletRequest request, Model model) {
        String token = extractTokenFromRequest(request);
        
        if (token == null || !sessionManager.isValidSession(token)) {
            return "redirect:/";
        }
        
        User user = sessionManager.getUserFromSession(token);
        boolean isSeller = user.getRoles().stream()
            .anyMatch(role -> "SELLER".equals(role.getName().name()) || "ADMIN".equals(role.getName().name()));
        
        if (!isSeller) {
            return "redirect:/";
        }
        
        model.addAttribute("user", user);
        return "seller/dashboard"; // You'll need this template
    }
    
    @GetMapping("/cart")
    public String cart(HttpServletRequest request, Model model) {
        String token = extractTokenFromRequest(request);
        
        if (token != null && sessionManager.isValidSession(token)) {
            User user = sessionManager.getUserFromSession(token);
            model.addAttribute("user", user);
        }
        
        return "cart"; // You'll need a cart.html template
    }
    
    @GetMapping("/checkout")
    public String checkout(HttpServletRequest request, Model model) {
        String token = extractTokenFromRequest(request);
        
        if (token != null && sessionManager.isValidSession(token)) {
            User user = sessionManager.getUserFromSession(token);
            model.addAttribute("user", user);
        }
        
        return "checkout"; // You'll need a checkout.html template
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Check Authorization header first
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
        
        // Check session attribute
        Object tokenAttr = request.getSession().getAttribute("auth_token");
        if (tokenAttr != null) {
            return tokenAttr.toString();
        }
        
        return null;
    }
}