package com.example.ERP_Api.Entity;


import java.util.Map;
import java.util.Set;

public enum ShippingStatus {
    PENDING,
    DELIVERED,
    DECLINED;

    private static final Map<ShippingStatus, Set<ShippingStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(DELIVERED, DECLINED),
            DELIVERED, Set.of(),
            DECLINED, Set.of()
    );

    public boolean canTransitionTo(ShippingStatus newStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(newStatus);
    }
}
