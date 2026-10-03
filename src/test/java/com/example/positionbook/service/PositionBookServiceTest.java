package com.example.positionbook.service;

import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.domain.TradeEventType;
import com.example.positionbook.exception.BadTradeEventException;
import com.example.positionbook.exception.ConflictException;
import com.example.positionbook.exception.PositionOverflowException;
import com.example.positionbook.exception.ResourceNotFoundException;
import com.example.positionbook.repository.InMemoryPositionBookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionBookServiceTest {
    private PositionBookService service;

    @BeforeEach
    void setUp() {
        service = new PositionBookService(new InMemoryPositionBookRepository());
    }

    @Test
    void buysAreAggregated() {
        service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(2, TradeEventType.BUY, "ACC1", "SEC1", 50));
        assertEquals(150, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void positionsAreIndependentByAccountAndSecurity() {
        service.process(e(3, TradeEventType.BUY, "ACC1", "SEC1", 12));
        service.process(e(4, TradeEventType.BUY, "ACC1", "SECXYZ", 50));
        service.process(e(5, TradeEventType.BUY, "ACC2", "SECXYZ", 33));
        service.process(e(6, TradeEventType.BUY, "ACC1", "SEC1", 20));
        assertEquals(32, service.getPosition("ACC1", "SEC1").quantity());
        assertEquals(50, service.getPosition("ACC1", "SECXYZ").quantity());
        assertEquals(33, service.getPosition("ACC2", "SECXYZ").quantity());
    }

    @Test
    void sellsReducePosition() {
        service.process(e(10, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(11, TradeEventType.SELL, "ACC1", "SEC1", 50));
        assertEquals(50, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void shortPositionsAreAllowed() {
        service.process(e(10, TradeEventType.SELL, "ACC1", "SEC1", 50));
        assertEquals(-50, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void cancellationUsesSameIdAndReversesTrade() {
        service.process(e(21, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(21, TradeEventType.CANCEL, "IGNORED", "IGNORED", 0));
        service.process(e(22, TradeEventType.BUY, "ACC1", "SEC1", 5));
        assertEquals(5, service.getPosition("ACC1", "SEC1").quantity());
        var events = service.getPositionDetails("ACC1", "SEC1").events();
        assertEquals(3, events.size());
        assertEquals(TradeEventType.CANCEL, events.get(1).type());
        assertEquals("ACC1", events.get(1).tradingAccount());
        assertEquals("SEC1", events.get(1).securityId());
        assertEquals(0, events.get(1).quantity());
    }

    @Test
    void sellingTradeCanBeCancelled() {
        service.process(e(30, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(31, TradeEventType.SELL, "ACC1", "SEC1", 40));
        service.process(e(31, TradeEventType.CANCEL, "X", "Y", 0));
        assertEquals(100, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void duplicateTradeIdIsRejectedWithoutChangingPosition() {
        service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", 100));
        assertThrows(ConflictException.class, () -> service.process(e(1, TradeEventType.SELL, "ACC1", "SEC1", 50)));
        assertEquals(100, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void duplicateCancellationIsRejectedWithoutChangingPosition() {
        service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(1, TradeEventType.CANCEL, "x", "y", 0));
        assertThrows(ConflictException.class, () -> service.process(e(1, TradeEventType.CANCEL, "x", "y", 0)));
        assertEquals(0, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void unknownCancellationIsRejected() {
        assertThrows(BadTradeEventException.class, () -> service.process(e(999, TradeEventType.CANCEL, "x", "y", 0)));
    }

    @Test
    void zeroTradeQuantityIsRejected() {
        assertThrows(BadTradeEventException.class, () -> service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", 0)));
        assertThrows(BadTradeEventException.class, () -> service.process(e(2, TradeEventType.SELL, "ACC1", "SEC1", 0)));
    }

    @Test
    void unknownPositionThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.getPosition("ACC1", "SEC1"));
    }

    @Test
    void drilldownContainsEventsInOrder() {
        service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(2, TradeEventType.SELL, "ACC1", "SEC1", 50));
        var details = service.getPositionDetails("ACC1", "SEC1");
        assertEquals(2, details.events().size());
        assertEquals(1, details.events().get(0).eventId());
        assertEquals(2, details.events().get(1).eventId());
    }

    @Test
    void zeroPositionRemainsRetrievableAfterCancellation() {
        service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(1, TradeEventType.CANCEL, "x", "y", 0));
        assertEquals(0, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void overflowIsRejected() {
        service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", Long.MAX_VALUE));
        assertThrows(PositionOverflowException.class,
                () -> service.process(e(2, TradeEventType.BUY, "ACC1", "SEC1", 1)));
        assertEquals(Long.MAX_VALUE, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void cancellationDoesNotUseCancellationPayload() {
        service.process(e(1, TradeEventType.BUY, "ACC1", "SEC1", 100));
        service.process(e(1, TradeEventType.CANCEL, "OTHER", "OTHER_SECURITY", 0));
        assertThrows(ResourceNotFoundException.class, () -> service.getPosition("OTHER", "OTHER_SECURITY"));
        assertEquals(0, service.getPosition("ACC1", "SEC1").quantity());
    }

    private static TradeEvent e(long id, TradeEventType type, String account, String security, long quantity) {
        return new TradeEvent(id, type, account, security, quantity);
    }
}
