package com.example.positionbook.repository;

import com.example.positionbook.domain.Position;
import com.example.positionbook.domain.PositionKey;
import com.example.positionbook.domain.TradeEvent;

import java.util.List;
import java.util.Optional;

public interface PositionBookRepository {
    /**
     * Atomically validates state, applies the trade to the position and records the event.
     */
    void processTrade(TradeEvent event);

    /**
     * Atomically validates cancellation state, reverses the original trade and records the cancellation.
     */
    void cancelTrade(long eventId);

    Optional<Position> findPosition(PositionKey key);

    List<TradeEvent> findEvents(PositionKey key);
}
