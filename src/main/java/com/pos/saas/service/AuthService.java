package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.User;
import com.pos.saas.payload.UserDto;
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

    /** Public signup creates a STORE_ADMIN account. Stores are created afterwards via POST /api/stores. */
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        String email = normalize(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        if(req.getRole() == UserRole.ROLE_SUPER_ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to perform this action");
        }

        User user = userRepository.save(User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(req.getRole())
                .build());

        return new AuthResponse(jwtProvider.generateToken(user),
                "Registered successfully. Create your first store to get started.", UserDto.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        String email = normalize(req.getEmail());

        // Throws BadCredentialsException / DisabledException
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(jwtProvider.generateToken(user), "Login successful", UserDto.from(user));
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