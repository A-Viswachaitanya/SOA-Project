package com.recurringflex.paymentservice.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private Long subscriptionId;
    private Long userId;
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
