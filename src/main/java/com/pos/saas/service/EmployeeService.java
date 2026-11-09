package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Branch;
import com.pos.saas.model.Store;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.UserDto;
import com.pos.saas.payload.request.EmployeeRequest;
import com.pos.saas.payload.request.EmployeeUpdateRequest;
import com.pos.saas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * STORE_ADMIN    -> branch managers + cashiers in any branch of their store.
 * BRANCH_MANAGER -> cashiers of their own branch only.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final Set<UserRole> EMPLOYEE_ROLES =
            EnumSet.of(UserRole.ROLE_BRANCH_MANAGER, UserRole.ROLE_CASHIER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessService access;

    @Transactional
    public UserDto create(EmployeeRequest req) {
        User actor = access.currentUser();

        if (!EMPLOYEE_ROLES.contains(req.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be ROLE_BRANCH_MANAGER or ROLE_CASHIER");
        }
        if (actor.getRole() == UserRole.ROLE_BRANCH_MANAGER && req.getRole() != UserRole.ROLE_CASHIER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Branch managers can only add cashiers");
        }
        if (req.getBranchId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId is required");
        }

        Branch branch = access.accessibleBranch(req.getBranchId());   // admin: owned, manager: own branch

        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User saved = userRepository.save(User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(req.getRole())
                .storeId(branch.getStoreId())
                .branchId(branch.getId())
                .build());
        return UserDto.from(saved);
    }

    public List<UserDto> listByBranch(Long branchId, UserRole role) {
        Branch branch = access.accessibleBranch(branchId);
        return filter(userRepository.findByBranchId(branch.getId()), role);
    }

    public List<UserDto> listByStore(Long storeId, UserRole role) {
        User actor = access.currentUser();
        Store store = access.accessibleStore(storeId);
        List<User> users = actor.getRole() == UserRole.ROLE_STORE_ADMIN
                ? userRepository.findByStoreId(store.getId())
                : userRepository.findByBranchId(actor.getBranchId());   // manager: own branch only
        return filter(users, role);
    }

    public UserDto get(Long id) {
        return UserDto.from(loadManageable(id, access.currentUser()));
    }

    @Transactional
    public UserDto update(Long id, EmployeeUpdateRequest req) {
        User actor = access.currentUser();
        User target = loadManageable(id, actor);

        if (req.getFullName() != null && !req.getFullName().isBlank()) target.setFullName(req.getFullName().trim());
        if (req.getPhone() != null) target.setPhone(req.getPhone());
        if (req.getPassword() != null) target.setPassword(passwordEncoder.encode(req.getPassword()));
        if (req.getActive() != null) target.setActive(req.getActive());

        if (req.getBranchId() != null && !req.getBranchId().equals(target.getBranchId())) {
            if (actor.getRole() != UserRole.ROLE_STORE_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only a store admin can move employees between branches");
            }
            Branch newBranch = access.ownedBranch(req.getBranchId());
            target.setBranchId(newBranch.getId());
            target.setStoreId(newBranch.getStoreId());
        }
        return UserDto.from(userRepository.save(target));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(loadManageable(id, access.currentUser()));
    }

    // ---- helpers ----

    private List<UserDto> filter(List<User> users, UserRole role) {
        return users.stream()
                .filter(u -> EMPLOYEE_ROLES.contains(u.getRole()))
                .filter(u -> role == null || u.getRole() == role)
                .map(UserDto::from)
                .toList();
    }

    private User loadManageable(Long id, User actor) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> AccessService.notFound("Employee not found"));

        // must be an employee of the same store as the caller
        if (!EMPLOYEE_ROLES.contains(target.getRole())
                || target.getStoreId() == null
                || !Objects.equals(target.getStoreId(), actor.getStoreId())) {
            throw AccessService.notFound("Employee not found");
        }

        if (actor.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            if (!Objects.equals(target.getBranchId(), actor.getBranchId())) {
                throw AccessService.notFound("Employee not found");
            }
            if (target.getRole() != UserRole.ROLE_CASHIER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only manage cashiers");
            }
        } else if (actor.getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage employees");
        }
        return target;
    }
}
