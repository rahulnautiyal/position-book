package com.example.positionbook.domain;

import com.example.positionbook.exception.InvalidTradeEventException;

public record TradeEvent(long eventId, TradeEventType type, String tradingAccount, String securityId, long quantity) {
    public TradeEvent {
        if (eventId <= 0) {
            throw new InvalidTradeEventException("eventId must be greater than zero");
        }
        if (type == null) {
            throw new InvalidTradeEventException("type is required");
        }

        if (type == TradeEventType.CANCEL) {
            if (quantity != 0) {
                throw new InvalidTradeEventException("quantity must be zero for CANCEL events");
            }
        } else {
            if (tradingAccount == null || tradingAccount.isBlank()) {
                throw new InvalidTradeEventException("tradingAccount is required for BUY and SELL events");
            }
            if (securityId == null || securityId.isBlank()) {
                throw new InvalidTradeEventException("securityId is required for BUY and SELL events");
            }
            if (quantity <= 0) {
                throw new InvalidTradeEventException("quantity must be greater than zero for BUY and SELL events");
            }
        }
    }

    public long signedQuantity() {
        return switch (type) {
            case BUY -> quantity;
            case SELL -> -quantity;
            case CANCEL -> 0;
        };
    }
}
