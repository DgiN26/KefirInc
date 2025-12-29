package com.kefir.logistics.launcher_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000",    // React фронтенд
                        "http://localhost:8080",    // API Gateway
                        "http://localhost:8081",    // User Service
                        "http://localhost:8090",    // Saga Service
                        "http://localhost:8097",    // Auth Service
                        "http://localhost:8099",    // ← ДОБАВЛЕНО: Сам Launcher-service
                        "http://127.0.0.1:8099"     // ← ДОБАВЛЕНО: Альтернативный адрес
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}