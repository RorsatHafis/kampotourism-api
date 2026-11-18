package com.kampotourism.auth;

import com.kampotourism.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN from ADMIN_EMAIL / ADMIN_PASSWORD when the user table is empty. */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminUserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityProperties properties;

    AdminBootstrap(AdminUserRepository users, PasswordEncoder encoder, SecurityProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String email = properties.bootstrapAdmin().email();
        String password = properties.bootstrapAdmin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No admin user exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set: "
                    + "the admin API cannot be used until one is created.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        users.save(new AdminUser(email.trim().toLowerCase(), encoder.encode(password), "Administrator", Role.ADMIN));
        log.info("Created the first admin user {}", email.trim().toLowerCase());
    }
}
