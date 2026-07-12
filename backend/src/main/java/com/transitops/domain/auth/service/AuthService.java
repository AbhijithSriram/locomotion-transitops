package com.transitops.domain.auth.service;

import com.transitops.common.exception.UnauthorizedException;
import com.transitops.domain.auth.dto.LoginRequest;
import com.transitops.domain.auth.dto.LoginResponse;
import com.transitops.domain.auth.entity.User;
import com.transitops.domain.auth.dto.AuthUserDto;
import com.transitops.domain.auth.repository.UserRepository;
import com.transitops.domain.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        long expiresInMs = 900000L; // 15 minutes
        
        AuthUserDto authUser = new AuthUserDto(
                user.getId(),
                "Driver", // Defaulting name since it's not in User entity
                user.getEmail(),
                user.getRole().name(),
                user.getId() // driverId is the same as userId in this system
        );

        return new LoginResponse(accessToken, refreshToken, expiresInMs, authUser);
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String userId = jwtService.extractUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        long expiresInMs = 900000L;
        
        AuthUserDto authUser = new AuthUserDto(
                user.getId(),
                "Driver",
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return new LoginResponse(newAccessToken, newRefreshToken, expiresInMs, authUser);
    }
}