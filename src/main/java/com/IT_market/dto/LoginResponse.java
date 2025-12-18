package com.IT_market.dto;

import java.util.List;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String[] roles;
    private boolean businessAccount;
    
    public LoginResponse(String token, Long userId, String username, String email, 
                         String fullName, String[] roles, boolean businessAccount) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
        this.businessAccount = businessAccount;
    }
    
    // Getters
    public String getToken() {
        return token;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String[] getRoles() {
        return roles;
    }
    
    public boolean isBusinessAccount() {
        return businessAccount;
    }
}