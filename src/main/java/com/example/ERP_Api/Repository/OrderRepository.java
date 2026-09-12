package com.example.ERP_Api.Repository;

import com.example.ERP_Api.Entity.Order;
import com.example.ERP_Api.Entity.ShippingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByShippingStatus(ShippingStatus status);
    List<Order> findByCustomerId(Long customerId);
}
