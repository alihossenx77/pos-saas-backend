package com.pos.saas.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @Email(message = "Please provide a valid email address")
    @Size(max = 150)
    private String email;

    @Pattern(regexp = "^(|\\+?[0-9 ()-]{7,20})$", message = "Please provide a valid phone number")
    private String phone;
}
