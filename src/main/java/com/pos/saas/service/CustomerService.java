package com.pos.saas.service;

import com.pos.saas.model.Customer;
import com.pos.saas.model.User;
import com.pos.saas.payload.dto.CustomerDto;
import com.pos.saas.payload.request.CustomerRequest;
import com.pos.saas.repository.CustomerRepository;
import com.pos.saas.repository.OrderRepository;
import com.pos.saas.util.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Customers belong to the store (shared by all its branches). */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AccessService access;

    @Transactional
    public CustomerDto create(CustomerRequest req) {
        User actor = access.currentStoreUser();
        String phone = blankToNull(req.getPhone());

        if (phone != null && customerRepository.existsByStoreIdAndPhone(actor.getStoreId(), phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with this phone number already exists");
        }

        Customer customer = Customer.builder().storeId(actor.getStoreId()).build();
        apply(customer, req, phone);
        return CustomerDto.from(customerRepository.save(customer));
    }

    /** q is optional (name / phone / email). */
    public List<CustomerDto> list(String q, int page, int size) {
        User actor = access.currentStoreUser();
        List<Customer> customers = (q == null || q.isBlank())
                ? customerRepository.findByStoreIdOrderByFullNameAsc(actor.getStoreId(), Paging.of(page, size))
                : customerRepository.search(actor.getStoreId(), q.trim(), Paging.of(page, size));
        return customers.stream().map(CustomerDto::from).toList();
    }

    public CustomerDto get(Long id) {
        return CustomerDto.from(find(id));
    }

    @Transactional
    public CustomerDto update(Long id, CustomerRequest req) {
        Customer customer = find(id);
        String phone = blankToNull(req.getPhone());

        if (phone != null && customerRepository.existsByStoreIdAndPhoneAndIdNot(customer.getStoreId(), phone, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A customer with this phone number already exists");
        }
        apply(customer, req, phone);
        return CustomerDto.from(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = find(id);
        if (orderRepository.existsByCustomerId(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This customer has purchase history and cannot be deleted");
        }
        customerRepository.delete(customer);
    }

    // ---- helpers ----

    Customer find(Long id) {
        User actor = access.currentStoreUser();
        return customerRepository.findByIdAndStoreId(id, actor.getStoreId())
                .orElseThrow(() -> AccessService.notFound("Customer not found"));
    }

    private void apply(Customer c, CustomerRequest r, String phone) {
        c.setFullName(r.getFullName().trim());
        c.setEmail(blankToNull(r.getEmail()));
        c.setPhone(phone);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
