package com.recurringflex.paymentservice.repository;

import com.recurringflex.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionId(Long subscriptionId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(String status);
}
