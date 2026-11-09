package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Branch;
import com.pos.saas.model.Store;
import com.pos.saas.model.User;
import com.pos.saas.repository.BranchRepository;
import com.pos.saas.repository.StoreRepository;
import com.pos.saas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Central ownership checks. Every user (admin, manager, cashier) has a storeId,
 * so the store check is the same for everyone. Anything the caller may not see
 * returns 404 (not 403) so other people's ids can't be probed.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public User currentAdmin() {
        User u = currentUser();
        if (u.getRole() != UserRole.ROLE_STORE_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a store admin can do this");
        }
        return u;
    }

    /** Logged-in user who already belongs to a store (admin after creating it, or an employee). */
    public User currentStoreUser() {
        User u = currentUser();
        if (u.getStoreId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Create your store first");
        }
        return u;
    }

    /** The logged-in store admin's store. */
    public Store myStore() {
        User admin = currentAdmin();
        if (admin.getStoreId() == null) throw notFound("You have not created a store yet");
        return storeRepository.findById(admin.getStoreId())
                .orElseThrow(() -> notFound("You have not created a store yet"));
    }

    /** The given store, only if it is the logged-in admin's store. */
    public Store ownedStore(Long storeId) {
        Store store = myStore();
        if (!store.getId().equals(storeId)) throw notFound("Store not found");
        return store;
    }

    /** Any user, but only their own store. */
    public Store accessibleStore(Long storeId) {
        User u = currentUser();
        if (!Objects.equals(u.getStoreId(), storeId)) throw notFound("Store not found");
        return storeRepository.findById(storeId).orElseThrow(() -> notFound("Store not found"));
    }

    /** Branch inside the logged-in admin's store. */
    public Branch ownedBranch(Long branchId) {
        User admin = currentAdmin();
        Branch b = branchRepository.findById(branchId).orElseThrow(() -> notFound("Branch not found"));
        if (!Objects.equals(admin.getStoreId(), b.getStoreId())) throw notFound("Branch not found");
        return b;
    }

    /** Store admin: any branch of their store. Branch manager / cashier: only their own branch. */
    public Branch accessibleBranch(Long branchId) {
        User u = currentUser();
        Branch b = branchRepository.findById(branchId).orElseThrow(() -> notFound("Branch not found"));
        boolean ok = u.getRole() == UserRole.ROLE_STORE_ADMIN
                ? Objects.equals(u.getStoreId(), b.getStoreId())
                : Objects.equals(u.getBranchId(), b.getId());
        if (!ok) throw notFound("Branch not found");
        return b;
    }

    public static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}