package org.example.notificationservice.infrastructure.kafka.event;

/**
 * Constants for Kafka event types
 */
public final class EventTypes {

    private EventTypes() {}

    // Order Events
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    // User Events
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";

    // Payment Events
    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    // Seller Events
    public static final String SELLER_APPROVED = "SELLER_APPROVED";
    public static final String SELLER_REJECTED = "SELLER_REJECTED";
    public static final String SELLER_SUSPENDED = "SELLER_SUSPENDED";
    public static final String SELLER_BANNED = "SELLER_BANNED";
    public static final String SELLER_REACTIVATED = "SELLER_REACTIVATED";
}

