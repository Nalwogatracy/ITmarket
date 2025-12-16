package com.IT_market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class ItMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItMarketApplication.class, args);
        System.out.println("✅ IT Smart Market Backend Started Successfully!");
        System.out.println("🌐 Local: http://localhost:8080");
        System.out.println("📚 API Docs: http://localhost:8080/swagger-ui.html");
        System.out.println("👤 Admin Dashboard: http://localhost:8080/admin/dashboard");
    }
}