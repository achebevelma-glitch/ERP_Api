package com.example.ERP_Api.Contoller;

import com.example.ERP_Api.Entity.Customer;
import com.example.ERP_Api.Service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // GET all customers
    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    // GET customer by ID
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {

        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create customer
    @PostMapping
    public ResponseEntity<Customer> createCustomer(
            @RequestBody Customer customer) {

        Customer savedCustomer = customerService.createCustomer(customer);

        return ResponseEntity.ok(savedCustomer);
    }

    // PUT update customer
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Long id,
            @RequestBody Customer customer) {

        try {
            Customer updatedCustomer =
                    customerService.updateCustomer(id, customer);

            return ResponseEntity.ok(updatedCustomer);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE customer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {

        try {
            customerService.deleteCustomer(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
