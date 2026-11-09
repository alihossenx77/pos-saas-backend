package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.UserDto;
import com.pos.saas.payload.request.LoginRequest;
import com.pos.saas.payload.request.SignupRequest;
import com.pos.saas.payload.response.AuthResponse;
import com.pos.saas.repository.UserRepository;
import com.pos.saas.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    /**
     * Public signup. Default role is STORE_ADMIN; ROLE_USER is also allowed.
     * Branch managers / cashiers are created by a store admin, super admins are created manually.
     */
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        String email = normalize(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        UserRole role = req.getRole() == null ? UserRole.ROLE_STORE_ADMIN : req.getRole();
        if (role != UserRole.ROLE_STORE_ADMIN && role != UserRole.ROLE_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This role cannot be chosen at signup. Branch managers and cashiers are added by a store admin.");
        }

        User user = userRepository.save(User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(role)
                .build());

        return new AuthResponse(jwtProvider.generateToken(user), UserDto.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        String email = normalize(req.getEmail());

        // Throws BadCredentialsException / DisabledException
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(jwtProvider.generateToken(user), UserDto.from(user));
    }

    public UserDto getByEmail(String email) {
        return userRepository.findByEmail(normalize(email))
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
