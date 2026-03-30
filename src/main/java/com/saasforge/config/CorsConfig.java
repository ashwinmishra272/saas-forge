package com.saasforge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Which frontend origins are allowed to call us
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));

        // Which HTTP methods are allowed
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Which headers the frontend can send
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));

        // Which headers the frontend can read from the response
        configuration.setExposedHeaders(List.of("Authorization"));

        // Allow cookies and Authorization headers to be sent
        configuration.setAllowCredentials(true);

        // How long browser can cache the preflight response (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply this CORS config to ALL endpoints
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
