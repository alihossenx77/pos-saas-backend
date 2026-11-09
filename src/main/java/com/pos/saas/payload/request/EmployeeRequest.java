package com.pos.saas.payload.request;

import com.pos.saas.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeRequest {

    @NotBlank @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank @Email @Size(max = 150)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
    private String password;

    @Pattern(regexp = "^(|\\+?[0-9 ()-]{7,20})$",
            message = "Please provide a valid phone number")
    private String phone;

    @NotNull(message = "role is required (ROLE_BRANCH_MANAGER or ROLE_CASHIER)")
    private UserRole role;

    private Long branchId;     // required on POST /api/employees; taken from the URL on /branch/{branchId}
}
