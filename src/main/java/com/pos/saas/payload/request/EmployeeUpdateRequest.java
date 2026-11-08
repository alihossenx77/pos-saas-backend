package com.pos.saas.payload.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeUpdateRequest {

    @Size(min = 2, max = 100)
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$",
            message = "Phone must be 7-20 characters: digits, spaces, ( ) - and an optional leading +")
    private String phone;

    @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
    private String password;          // only when changing it

    private Boolean active;           // active / inactive
    private Long branchId;            // store admin only: move to another branch
}
