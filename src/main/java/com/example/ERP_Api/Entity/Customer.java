package com.example.ERP_Api.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "customers")
public class Customer{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String name;
    @Setter
    private String email;

    @Setter
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    private Instant createdAt;


    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("customer")
    private List<Order> orders = new ArrayList<>();

    public Customer() {
    }

    public Customer(String name, String email, CustomerStatus status) {
        this.name = name;
        this.email = email;
        this.status = status;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {

        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public int getOrderCount() {
        return orders.size();
    }
}