package com.kampotourism.auth;

import com.kampotourism.auth.AuthDtos.LoginRequest;
import com.kampotourism.auth.AuthDtos.LoginResponse;
import com.kampotourism.auth.AuthDtos.UserDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return service.login(request.email(), request.password(), http.getRemoteAddr());
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearer")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        service.logout(jwt.getId(), jwt.getExpiresAt());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearer")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return service.currentUser(jwt);
    }
}
