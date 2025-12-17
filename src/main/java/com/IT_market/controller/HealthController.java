package com.IT_market.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.PostConstruct;

@RestController
public class HealthController {
    
    @PostConstruct
    public void init() {
        System.out.println("HealthController loaded (minimal)");
    }
    
    @GetMapping("/health")
    public String health() {
        // Return minimal response without any DB checks
        return "OK";
    }
    
    @GetMapping("/")
    public String root() {
        return "App is running (lightweight mode)";
    }
}