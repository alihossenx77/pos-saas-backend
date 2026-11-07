package com.pos.saas.payload;

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
    private Long tenantId;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserDto from(User u) {
        return UserDto.builder()
                .id(u.getId()).fullName(u.getFullName()).email(u.getEmail())
                .phone(u.getPhone()).role(u.getRole()).tenantId(u.getTenantId())
                .active(u.isActive()).createdAt(u.getCreatedAt())
                .build();
    }
}
