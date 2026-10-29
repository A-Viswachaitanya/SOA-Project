package com.recurringflex.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentRequest {
    @NotNull(message = "subscriptionId is required")
    private Long subscriptionId;

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.00", message = "amount cannot be negative")
    private BigDecimal amount;

    public PaymentRequest() {}
    public PaymentRequest(Long subscriptionId, Long userId, BigDecimal amount) {
        this.subscriptionId = subscriptionId; this.userId = userId; this.amount = amount;
    }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
