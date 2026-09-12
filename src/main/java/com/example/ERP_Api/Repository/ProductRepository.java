
package com.example.ERP_Api.Repository;

import com.example.ERP_Api.Entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByNameContainingIgnoreCaseAndCategoryContainingIgnoreCase(
            String name, String category, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}