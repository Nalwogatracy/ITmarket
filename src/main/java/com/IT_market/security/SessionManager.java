package com.IT_market.security;

import com.IT_market.model.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SessionManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenUtil jwtTokenUtil;
    
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_PREFIX = "user:";
    private static final long SESSION_TIMEOUT = 24 * 60 * 60; // 24 hours
    
    public SessionManager(RedisTemplate<String, Object> redisTemplate, JwtTokenUtil jwtTokenUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenUtil = jwtTokenUtil;
    }
    
    public String createSession(User user) {
        String token = jwtTokenUtil.generateToken(user);
        
        // Store user data in Redis
        String sessionKey = SESSION_PREFIX + token;
        String userKey = USER_PREFIX + user.getId();
        
        redisTemplate.opsForValue().set(sessionKey, user.getId(), SESSION_TIMEOUT, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(userKey, user, SESSION_TIMEOUT, TimeUnit.SECONDS);
        
        System.out.println("Session created for user: " + user.getUsername());
        System.out.println("Token generated (first 50 chars): " + token.substring(0, Math.min(50, token.length())) + "...");
        return token;
    }
    
    public User getUserFromSession(String token) {
        if (token == null) {
            return null;
        }
        
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        } // Remove "Bearer " prefix
        if (!jwtTokenUtil.isTokenValid(token)) {
            System.err.println("Invalid JWT token");
            return null;
        }
        
        String sessionKey = SESSION_PREFIX + token;
        Long userId = (Long) redisTemplate.opsForValue().get(sessionKey);
        
        if (userId == null || !jwtTokenUtil.isTokenValid(token)) {
            return null;
        }
        
        // Refresh session timeout
        redisTemplate.expire(sessionKey, SESSION_TIMEOUT, TimeUnit.SECONDS);
        
        String userKey = USER_PREFIX + userId;
        return (User) redisTemplate.opsForValue().get(userKey);
    }
    
    public void invalidateSession(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String sessionKey = SESSION_PREFIX + token;
            redisTemplate.delete(sessionKey);
            System.out.println("Session invalidated for token: " + token);
        }
    }
    
    public boolean isValidSession(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return false;
        }
        
        token = token.substring(7);
        String sessionKey = SESSION_PREFIX + token;
        
        boolean exists = redisTemplate.hasKey(sessionKey);
        boolean validToken = jwtTokenUtil.isTokenValid(token);
        
        return exists && validToken;
    }
}