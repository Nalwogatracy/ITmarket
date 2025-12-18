package com.IT_market;

import com.IT_market.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final AuthInterceptor authInterceptor;
    
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                      "http://localhost:8080",    // Your Spring Boot server\n" +
                       "http://localhost:3000",    // React/Vue dev server\n" +
                       "http://127.0.0.1:8080",    // Alternative localhost\n" +
                       "http://127.0.0.1:3000",    // Alternative dev server\n" +
                       "http://localhost:5500",     // Live Server extension\n" +
                       "file://")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/products",
                        "/api/products/**",
                        "/api/orders/guest",
                        "/api/orders/by-email",
                        "/api/health",
                        "/uploads/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}