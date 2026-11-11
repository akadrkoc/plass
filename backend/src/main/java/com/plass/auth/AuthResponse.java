package com.plass.auth;

import java.time.Instant;

import com.plass.user.UserResponse;

public record AuthResponse(String token, Instant expiresAt, UserResponse user) {
}
