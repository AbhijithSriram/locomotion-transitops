package com.transitops.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresInMs,
        AuthUserDto user
) {}