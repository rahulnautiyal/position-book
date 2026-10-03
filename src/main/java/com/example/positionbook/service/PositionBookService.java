package com.example.positionbook.service;

import com.example.positionbook.domain.Position;
import com.example.positionbook.domain.PositionKey;
import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.domain.TradeEventType;
import com.example.positionbook.exception.ResourceNotFoundException;
import com.example.positionbook.repository.PositionBookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PositionBookService {
    private final PositionBookRepository repository;

    public PositionBookService(PositionBookRepository repository) {
        this.repository = repository;
    }

    public void process(TradeEvent event) {
        if (event.type() == TradeEventType.CANCEL) {
            repository.cancelTrade(event.eventId());
        } else {
            repository.processTrade(event);
        }
    }

    public Position getPosition(String tradingAccount, String securityId) {
        PositionKey key = new PositionKey(tradingAccount, securityId);
        return repository.findPosition(key).orElseThrow(() -> new ResourceNotFoundException(
                "Position not found for account='" + tradingAccount + "', security='" + securityId + "'"));
    }

    public PositionDetails getPositionDetails(String tradingAccount, String securityId) {
        PositionKey key = new PositionKey(tradingAccount, securityId);
        Position position = repository.findPosition(key).orElseThrow(() -> new ResourceNotFoundException(
                "Position not found for account='" + tradingAccount + "', security='" + securityId + "'"));
        return new PositionDetails(position, repository.findEvents(key));
    }

    public record PositionDetails(Position position, List<TradeEvent> events) {}
}
