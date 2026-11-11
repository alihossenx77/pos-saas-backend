package com.pos.saas.service;

import com.pos.saas.domain.UserRole;
import com.pos.saas.model.Branch;
import com.pos.saas.model.Store;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.BranchDto;
import com.pos.saas.payload.request.BranchRequest;
import com.pos.saas.repository.BranchRepository;
import com.pos.saas.repository.InventoryRepository;
import com.pos.saas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final AccessService access;

    @Transactional
    public BranchDto create(BranchRequest req) {
        if (req.getStoreId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId is required");
        }

        Store store = access.ownedStore(req.getStoreId());
        validateHours(req);

        String name = req.getName().trim();
        if (branchRepository.existsByStoreIdAndNameIgnoreCase(store.getId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This store already has a branch with that name");
        }

        Branch branch = Branch.builder().storeId(store.getId()).build();
        apply(branch, req);
        return BranchDto.from(branchRepository.save(branch));
    }

    /** Store admin: all branches of the store. Employees: only their own branch. */
    public List<BranchDto> listByStore(Long storeId) {
        User u = access.currentUser();
        Store store = access.accessibleStore(storeId);

        List<Branch> branches = u.getRole() == UserRole.ROLE_STORE_ADMIN
                ? branchRepository.findByStoreId(store.getId())
                : branchRepository.findById(u.getBranchId() == null ? -1L : u.getBranchId()).stream().toList();
        return branches.stream().map(BranchDto::from).toList();
    }

    public BranchDto get(Long id) {
        return BranchDto.from(access.accessibleBranch(id));
    }

    @Transactional
    public BranchDto update(Long id, BranchRequest req) {
        Branch branch = access.accessibleBranch(id);   // manager: only own branch
        validateHours(req);

        String name = req.getName().trim();
        if (branchRepository.existsByStoreIdAndNameIgnoreCaseAndIdNot(branch.getStoreId(), name, branch.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This store already has a branch with that name");
        }
        apply(branch, req);
        return BranchDto.from(branchRepository.save(branch));
    }

    @Transactional
    public void delete(Long id) {
        Branch branch = access.ownedBranch(id);
        if (userRepository.existsByBranchId(branch.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a branch that still has employees. Move or remove them first.");
        }
        if (inventoryRepository.existsByBranchId(branch.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a branch that still has stock records. Remove its inventory first.");
        }
        branchRepository.delete(branch);
    }

    private void validateHours(BranchRequest r) {
        if (r.getOpenTime() != null && r.getCloseTime() != null && !r.getCloseTime().isAfter(r.getOpenTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "closeTime must be after openTime");
        }
    }

    private void apply(Branch b, BranchRequest r) {
        b.setName(r.getName().trim());
        b.setAddress(r.getAddress().trim());
        b.setPhone(r.getPhone());
        b.setEmail(r.getEmail());
        b.setOpenTime(r.getOpenTime());
        b.setCloseTime(r.getCloseTime());
        if (r.getWorkingDays() != null) {
            b.getWorkingDays().clear();
            b.getWorkingDays().addAll(r.getWorkingDays());
        }
    }
}