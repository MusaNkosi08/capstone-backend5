package za.ac.cput.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    @JsonValue
    public String toString() {
        return displayName;
    }

    @JsonCreator
    public static OrderStatus fromString(String status) {
        for (OrderStatus orderStatus : OrderStatus.values()) {
            if (orderStatus.displayName.equalsIgnoreCase(status) || 
                orderStatus.name().equalsIgnoreCase(status)) {
                return orderStatus;
            }
        }
        return PENDING; // default fallback
    }
}