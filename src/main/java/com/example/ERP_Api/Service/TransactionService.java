package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.Order;
import com.example.ERP_Api.Entity.Transaction;
import com.example.ERP_Api.Entity.TransactionType;
import com.example.ERP_Api.Repository.OrderRepository;
import com.example.ERP_Api.Repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              OrderRepository orderRepository) {
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
    }

    public List<Transaction> list(LocalDate from, LocalDate to,
                                  TransactionType type, String category) {

        if (from != null && to != null) {
            return transactionRepository.findByDateBetween(from, to);
        }

        if (type != null) {
            return transactionRepository.findByType(type);
        }

        if (category != null) {
            return transactionRepository.findByCategoryContainingIgnoreCase(category);
        }

        return transactionRepository.findAll();
    }

    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Transaction not found: " + id));
    }

    public record CreateTransactionRequest(
            LocalDate date,
            String description,
            String category,
            Long amountCents,
            TransactionType type,
            Long orderId
    ) {}

    public Transaction create(CreateTransactionRequest req) {

        Transaction transaction = new Transaction(
                req.date(),
                req.description(),
                req.category(),
                req.amountCents(),
                req.type()
        );

        if (req.orderId() != null) {

            Order order = orderRepository.findById(req.orderId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Order not found: " + req.orderId()
                            ));

            transaction.setOrder(order);
        }

        return transactionRepository.save(transaction);
    }

    public void delete(Long id) {
        transactionRepository.deleteById(id);
    }

    public Map<String, Long> getSummary() {

        long revenue =
                transactionRepository.sumAmountCentsByType(TransactionType.INCOME);

        long expenses =
                transactionRepository.sumAmountCentsByType(TransactionType.EXPENSE);

        return Map.of(
                "revenueCents", revenue,
                "expensesCents", expenses,
                "netProfitCents", revenue - expenses
        );
    }
}