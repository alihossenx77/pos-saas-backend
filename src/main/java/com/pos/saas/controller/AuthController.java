package com.pos.saas.controller;

import com.pos.saas.payload.dto.UserDto;
import com.pos.saas.payload.request.LoginRequest;
import com.pos.saas.payload.request.SignupRequest;
import com.pos.saas.payload.response.ApiResponse;
import com.pos.saas.payload.response.AuthResponse;
import com.pos.saas.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.created("Registered successfully", authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    /** Returns the logged-in user (handy for the React app on page refresh). */
    @GetMapping("/me")
    public ApiResponse<UserDto> me(Authentication authentication) {
        return ApiResponse.ok(authService.getByEmail(authentication.getName()));
    }
}
