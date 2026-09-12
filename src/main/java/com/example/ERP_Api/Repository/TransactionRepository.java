package com.example.ERP_Api.Repository;


import com.example.ERP_Api.Entity.Transaction;
import com.example.ERP_Api.Entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByDateBetween(LocalDate from, LocalDate to);
    List<Transaction> findByType(TransactionType type);
    List<Transaction> findByCategoryContainingIgnoreCase(String category);

    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM Transaction t WHERE t.type = :type")
    long sumAmountCentsByType(@Param("type") TransactionType type);
}