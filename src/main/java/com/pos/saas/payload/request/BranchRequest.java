package com.pos.saas.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
public class BranchRequest {

    private Long storeId;                 // required when creating; ignored on update

    @NotBlank(message = "Branch name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String address;

    @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$",
            message = "Phone must be 7-20 characters: digits, spaces, ( ) - and an optional leading +")
    private String phone;

    @Email @Size(max = 150)
    private String email;

    private Set<DayOfWeek> workingDays;   // ["MONDAY","TUESDAY",...]
    private LocalTime openTime;           // "09:00"
    private LocalTime closeTime;          // "21:00"
}
