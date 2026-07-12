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
        userRepository.findByEmail("admin@transitops.com").ifPresentOrElse(
                existing -> { /* already seeded */ },
                () -> {
                    User admin = User.builder()
                            .id(Ids.newId())
                            .email("admin@transitops.com")
                            .passwordHash(passwordEncoder.encode("admin123"))
                            .role(Role.FLEET_MANAGER)
                            .build();
                    userRepository.save(admin);
                }
        );
    }
}