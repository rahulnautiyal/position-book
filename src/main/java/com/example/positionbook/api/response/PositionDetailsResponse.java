package com.example.positionbook.api.response;

import com.example.positionbook.domain.Position;
import java.util.List;

public record PositionDetailsResponse(PositionResponse position, List<TradeEventResponse> events) {
    public static PositionDetailsResponse of(Position position, List<com.example.positionbook.domain.TradeEvent> events) {
        return new PositionDetailsResponse(PositionResponse.from(position), events.stream().map(TradeEventResponse::from).toList());
    }
}
