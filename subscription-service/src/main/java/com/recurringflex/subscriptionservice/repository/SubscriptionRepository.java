package com.recurringflex.subscriptionservice.repository;

import com.recurringflex.subscriptionservice.entity.Subscription;
import com.recurringflex.subscriptionservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserId(Long userId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);

    List<Subscription> findByStatusAndAutoRenewTrueAndEndDateBefore(SubscriptionStatus status, LocalDate date);

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
}
