package com.transitops.domain.auth.service;

import com.transitops.common.enums.Role;
import com.transitops.common.util.Ids;
import com.transitops.domain.auth.entity.User;
import com.transitops.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("admin@transitops.com", Role.FLEET_MANAGER);
        seed("safety@transitops.com", Role.SAFETY_OFFICER);
        seed("finance@transitops.com", Role.FINANCIAL_ANALYST);
    }

    private void seed(String email, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> { /* already seeded */ },
                () -> userRepository.save(User.builder()
                        .id(Ids.newId())
                        .email(email)
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(role)
                        .build())
        );
    }
}