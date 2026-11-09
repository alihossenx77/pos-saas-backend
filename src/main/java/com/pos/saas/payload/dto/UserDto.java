package com.pos.saas.payload.dto;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private Long storeId;
    private Long branchId;
    private boolean active;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public static UserDto from(User u) {
        return UserDto.builder()
                .id(u.getId()).fullName(u.getFullName()).email(u.getEmail())
                .phone(u.getPhone()).role(u.getRole())
                .storeId(u.getStoreId()).branchId(u.getBranchId())
                .active(u.isActive()).lastLogin(u.getLastLogin()).createdAt(u.getCreatedAt())
                .build();
    }
}
