package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.Invoice;
import com.example.ERP_Api.Entity.Notification;
import com.example.ERP_Api.Repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ERP_Api.Entity.Order;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /** What the bell needs in one call: the unread number and the 50 newest messages. */
    public record Overview(long unreadCount, List<Notification> items) {}

    @Transactional(readOnly = true)
    public Overview overview() {
        return new Overview(notifications.countBySeenFalse(), notifications.findTop50ByOrderByCreatedAtDesc());
    }

    /**
     * Called from InvoiceService.checkout, inside the same transaction as the order.
     * The notification is saved only if the order is saved, and never for a failed checkout.
     */
    @Transactional
    public void orderPlaced(Invoice invoice, int itemCount) {
        Notification n = new Notification();
        n.setType("ORDER_PLACED");
        n.setTitle("New order " + invoice.getInvoiceNumber());
        n.setMessage(invoice.getCustomerName() + " placed an order ("
                + itemCount + (itemCount == 1 ? " item)" : " items)"));
        n.setInvoiceId(invoice.getId());
        n.setAmountCents(invoice.getTotalCents());
        notifications.save(n);
    }

    /** For an order made on the Orders screen (one product, one customer). */
    @Transactional
    public void orderCreated(Order order) {
        Notification n = new Notification();
        n.setType("ORDER_PLACED");
        n.setTitle("New order #" + order.getId());
        n.setMessage(order.getCustomer().getName() + " ordered "
                + order.getQuantity() + " x " + order.getProduct().getName());
        n.setAmountCents(order.getAmountCents());
        notifications.save(n);
    }
    @Transactional
    public void markRead(Long id) {
        Notification n = notifications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
        n.setSeen(true);
        notifications.save(n);
    }

    @Transactional
    public int markAllRead() {
        return notifications.markAllSeen();
    }
}
