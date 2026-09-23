package com.recurringflex.paymentservice.service;

import com.recurringflex.paymentservice.dto.PaymentRequest;
import com.recurringflex.paymentservice.entity.Payment;
import com.recurringflex.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = new Payment();
        samplePayment.setId(1L);
        samplePayment.setSubscriptionId(10L);
        samplePayment.setUserId(2L);
        samplePayment.setAmount(BigDecimal.valueOf(29.99));
        samplePayment.setStatus("SUCCESS");
        samplePayment.setTransactionRef("TXN-ABCD1234");
    }

    @Test
    @DisplayName("Process Payment: generates transaction ref and saves payment with SUCCESS status")
    void testProcessPayment_Success() {
        PaymentRequest request = new PaymentRequest(10L, 2L, BigDecimal.valueOf(29.99));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        Payment result = paymentService.processPayment(request);

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(BigDecimal.valueOf(29.99), result.getAmount());
        assertEquals(10L, result.getSubscriptionId());
        assertEquals(2L, result.getUserId());
        assertNotNull(result.getTransactionRef());
        assertTrue(result.getTransactionRef().startsWith("TXN-"));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Get Payment by ID: returns payment when exists")
    void testGetPaymentById_Success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));

        Payment result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("TXN-ABCD1234", result.getTransactionRef());
    }

    @Test
    @DisplayName("Get Payment by ID: throws RuntimeException when not found")
    void testGetPaymentById_NotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPaymentById(99L));
    }

    @Test
    @DisplayName("Get Payments by User ID: returns user transaction history")
    void testGetPaymentsByUserId() {
        when(paymentRepository.findByUserId(2L)).thenReturn(List.of(samplePayment));

        List<Payment> list = paymentService.getPaymentsByUserId(2L);

        assertEquals(1, list.size());
        assertEquals(2L, list.get(0).getUserId());
    }
}
