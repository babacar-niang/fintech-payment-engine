package com.babacar.payment.api;

import com.babacar.payment.service.PaymentService;
import com.babacar.payment.service.dto.CreatePaymentRequest;
import com.babacar.payment.service.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a payment",
               description = "Idempotent. Pass a unique Idempotency-Key header to prevent duplicate processing.")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key")
            @Parameter(description = "Unique key to ensure idempotency", required = true)
            String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @GetMapping
    @Operation(summary = "List payments by status")
    public ResponseEntity<List<PaymentResponse>> listPayments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(paymentService.listPayments(status));
    }
}
