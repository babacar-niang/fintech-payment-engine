package com.babacar.payment.service.dto;

import com.babacar.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private String senderId;
    private String receiverId;
    private String reference;
    private PaymentStatus status;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
