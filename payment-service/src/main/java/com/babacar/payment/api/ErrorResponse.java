package com.babacar.payment.api;

public record ErrorResponse(
        String code,
        String message
) {
}