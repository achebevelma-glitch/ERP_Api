package com.example.ERP_Api.Entity;


import java.util.Map;
import java.util.Set;

public enum ShippingStatus {
    PENDING,
    DELIVERED,
    DECLINED;

    // Defines which transitions are legal, so a bare PATCH can't move an
    // order backwards (e.g. DELIVERED -> PENDING) or into a state that
    // doesn't make sense for where it currently is.
    private static final Map<ShippingStatus, Set<ShippingStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(DELIVERED, DECLINED),
            DELIVERED, Set.of(),   // terminal state
            DECLINED, Set.of()     // terminal state
    );

    public boolean canTransitionTo(ShippingStatus newStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(newStatus);
    }
}
