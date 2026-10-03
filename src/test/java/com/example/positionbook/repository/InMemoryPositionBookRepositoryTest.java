package com.example.positionbook.repository;

import com.example.positionbook.domain.PositionKey;
import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.domain.TradeEventType;
import com.example.positionbook.exception.ConflictException;
import com.example.positionbook.exception.PositionOverflowException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryPositionBookRepositoryTest {
    @Test
    void overflowDoesNotPartiallyPersistTrade() {
        var repository = new InMemoryPositionBookRepository();
        var key = new PositionKey("ACC1", "SEC1");
        repository.processTrade(event(1, TradeEventType.BUY, "ACC1", "SEC1", Long.MAX_VALUE));

        assertThrows(PositionOverflowException.class,
                () -> repository.processTrade(event(2, TradeEventType.BUY, "ACC1", "SEC1", 1)));

        assertEquals(Long.MAX_VALUE, repository.findPosition(key).orElseThrow().quantity());
        assertEquals(1, repository.findEvents(key).size());
    }

    @Test
    void cancellationIsAtomicAndRecordedOnce() {
        var repository = new InMemoryPositionBookRepository();
        var key = new PositionKey("ACC1", "SEC1");
        repository.processTrade(event(1, TradeEventType.BUY, "ACC1", "SEC1", 100));

        repository.cancelTrade(1);
        assertEquals(0, repository.findPosition(key).orElseThrow().quantity());
        assertEquals(2, repository.findEvents(key).size());
        assertThrows(ConflictException.class, () -> repository.cancelTrade(1));
        assertEquals(2, repository.findEvents(key).size());
    }

    private static TradeEvent event(long id, TradeEventType type, String account, String security, long quantity) {
        return new TradeEvent(id, type, account, security, quantity);
    }
}
