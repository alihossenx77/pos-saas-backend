package com.pos.saas.config;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.User;
import com.pos.saas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Public signup can never create a super admin, so create the first one at startup.
 * Set SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD (env vars or application.yml). Leave them empty to disable.
 * If a user with that email already exists nothing is changed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:}")
    private String email;

    @Value("${app.super-admin.password:}")
    private String password;

    @Value("${app.super-admin.name:Super Admin}")
    private String name;

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;

        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) return;

        if (password.length() < 8) {
            log.warn("SUPER_ADMIN_PASSWORD is shorter than 8 characters: super admin NOT created");
            return;
        }

        userRepository.save(User.builder()
                .fullName(name)
                .email(normalized)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ROLE_SUPER_ADMIN)
                .build());
        log.info("Super admin created: {}", normalized);
    }
}
