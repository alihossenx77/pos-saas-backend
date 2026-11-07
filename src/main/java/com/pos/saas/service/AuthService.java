package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Store;
import com.pos.saas.model.User;
import com.pos.saas.payload.AuthResponse;
import com.pos.saas.payload.LoginRequest;
import com.pos.saas.payload.SignupRequest;
import com.pos.saas.payload.UserDto;
import com.pos.saas.repository.StoreRepository;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    /** Public signup = a new business (tenant) + its first STORE_ADMIN user. */
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Store store = storeRepository.save(Store.builder()
                .brand(req.getStoreName())
                .build());

        User user = userRepository.save(User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(UserRole.ROLE_STORE_ADMIN)
                .tenantId(store.getId())
                .build());

        return new AuthResponse(jwtProvider.generateToken(user), "Registered successfully", UserDto.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException on wrong email/password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        return new AuthResponse(jwtProvider.generateToken(user), "Login successful", UserDto.from(user));
    }

    public UserDto getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
