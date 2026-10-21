package com.example.ERP_Api.Contoller;

import com.example.ERP_Api.Service.InvoiceService;
import com.example.ERP_Api.Service.InvoiceService.CheckoutRequest;
import com.example.ERP_Api.Service.InvoiceService.InvoiceDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDto checkout(Authentication auth, @RequestBody CheckoutRequest req) {
        return invoiceService.checkout(auth.getName(), req);
    }

    // The signed-in user's own invoices
    @GetMapping("/mine")
    public List<InvoiceDto> mine(Authentication auth) {
        return invoiceService.listMine(auth.getName());
    }

    // All invoices: admin only (enforced in SecurityConfig)
    @GetMapping
    public List<InvoiceDto> all() {
        return invoiceService.listAll();
    }

    @GetMapping("/{id}")
    public InvoiceDto get(Authentication auth, @PathVariable Long id) {
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return invoiceService.get(id, auth.getName(), admin);
    }
    @PatchMapping("/{id}/pay")
    public InvoiceDto pay(@PathVariable Long id) {
        return invoiceService.markPaid(id);
    }

    public record RefundRequest(Boolean restock) {}

    @PatchMapping("/{id}/refund")
    public InvoiceDto refund(@PathVariable Long id,
                             @RequestBody(required = false) RefundRequest req) {
        boolean restock = req == null || req.restock() == null || req.restock();
        return invoiceService.refund(id, restock);
    }
}
