package com.kampotourism.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Size(max = 254) String email,
                               @NotBlank @Size(max = 200) String password) {
    }

    public record UserDto(Long id, String email, String displayName, String role) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, UserDto user) {
    }
}
