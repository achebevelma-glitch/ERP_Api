package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.Customer;
import com.example.ERP_Api.Repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // Get all customers
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // Get customer by ID
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    // Create customer
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Update customer
    public Customer updateCustomer(Long id, Customer customerDetails) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setName(customerDetails.getName());
        customer.setEmail(customerDetails.getEmail());
        customer.setStatus(customerDetails.getStatus());

        return customerRepository.save(customer);
    }

    // Delete customer
    public void deleteCustomer(Long id) {

        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found");
        }

        customerRepository.deleteById(id);
    }
}