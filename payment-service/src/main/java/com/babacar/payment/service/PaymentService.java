package com.babacar.payment.service;

import com.babacar.payment.domain.OutboxEvent;
import com.babacar.payment.domain.Payment;
import com.babacar.payment.domain.PaymentStatus;
import com.babacar.payment.observability.PaymentMetrics;
import com.babacar.payment.service.dto.CreatePaymentRequest;
import com.babacar.payment.service.dto.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentMetrics metrics;
    private final ObjectMapper objectMapper;

    /**
     * Creates a payment using idempotency key to prevent duplicate processing.
     * Payment + outbox event are written in a single transaction (Outbox Pattern).
     */
    @Transactional
    public PaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request) {
        // Idempotency check — return existing response if key already seen
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> {
                    log.info("Idempotent request detected for key: {}", idempotencyKey);
                    return toResponse(existing);
                })
                .orElseGet(() -> {
                    Payment payment = Payment.builder()
                            .idempotencyKey(idempotencyKey)
                            .amount(request.getAmount())
                            .currency(request.getCurrency())
                            .senderId(request.getSenderId())
                            .receiverId(request.getReceiverId())
                            .reference(request.getReference())
                            .status(PaymentStatus.PENDING)
                            .build();

                    payment = paymentRepository.save(payment);

                    // Write outbox event in same transaction — guarantees consistency
                    writeOutboxEvent(payment);

                    metrics.incrementCreated();
                    log.info("Payment created: {} for amount {} {}", payment.getId(), payment.getAmount(), payment.getCurrency());

                    return toResponse(payment);
                });
    }

    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return toResponse(payment);
    }

    public List<PaymentResponse> listPayments(String status) {
        List<Payment> payments = (status != null)
                ? paymentRepository.findByStatus(PaymentStatus.valueOf(status.toUpperCase()))
                : paymentRepository.findAll();
        return payments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void markCompleted(UUID paymentId) {
        paymentRepository.findById(paymentId).ifPresent(p -> {
            p.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(p);
            metrics.incrementCompleted();
            log.info("Payment completed: {}", paymentId);
        });
    }

    @Transactional
    public void markFailed(UUID paymentId, String reason) {
        paymentRepository.findById(paymentId).ifPresent(p -> {
            p.setStatus(PaymentStatus.FAILED);
            p.setFailureReason(reason);
            paymentRepository.save(p);
            metrics.incrementFailed();
            log.warn("Payment failed: {} reason: {}", paymentId, reason);
        });
    }

    private void writeOutboxEvent(Payment payment) {
        try {
            String payload = objectMapper.writeValueAsString(toResponse(payment));
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Payment")
                    .aggregateId(payment.getId().toString())
                    .eventType("PaymentCreated")
                    .payload(payload)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write outbox event for payment " + payment.getId(), e);
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .idempotencyKey(p.getIdempotencyKey())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .senderId(p.getSenderId())
                .receiverId(p.getReceiverId())
                .reference(p.getReference())
                .status(p.getStatus())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
