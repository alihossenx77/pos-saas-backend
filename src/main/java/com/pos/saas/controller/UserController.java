package com.pos.saas.controller;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.User;
import com.pos.saas.payload.UserDto;
import com.pos.saas.repository.StoreRepository;
import com.pos.saas.repository.UserRepository;
import com.pos.saas.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final AccessService access;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @GetMapping("/api/user/profile")
    public ResponseEntity<UserDto> profile() {
        return ResponseEntity.ok(UserDto.from(access.currentUser()));
    }

    /** Yourself, people in your branch, or (store admin) anyone in your store. */
    @GetMapping("/api/users/{id}")
    public ResponseEntity<UserDto> byId(@PathVariable Long id) {
        User actor = access.currentUser();
        User target = userRepository.findById(id).orElseThrow(() -> AccessService.notFound("User not found"));

        boolean self = actor.getId().equals(target.getId());
        boolean sameBranch = actor.getBranchId() != null && Objects.equals(actor.getBranchId(), target.getBranchId());
        boolean sameStoreAdmin = actor.getRole() == UserRole.ROLE_STORE_ADMIN
                && actor.getStoreId() != null
                && Objects.equals(actor.getStoreId(), target.getStoreId());

        if (!self && !sameBranch && !sameStoreAdmin) throw AccessService.notFound("User not found");
        return ResponseEntity.ok(UserDto.from(target));
    }
}