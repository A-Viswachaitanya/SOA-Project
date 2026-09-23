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
import com.recurringflex.subscriptionservice.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

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
        UserResponse user = userClient.getUserById(request.getUserId());
        if (user == null) {
            throw new RuntimeException("User not found with id: " + request.getUserId());
        }

        // Get the plan
        Plan plan = planService.getPlanById(request.getPlanId());
        if (!plan.getActive()) {
            throw new RuntimeException("Plan is not active: " + plan.getName());
        }

        // Process payment via Feign
        PaymentRequest paymentRequest = new PaymentRequest(
                null, // subscription ID will be set after save
                request.getUserId(),
                plan.getPrice()
        );

        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setUserId(request.getUserId());
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationInDays()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : false);

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // Process payment after subscription is created
        try {
            paymentRequest.setSubscriptionId(savedSubscription.getId());
            PaymentResponse paymentResponse = paymentClient.processPayment(paymentRequest);
            if (!"SUCCESS".equals(paymentResponse.getStatus())) {
                // Rollback subscription if payment fails
                savedSubscription.setStatus(SubscriptionStatus.CANCELLED);
                subscriptionRepository.save(savedSubscription);
                throw new RuntimeException("Payment failed for subscription");
            }
        } catch (Exception e) {
            // If payment service is unavailable, keep subscription active (mock behavior)
            System.out.println("Payment processing note: " + e.getMessage());
        }

        return savedSubscription;
    }

    public Subscription getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + id));
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

        // Process renewal payment
        try {
            PaymentRequest paymentRequest = new PaymentRequest(
                    subscription.getId(),
                    subscription.getUserId(),
                    plan.getPrice()
            );
            paymentClient.processPayment(paymentRequest);
        } catch (Exception e) {
            System.out.println("Renewal payment note: " + e.getMessage());
        }

        // Extend subscription
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationInDays()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        return subscriptionRepository.save(subscription);
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
            System.out.println("Expired subscription ID: " + sub.getId() + " for user: " + sub.getUserId());
        }
    }

    // Called by scheduler to auto-renew eligible subscriptions
    public void autoRenewSubscriptions() {
        List<Subscription> renewableSubs = subscriptionRepository
                .findByStatusAndAutoRenewTrueAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDate.now());

        for (Subscription sub : renewableSubs) {
            try {
                renewSubscription(sub.getId());
                System.out.println("Auto-renewed subscription ID: " + sub.getId() + " for user: " + sub.getUserId());
            } catch (Exception e) {
                System.out.println("Failed to auto-renew subscription ID: " + sub.getId() + ": " + e.getMessage());
                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);
            }
        }
    }
}
