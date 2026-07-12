package com.transitops.domain.auth.security;

import com.transitops.domain.auth.entity.User;
import com.transitops.domain.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("URI: " + request.getRequestURI());
        System.out.println("AUTH HEADER: [" + authHeader + "]");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("NO BEARER HEADER — skipping auth");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            boolean valid = jwtService.isTokenValid(token);
            boolean isAccess = jwtService.isAccessToken(token);
            System.out.println("TOKEN VALID: " + valid + ", IS ACCESS: " + isAccess);

            if (!valid || !isAccess) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            System.out.println("USER ID: " + userId + ", ROLE: " + role);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<User> userOpt = userRepository.findById(userId);
                System.out.println("USER FOUND: " + userOpt.isPresent());

                if (userOpt.isPresent()) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userOpt.get(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("AUTH SET SUCCESSFULLY");
                }
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION IN FILTER: " + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}