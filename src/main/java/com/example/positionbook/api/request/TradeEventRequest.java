package com.example.positionbook.api.request;

import com.example.positionbook.domain.TradeEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Transport-level representation of an incoming trade event.
 * Business invariants are enforced by the TradeEvent domain object so that
 * events created outside the REST layer receive the same validation.
 */
public record TradeEventRequest(
        @Positive(message = "eventId must be greater than zero")
        long eventId,

        @NotNull(message = "type is required")
        TradeEventType type,

        @Size(max = 100, message = "tradingAccount must not exceed 100 characters")
        String tradingAccount,

        @Size(max = 100, message = "securityId must not exceed 100 characters")
        String securityId,

        Long quantity) {
}
