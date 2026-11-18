package com.kampotourism.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String jwtSecret, Duration accessTokenTtl, String issuer,
                                 BootstrapAdmin bootstrapAdmin) {

    public record BootstrapAdmin(String email, String password) {
    }

    public SecurityProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofHours(2);
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "kampotourism-api";
        }
        if (bootstrapAdmin == null) {
            bootstrapAdmin = new BootstrapAdmin(null, null);
        }
    }
}
