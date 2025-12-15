package com.IT_market;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // List of allowed origins
                String[] allowedOrigins = new String[] {
                    "http://localhost:3000", // React dev server
                    "https://frontend-five-pied-81.vercel.app", // Production
                    "https://frontend-git-master-nalwogas-projects.vercel.app",
                    "https://frontend-ok4pfmi38-nalwogas-projects.vercel.app"
                };

                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
