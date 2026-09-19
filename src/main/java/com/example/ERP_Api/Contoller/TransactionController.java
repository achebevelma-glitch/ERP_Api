package com.example.ERP_Api.Contoller;

import com.example.ERP_Api.Entity.Transaction;
import com.example.ERP_Api.Entity.TransactionType;
import com.example.ERP_Api.Service.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/transactions")
    public List<Transaction> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(required = false)
            TransactionType type,

            @RequestParam(required = false)
            String category) {

        return transactionService.list(from, to, type, category);
    }

    @GetMapping("/transactions/{id}")
    public Transaction get(@PathVariable Long id) {
        return transactionService.getById(id);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(
            @RequestBody TransactionService.CreateTransactionRequest req) {

        return transactionService.create(req);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        transactionService.delete(id);
    }

    @GetMapping("/accounting/summary")
    public Map<String, Long> summary() {
        return transactionService.getSummary();
    }
}