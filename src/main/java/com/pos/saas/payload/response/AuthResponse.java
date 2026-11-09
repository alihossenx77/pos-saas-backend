package com.pos.saas.payload.response;

import com.pos.saas.payload.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;

/** The "data" part of ApiResponse for signup / login. */
@Data @AllArgsConstructor
public class AuthResponse {
    private String jwt;
    private UserDto user;
}
