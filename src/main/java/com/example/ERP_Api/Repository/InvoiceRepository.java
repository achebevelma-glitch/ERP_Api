package com.example.ERP_Api.Repository;

import com.example.ERP_Api.Entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByCreatedAtDesc();
    List<Invoice> findByCreatedByIgnoreCaseOrderByCreatedAtDesc(String createdBy);
}
