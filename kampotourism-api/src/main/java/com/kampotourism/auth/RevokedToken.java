package com.kampotourism.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A token id that was logged out; rejected until the token would have expired anyway. */
@Entity
@Table(name = "revoked_token")
public class RevokedToken {

    @Id
    @Column(name = "jti")
    private String jti;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {
    }

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() { return jti; }
    public Instant getExpiresAt() { return expiresAt; }
}
