package com.transitops.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String role,
        String email
) {}