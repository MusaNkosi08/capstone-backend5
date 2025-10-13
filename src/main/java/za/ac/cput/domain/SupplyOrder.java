package za.ac.cput.domain;

/**
 * Deprecated placeholder for SupplyOrder. Supply order functionality removed.
 */
public final class SupplyOrder {
    private SupplyOrder() { }

    // If supply orders are reintroduced, providing a helper to create timestamps
    public static java.time.LocalDateTime now() {
        return java.time.LocalDateTime.now();
    }
}
