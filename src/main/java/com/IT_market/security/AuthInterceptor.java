package com.IT_market.security;

import com.IT_market.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    private final SessionManager sessionManager;
    
    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        String path = request.getRequestURI();
        
        // Skip auth for public endpoints
        if (isPublicEndpoint(path)) {
            return true;
        }
        
        // Check if user is authenticated
        User user = sessionManager.getUserFromSession(token);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or expired token\"}");
            return false;
        }
        
        // Check admin access for admin endpoints
        if (isAdminEndpoint(path) && !isAdmin(user)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"Admin access required\"}");
            return false;
        }
        
        // Check seller access for seller endpoints
        if (isSellerEndpoint(path) && !isSeller(user)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"Seller access required\"}");
            return false;
        }
        
        // Set current user in thread local
        currentUser.set(user);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clear thread local
        currentUser.remove();
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") ||
               (path.startsWith("/api/products") && 
                (requestIsGet(path) || path.equals("/api/products"))) ||
               path.startsWith("/api/orders/guest") ||
               path.startsWith("/api/orders/by-email") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/api/health") ||
               path.startsWith("/uploads/") ||
               path.equals("/") ||
               path.startsWith("/admin/");
    }
    
    private boolean isAdminEndpoint(String path) {
        return path.startsWith("/api/admin/") ||
               path.startsWith("/admin/api/") ||
               path.matches(".*/api/orders/\\w+/status") ||
               (path.matches(".*/api/products/\\w+") && requestIsDelete(path));
    }
    
    private boolean isSellerEndpoint(String path) {
        return path.startsWith("/api/seller/") ||
               (path.startsWith("/api/products") && requestIsPostOrPut(path));
    }
    
    private boolean requestIsGet(String path) {
        return true; // Simplified - in real app, check request method
    }
    
    private boolean requestIsDelete(String path) {
        return false; // Simplified - in real app, check request method
    }
    
    private boolean requestIsPostOrPut(String path) {
        return false; // Simplified - in real app, check request method
    }
    
    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));
    }
    
    private boolean isSeller(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_SELLER"));
    }
    
    public static User getCurrentUser() {
        return currentUser.get();
    }
}