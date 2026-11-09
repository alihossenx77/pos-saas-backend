package com.pos.saas.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreRequest {

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 100)
    private String brand;

    @Size(max = 500)
    private String description;

    @Size(max = 50)
    private String storeType;          // Grocery, Electronics, ...

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String address;

    @Pattern(regexp = "^(|\\+?[0-9 ()-]{7,20})$", message = "Please provide a valid phone number")
    private String phone;

    @Email @Size(max = 150)
    private String email;
}
