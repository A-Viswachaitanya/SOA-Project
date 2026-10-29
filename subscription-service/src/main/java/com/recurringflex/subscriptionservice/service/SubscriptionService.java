package com.recurringflex.subscriptionservice.service;

import com.recurringflex.subscriptionservice.client.PaymentClient;
import com.recurringflex.subscriptionservice.client.UserClient;
import com.recurringflex.subscriptionservice.dto.PaymentRequest;
import com.recurringflex.subscriptionservice.dto.PaymentResponse;
import com.recurringflex.subscriptionservice.dto.SubscribeRequest;
import com.recurringflex.subscriptionservice.dto.UserResponse;
import com.recurringflex.subscriptionservice.entity.Plan;
import com.recurringflex.subscriptionservice.entity.Subscription;
import com.recurringflex.subscriptionservice.entity.SubscriptionStatus;
import com.recurringflex.subscriptionservice.exception.PaymentFailedException;
import com.recurringflex.subscriptionservice.exception.ResourceNotFoundException;
import com.recurringflex.subscriptionservice.repository.SubscriptionRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanService planService;

    @Autowired
    private UserClient userClient;

    @Autowired
    private PaymentClient paymentClient;

    public Subscription subscribe(SubscribeRequest request) {
        // Validate user exists via Feign
        UserResponse user;
        try {
            user = userClient.getUserById(request.getUserId());
        } catch (FeignException.NotFound e) {
            user = null;
        }
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + request.getUserId());
        }

        // Get the plan
        Plan plan = planService.getPlanById(request.getPlanId());
        if (!plan.getActive()) {
            throw new RuntimeException("Plan is not active: " + plan.getName());
        }

        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setUserId(request.getUserId());
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationInDays()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : false);

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // Process payment after subscription is created (payment needs the subscription ID)
        if (!chargePayment(savedSubscription, plan)) {
            // Roll back: a subscription is only active once it is paid for
            savedSubscription.setStatus(SubscriptionStatus.CANCELLED);
            savedSubscription.setAutoRenew(false);
            subscriptionRepository.save(savedSubscription);
            throw new PaymentFailedException("Payment failed. Subscription " + savedSubscription.getId() + " was cancelled.");
        }

        return savedSubscription;
    }

    public Subscription getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));
    }

    public List<Subscription> getSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public List<Subscription> getActiveSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }

    public Subscription cancelSubscription(Long id) {
        Subscription subscription = getSubscriptionById(id);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("Can only cancel ACTIVE subscriptions. Current status: " + subscription.getStatus());
        }
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        return subscriptionRepository.save(subscription);
    }

    public Subscription renewSubscription(Long id) {
        Subscription subscription = getSubscriptionById(id);

        Plan plan = subscription.getPlan();

        // Only extend the subscription once the renewal payment succeeds
        if (!chargePayment(subscription, plan)) {
            throw new PaymentFailedException("Renewal payment failed for subscription " + subscription.getId());
        }

        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationInDays()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        return subscriptionRepository.save(subscription);
    }

    public void deleteSubscription(Long id) {
        Subscription subscription = getSubscriptionById(id);
        subscriptionRepository.delete(subscription);
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    // Called by scheduler to expire subscriptions past end date
    public void expireSubscriptions() {
        List<Subscription> expiredSubs = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDate.now());

        for (Subscription sub : expiredSubs) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Expired subscription ID: {} for user: {}", sub.getId(), sub.getUserId());
        }
    }

    // Called by scheduler to auto-renew eligible subscriptions
    public void autoRenewSubscriptions() {
        List<Subscription> renewableSubs = subscriptionRepository
                .findByStatusAndAutoRenewTrueAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDate.now());

        for (Subscription sub : renewableSubs) {
            try {
                renewSubscription(sub.getId());
                log.info("Auto-renewed subscription ID: {} for user: {}", sub.getId(), sub.getUserId());
            } catch (Exception e) {
                log.warn("Failed to auto-renew subscription ID: {}: {}", sub.getId(), e.getMessage());
                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);
            }
        }
    }

    // Charges the plan price via payment-service; false if declined or the service is unreachable
    private boolean chargePayment(Subscription subscription, Plan plan) {
        PaymentRequest paymentRequest = new PaymentRequest(
                subscription.getId(),
                subscription.getUserId(),
                plan.getPrice()
        );
        try {
            PaymentResponse paymentResponse = paymentClient.processPayment(paymentRequest);
            return paymentResponse != null && "SUCCESS".equals(paymentResponse.getStatus());
        } catch (Exception e) {
            log.warn("Payment processing failed for subscription {}: {}", subscription.getId(), e.getMessage());
            return false;
        }
    }
}
