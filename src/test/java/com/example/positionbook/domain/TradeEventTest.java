package com.example.positionbook.domain;

import com.example.positionbook.exception.InvalidTradeEventException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeEventTest {
    @Test
    void buyAndSellRequirePositiveQuantity() {
        assertThrows(InvalidTradeEventException.class,
                () -> new TradeEvent(1, TradeEventType.BUY, "ACC1", "SEC1", 0));
        assertThrows(InvalidTradeEventException.class,
                () -> new TradeEvent(2, TradeEventType.SELL, "ACC1", "SEC1", -1));
    }

    @Test
    void cancelRequiresZeroQuantity() {
        assertDoesNotThrow(() -> new TradeEvent(1, TradeEventType.CANCEL, "", "", 0));
        assertThrows(InvalidTradeEventException.class,
                () -> new TradeEvent(1, TradeEventType.CANCEL, "", "", 1));
    }

    @Test
    void buyAndSellRequireAccountAndSecurity() {
        assertThrows(InvalidTradeEventException.class,
                () -> new TradeEvent(1, TradeEventType.BUY, "", "SEC1", 1));
        assertThrows(InvalidTradeEventException.class,
                () -> new TradeEvent(2, TradeEventType.SELL, "ACC1", "", 1));
    }
}
