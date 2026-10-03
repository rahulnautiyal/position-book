package com.example.positionbook.api.response;

import com.example.positionbook.domain.Position;

public record PositionResponse(String tradingAccount, String securityId, long quantity) {
    public static PositionResponse from(Position position) {
        return new PositionResponse(position.tradingAccount(), position.securityId(), position.quantity());
    }
}
