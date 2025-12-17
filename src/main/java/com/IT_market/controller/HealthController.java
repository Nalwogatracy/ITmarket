
package com.IT_market.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    
    /**
     * Koyeb health check endpoint
     * Returns plain text "OK" with HTTP 200 status
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
    
    /**
     * API status endpoint
     */
    @GetMapping("/api/status")
    public String status() {
        return "{\"status\":\"UP\",\"service\":\"IT Market API\",\"timestamp\":\"" + 
               java.time.LocalDateTime.now() + "\"}";
    }
    
    /**
     * Simple ping endpoint
     */
    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }
}
