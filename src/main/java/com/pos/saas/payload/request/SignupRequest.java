package com.pos.saas.payload.request;

import com.pos.saas.domain.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    /**
     * Optional. Public signup only allows ROLE_STORE_ADMIN (default) or ROLE_USER.
     * Branch managers and cashiers are added by a store admin through /api/employees.
     */
    private UserRole role;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
             message = "Password must contain at least one letter and one digit")
    private String password;

    @Pattern(regexp = "^(|\\+?[0-9 ()-]{7,20})$", message = "Please provide a valid phone number")
    private String phone;
}
