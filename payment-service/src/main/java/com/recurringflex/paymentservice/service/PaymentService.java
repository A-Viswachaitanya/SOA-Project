package com.recurringflex.paymentservice.service;

import com.recurringflex.paymentservice.dto.PaymentRequest;
import com.recurringflex.paymentservice.entity.Payment;
import com.recurringflex.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public Payment processPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setSubscriptionId(request.getSubscriptionId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setPaymentDate(LocalDateTime.now());

        // Mock payment processing - always succeeds
        payment.setStatus("SUCCESS");

        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }

    public List<Payment> getPaymentsBySubscriptionId(Long subscriptionId) {
        return paymentRepository.findBySubscriptionId(subscriptionId);
    }

    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
