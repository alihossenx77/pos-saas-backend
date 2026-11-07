package com.pos.saas.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class AuthResponse {
    private String jwt;
    private String message;
    private UserDto user;
}
