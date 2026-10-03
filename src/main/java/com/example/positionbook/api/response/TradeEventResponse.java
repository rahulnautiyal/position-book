package com.example.positionbook.api.response;

import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.domain.TradeEventType;

public record TradeEventResponse(long eventId, TradeEventType type, String tradingAccount, String securityId, long quantity) {
    public static TradeEventResponse from(TradeEvent event) {
        return new TradeEventResponse(event.eventId(), event.type(), event.tradingAccount(), event.securityId(), event.quantity());
    }
}
