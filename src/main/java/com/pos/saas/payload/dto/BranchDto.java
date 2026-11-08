package com.pos.saas.payload.dto;

import com.pos.saas.model.Branch;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

@Data @Builder
public class BranchDto {
    private Long id;
    private Long storeId;
    private String name;
    private String address;
    private String phone;
    private String email;
    private Set<DayOfWeek> workingDays;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BranchDto from(Branch b) {
        return BranchDto.builder()
                .id(b.getId()).storeId(b.getStoreId()).name(b.getName()).address(b.getAddress())
                .phone(b.getPhone()).email(b.getEmail())
                .workingDays(new TreeSet<>(b.getWorkingDays()))
                .openTime(b.getOpenTime()).closeTime(b.getCloseTime())
                .createdAt(b.getCreatedAt()).updatedAt(b.getUpdatedAt())
                .build();
    }
}
