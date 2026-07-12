package com.transitops.domain.auth.dto;

public record AuthUserDto(
    String id,
    String name,
    String email,
    String role,
    String driverId
) {}
