package com.pos.saas.service;

import com.pos.saas.domain.StoreStatus;
import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Store;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.StoreDto;
import com.pos.saas.payload.request.StoreRequest;
import com.pos.saas.repository.BranchRepository;
import com.pos.saas.repository.StoreRepository;
import com.pos.saas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AccessService access;

    /** One store per store admin. The name may be the same as any other store's name. */
    @Transactional
    public StoreDto create(StoreRequest req) {
        User admin = access.currentAdmin();

        if (storeRepository.existsByStoreAdminId(admin.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a store. Add more locations as branches.");
        }

        Store store = Store.builder().storeAdminId(admin).build();
        apply(store, req);
        Store saved = storeRepository.save(store);

        admin.setStoreId(saved.getId());      // <- the missing link
        userRepository.save(admin);

        return StoreDto.from(saved);
    }

    /** The logged-in store admin's store. */
    public StoreDto myStore() {
        return StoreDto.from(access.myStore());
    }

    /** The store the logged-in branch manager / cashier works in. */
    public StoreDto employeeStore() {
        User u = access.currentUser();
        if (u.getStoreId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not assigned to a store");
        }
        return StoreDto.from(access.accessibleStore(u.getStoreId()));
    }

    public StoreDto get(Long id) {
        return StoreDto.from(access.accessibleStore(id));
    }

    @Transactional
    public StoreDto update(Long id, StoreRequest req) {
        Store store = access.ownedStore(id);
        apply(store, req);
        return StoreDto.from(storeRepository.save(store));
    }

    @Transactional
    public void delete(Long id) {
        Store store = access.ownedStore(id);
        User admin = store.getStoreAdminId();

        // the admin themself doesn't count as an employee
        if (branchRepository.existsByStoreId(id) || userRepository.existsByIdAndStoreId(id, admin.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Remove this store's branches and employees before deleting it");
        }

        admin.setStoreId(null);
        userRepository.save(admin);
        storeRepository.delete(store);
    }

    // ---- super admin ----

    public List<StoreDto> all(StoreStatus status) {
        List<Store> stores = status == null ? storeRepository.findAll() : storeRepository.findByStatus(status);
        return stores.stream().map(StoreDto::from).toList();
    }

    @Transactional
    public StoreDto moderate(Long id, StoreStatus status) {
        if (access.currentUser().getRole() != UserRole.ROLE_SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a super admin can do this");
        }
        Store store = storeRepository.findById(id).orElseThrow(() -> AccessService.notFound("Store not found"));
        store.setStatus(status);
        return StoreDto.from(storeRepository.save(store));
    }

    private void apply(Store s, StoreRequest r) {
        s.setBrand(r.getBrand().trim());
        s.setDescription(r.getDescription());
        s.setStoreType(r.getStoreType());
        s.setAddress(r.getAddress().trim());
        s.setPhone(r.getPhone());
        s.setEmail(r.getEmail());
    }
}