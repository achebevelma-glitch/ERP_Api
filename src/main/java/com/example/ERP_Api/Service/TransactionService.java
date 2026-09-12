package com.example.ERP_Api.Service;


import com.example.ERP_Api.Entity.Transaction;
import com.example.ERP_Api.Entity.TransactionType;
import com.example.ERP_Api.Repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> list(LocalDate from, LocalDate to, TransactionType type, String category) {
        if (from != null && to != null) return transactionRepository.findByDateBetween(from, to);
        if (type != null) return transactionRepository.findByType(type);
        if (category != null) return transactionRepository.findByCategoryContainingIgnoreCase(category);
        return transactionRepository.findAll();
    }

    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    public Transaction create(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void delete(Long id) {
        transactionRepository.deleteById(id);
    }

    // Sums run as SUM() in the database, not by loading every row into
    // Java and adding them up — same precision reasoning as storing cents,
    // just applied to the query side instead of the storage side.
    public Map<String, Long> getSummary() {
        long revenue = transactionRepository.sumAmountCentsByType(TransactionType.INCOME);
        long expenses = transactionRepository.sumAmountCentsByType(TransactionType.EXPENSE);
        return Map.of(
                "revenueCents", revenue,
                "expensesCents", expenses,
                "netProfitCents", revenue - expenses
        );
    }
}