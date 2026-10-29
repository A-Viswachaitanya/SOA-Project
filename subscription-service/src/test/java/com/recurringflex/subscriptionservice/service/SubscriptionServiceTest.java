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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanService planService;

    @Mock
    private UserClient userClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Plan standardPlan;
    private UserResponse mockUser;
    private Subscription activeSubscription;

    @BeforeEach
    void setUp() {
        standardPlan = new Plan();
        standardPlan.setId(1L);
        standardPlan.setName("Pro Monthly");
        standardPlan.setDescription("Monthly plan with premium features");
        standardPlan.setPrice(BigDecimal.valueOf(19.99));
        standardPlan.setDurationInDays(30);
        standardPlan.setActive(true);

        mockUser = new UserResponse(1L, "viswa", "viswa@example.com", "USER", java.time.LocalDateTime.now());

        activeSubscription = new Subscription();
        activeSubscription.setId(100L);
        activeSubscription.setUserId(1L);
        activeSubscription.setPlan(standardPlan);
        activeSubscription.setStartDate(LocalDate.now());
        activeSubscription.setEndDate(LocalDate.now().plusDays(30));
        activeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        activeSubscription.setAutoRenew(true);
    }

    @Test
    @DisplayName("Subscribe: successfully creates an ACTIVE subscription when user and active plan exist")
    void testSubscribe_Success() {
        SubscribeRequest request = new SubscribeRequest();
        request.setUserId(1L);
        request.setPlanId(1L);
        request.setAutoRenew(true);

        when(userClient.getUserById(1L)).thenReturn(mockUser);
        when(planService.getPlanById(1L)).thenReturn(standardPlan);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setStatus("SUCCESS");
        paymentResponse.setTransactionRef("TXN-12345");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

        Subscription result = subscriptionService.subscribe(request);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals(1L, result.getUserId());
        assertEquals(standardPlan, result.getPlan());
        assertEquals(LocalDate.now(), result.getStartDate());
        assertEquals(LocalDate.now().plusDays(30), result.getEndDate());
        assertTrue(result.getAutoRenew());

        verify(subscriptionRepository, atLeastOnce()).save(any(Subscription.class));
        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    @DisplayName("Subscribe: throws exception when user does not exist")
    void testSubscribe_UserNotFound_ThrowsException() {
        SubscribeRequest request = new SubscribeRequest();
        request.setUserId(999L);
        request.setPlanId(1L);

        when(userClient.getUserById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            subscriptionService.subscribe(request);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(subscriptionRepository, never()).save(any());
        verify(paymentClient, never()).processPayment(any());
    }

    @Test
    @DisplayName("Subscribe: throws exception when plan is inactive")
    void testSubscribe_InactivePlan_ThrowsException() {
        standardPlan.setActive(false);

        SubscribeRequest request = new SubscribeRequest();
        request.setUserId(1L);
        request.setPlanId(1L);

        when(userClient.getUserById(1L)).thenReturn(mockUser);
        when(planService.getPlanById(1L)).thenReturn(standardPlan);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            subscriptionService.subscribe(request);
        });

        assertTrue(exception.getMessage().contains("Plan is not active"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel: successfully cancels an ACTIVE subscription and sets autoRenew to false")
    void testCancelSubscription_Success() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        Subscription result = subscriptionService.cancelSubscription(100L);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.CANCELLED, result.getStatus());
        assertFalse(result.getAutoRenew());
        verify(subscriptionRepository, times(1)).save(activeSubscription);
    }

    @Test
    @DisplayName("Cancel: throws exception when subscription is not ACTIVE")
    void testCancelSubscription_NotActive_ThrowsException() {
        activeSubscription.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(activeSubscription));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            subscriptionService.cancelSubscription(100L);
        });

        assertTrue(exception.getMessage().contains("Can only cancel ACTIVE subscriptions"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Renew: successfully extends subscription end date and triggers renewal payment")
    void testRenewSubscription_Success() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(activeSubscription));
        when(paymentClient.processPayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(1L, 100L, 1L, BigDecimal.valueOf(19.99), "SUCCESS", "TXN-1", null));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        Subscription result = subscriptionService.renewSubscription(100L);

        assertNotNull(result);
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals(LocalDate.now().plusDays(30), result.getEndDate());
        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
        verify(subscriptionRepository, times(1)).save(activeSubscription);
    }

    @Test
    @DisplayName("Expire Subscriptions: marks active subscriptions past end date as EXPIRED")
    void testExpireSubscriptions_Success() {
        Subscription expiredSub1 = new Subscription();
        expiredSub1.setId(101L);
        expiredSub1.setUserId(2L);
        expiredSub1.setStatus(SubscriptionStatus.ACTIVE);
        expiredSub1.setEndDate(LocalDate.now().minusDays(1));

        when(subscriptionRepository.findByStatusAndEndDateBefore(eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(expiredSub1));

        subscriptionService.expireSubscriptions();

        assertEquals(SubscriptionStatus.EXPIRED, expiredSub1.getStatus());
        verify(subscriptionRepository, times(1)).save(expiredSub1);
    }

    @Test
    @DisplayName("Auto-Renew: automatically renews eligible subscriptions")
    void testAutoRenewSubscriptions_Success() {
        when(subscriptionRepository.findByStatusAndAutoRenewTrueAndEndDateBefore(eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(activeSubscription));
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(activeSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentClient.processPayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(1L, 100L, 1L, BigDecimal.valueOf(19.99), "SUCCESS", "TXN-1", null));

        subscriptionService.autoRenewSubscriptions();

        assertEquals(SubscriptionStatus.ACTIVE, activeSubscription.getStatus());
        verify(subscriptionRepository, atLeastOnce()).save(activeSubscription);
    }

    @Test
    @DisplayName("Subscribe: cancels the subscription and throws when payment is declined")
    void testSubscribe_PaymentFailed_CancelsSubscription() {
        SubscribeRequest request = new SubscribeRequest(1L, 1L, true);

        when(userClient.getUserById(1L)).thenReturn(mockUser);
        when(planService.getPlanById(1L)).thenReturn(standardPlan);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> {
            Subscription sub = i.getArgument(0);
            sub.setId(200L);
            return sub;
        });
        when(paymentClient.processPayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(1L, 200L, 1L, BigDecimal.valueOf(19.99), "FAILED", "TXN-1", null));

        assertThrows(PaymentFailedException.class, () -> subscriptionService.subscribe(request));

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(2)).save(captor.capture());
        assertEquals(SubscriptionStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Subscribe: cancels the subscription when payment-service is unreachable")
    void testSubscribe_PaymentServiceDown_CancelsSubscription() {
        SubscribeRequest request = new SubscribeRequest(1L, 1L, false);

        when(userClient.getUserById(1L)).thenReturn(mockUser);
        when(planService.getPlanById(1L)).thenReturn(standardPlan);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(PaymentFailedException.class, () -> subscriptionService.subscribe(request));
    }

    @Test
    @DisplayName("Renew: does not extend the subscription when payment fails")
    void testRenewSubscription_PaymentFailed() {
        LocalDate originalEnd = activeSubscription.getEndDate().minusDays(5);
        activeSubscription.setEndDate(originalEnd);
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(activeSubscription));
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(PaymentFailedException.class, () -> subscriptionService.renewSubscription(100L));

        assertEquals(originalEnd, activeSubscription.getEndDate());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete: removes an existing subscription")
    void testDeleteSubscription_Success() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(activeSubscription));

        subscriptionService.deleteSubscription(100L);

        verify(subscriptionRepository).delete(activeSubscription);
    }

    @Test
    @DisplayName("Get by ID: throws ResourceNotFoundException when missing")
    void testGetSubscriptionById_NotFound() {
        when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subscriptionService.getSubscriptionById(999L));
    }
}
