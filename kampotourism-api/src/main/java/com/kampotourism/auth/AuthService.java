package com.kampotourism.auth;

import com.kampotourism.auth.AuthDtos.LoginResponse;
import com.kampotourism.auth.AuthDtos.UserDto;
import com.kampotourism.common.error.InvalidCredentialsException;
import com.kampotourism.common.error.TooManyRequestsException;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AdminUserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginAttemptLimiter limiter;
    private final RevokedTokenRepository revoked;

    /** Compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyHash;

    public AuthService(AdminUserRepository users, PasswordEncoder encoder, TokenService tokens,
                       LoginAttemptLimiter limiter, RevokedTokenRepository revoked) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.limiter = limiter;
        this.revoked = revoked;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @Transactional
    public LoginResponse login(String email, String password, String clientIp) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String key = clientIp + "|" + normalized;
        if (limiter.isBlocked(key)) {
            throw new TooManyRequestsException("Too many failed sign-in attempts. Try again in a few minutes.");
        }

        AdminUser user = users.findByEmailIgnoreCase(normalized).filter(AdminUser::isEnabled).orElse(null);
        boolean passwordMatches = encoder.matches(password, user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !passwordMatches) {
            limiter.recordFailure(key);
            throw new InvalidCredentialsException();
        }

        limiter.recordSuccess(key);
        user.markLoggedIn();
        TokenService.IssuedToken token = tokens.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), toDto(user));
    }

    @Transactional
    public void logout(String tokenId, Instant expiresAt) {
        revoked.deleteExpired(Instant.now());
        if (tokenId != null && expiresAt != null && !revoked.existsById(tokenId)) {
            revoked.save(new RevokedToken(tokenId, expiresAt));
        }
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(Jwt jwt) {
        long id;
        try {
            id = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidCredentialsException();
        }
        return users.findById(id).filter(AdminUser::isEnabled).map(AuthService::toDto)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private static UserDto toDto(AdminUser u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole().name());
    }
}
