package com.example.positionbook.repository;

import com.example.positionbook.domain.Position;
import com.example.positionbook.domain.PositionKey;
import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.domain.TradeEventType;
import com.example.positionbook.exception.BadTradeEventException;
import com.example.positionbook.exception.ConflictException;
import com.example.positionbook.exception.PositionOverflowException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class InMemoryPositionBookRepository implements PositionBookRepository {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<PositionKey, Long> quantities = new HashMap<>();
    private final Map<PositionKey, List<TradeEvent>> eventsByPosition = new HashMap<>();
    private final Map<Long, TradeEvent> originalTradesById = new HashMap<>();
    private final Set<Long> cancelledEventIds = new HashSet<>();

    @Override
    public void processTrade(TradeEvent event) {
        lock.writeLock().lock();
        try {
            if (event.type() == TradeEventType.CANCEL) {
                throw new IllegalArgumentException("CANCEL events must be processed through cancelTrade");
            }
            if (originalTradesById.containsKey(event.eventId())) {
                throw new ConflictException("Trade event ID already exists: " + event.eventId());
            }

            PositionKey key = new PositionKey(event.tradingAccount(), event.securityId());
            long current = quantities.getOrDefault(key, 0L);
            long updated = addExact(current, event.signedQuantity());

            quantities.put(key, updated);
            eventsByPosition.computeIfAbsent(key, ignored -> new ArrayList<>()).add(event);
            originalTradesById.put(event.eventId(), event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void cancelTrade(long eventId) {
        lock.writeLock().lock();
        try {
            if (cancelledEventIds.contains(eventId)) {
                throw new ConflictException("Event has already been cancelled: " + eventId);
            }

            TradeEvent original = originalTradesById.get(eventId);
            if (original == null) {
                throw new BadTradeEventException("Cannot cancel unknown event ID: " + eventId);
            }

            PositionKey key = new PositionKey(original.tradingAccount(), original.securityId());
            long current = quantities.getOrDefault(key, 0L);
            long updated = addExact(current, -original.signedQuantity());
            TradeEvent cancellation = new TradeEvent(eventId, TradeEventType.CANCEL,
                    original.tradingAccount(), original.securityId(), 0L);

            quantities.put(key, updated);
            eventsByPosition.computeIfAbsent(key, ignored -> new ArrayList<>()).add(cancellation);
            cancelledEventIds.add(eventId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Position> findPosition(PositionKey key) {
        lock.readLock().lock();
        try {
            Long quantity = quantities.get(key);
            return quantity == null
                    ? Optional.empty()
                    : Optional.of(new Position(key.tradingAccount(), key.securityId(), quantity));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<TradeEvent> findEvents(PositionKey key) {
        lock.readLock().lock();
        try {
            return List.copyOf(eventsByPosition.getOrDefault(key, List.of()));
        } finally {
            lock.readLock().unlock();
        }
    }

    private static long addExact(long current, long delta) {
        try {
            return Math.addExact(current, delta);
        } catch (ArithmeticException ex) {
            throw new PositionOverflowException("Position quantity overflow", ex);
        }
    }
}
