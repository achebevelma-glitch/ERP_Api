package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.*;
import com.example.ERP_Api.Repository.CustomerRepository;
import com.example.ERP_Api.Repository.InventoryRepository;
import com.example.ERP_Api.Repository.OrderRepository;
import com.example.ERP_Api.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceStockTest {

    @Mock OrderRepository orders;
    @Mock InventoryRepository inventories;
    @Mock ProductRepository products;
    @Mock CustomerRepository customers;
    @Mock InventoryService inventoryService;

    @InjectMocks OrderService service;

    Customer customer;
    Product product;

    @BeforeEach
    void setUp() {
        customer = new Customer("Acme Ltd", "acme@example.com", CustomerStatus.ACTIVE);
        product = new Product("Smart TV 55", "TV-55", "TV", 10_000L);
        ReflectionTestUtils.setField(product, "id", 5L);
    }

    @Test
    void creatingAnOrderLogsAStockAdjustmentNamingTheOrder() {
        Inventory inventory = new Inventory(product, 10);
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(5L)).thenReturn(Optional.of(product));
        when(inventories.findByProductIdForUpdate(5L)).thenReturn(Optional.of(inventory));
        when(orders.save(any(Order.class))).thenAnswer(call -> {
            Order o = call.getArgument(0);
            ReflectionTestUtils.setField(o, "id", 77L);
            return o;
        });

        Order created = service.create(new OrderService.CreateOrderRequest(1L, 5L, 3));

        assertEquals(30_000L, created.getAmountCents());
        verify(inventoryService).adjustStock(5L, -3, "Order #77");
        // The service must not also subtract by hand, or stock would drop twice.
        assertEquals(10, inventory.getQuantity());
    }

    @Test
    void shortStockRejectsTheOrderAndChangesNothing() {
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(5L)).thenReturn(Optional.of(product));
        when(inventories.findByProductIdForUpdate(5L)).thenReturn(Optional.of(new Inventory(product, 2)));

        assertThrows(IllegalStateException.class,
                () -> service.create(new OrderService.CreateOrderRequest(1L, 5L, 3)));

        verify(orders, never()).save(any());
        verify(inventoryService, never()).adjustStock(anyLong(), anyInt(), anyString());
    }

    @Test
    void decliningAnOrderReturnsItsQuantityToStock() {
        Order order = pendingOrder(3);
        when(orders.findById(77L)).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));

        Order result = service.cancel(77L);

        assertEquals(ShippingStatus.DECLINED, result.getShippingStatus());
        verify(inventoryService).adjustStock(5L, 3, "Order #77 declined");
    }

    @Test
    void deliveringAnOrderDoesNotTouchStock() {
        Order order = pendingOrder(3);
        when(orders.findById(77L)).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));

        service.transitionStatus(77L, ShippingStatus.DELIVERED);

        verify(inventoryService, never()).adjustStock(anyLong(), anyInt(), anyString());
    }

    @Test
    void anAlreadyDeclinedOrderCannotBeDeclinedAgainSoStockIsNeverReturnedTwice() {
        Order order = pendingOrder(3);
        order.setShippingStatus(ShippingStatus.DECLINED);
        when(orders.findById(77L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> service.cancel(77L));

        verify(inventoryService, never()).adjustStock(anyLong(), anyInt(), anyString());
    }

    private Order pendingOrder(int quantity) {
        Order order = new Order(customer, product, quantity, 10_000L * quantity,
                PaymentStatus.DUE, ShippingStatus.PENDING);
        ReflectionTestUtils.setField(order, "id", 77L);
        return order;
    }
}
