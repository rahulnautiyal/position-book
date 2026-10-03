package com.example.positionbook.service;

import com.example.positionbook.domain.TradeEvent;
import com.example.positionbook.domain.TradeEventType;
import com.example.positionbook.exception.ConflictException;
import com.example.positionbook.repository.InMemoryPositionBookRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionBookConcurrencyTest {
    @Test
    void concurrentTradesAreAppliedExactlyOnce() throws Exception {
        var service = new PositionBookService(new InMemoryPositionBookRepository());
        int workers = 20;
        int tradesPerWorker = 500;
        var start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(workers)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                int workerId = worker;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < tradesPerWorker; i++) {
                        long eventId = 1L + (long) workerId * tradesPerWorker + i;
                        service.process(new TradeEvent(eventId, TradeEventType.BUY, "ACC1", "SEC1", 1));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertEquals((long) workers * tradesPerWorker,
                service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void concurrentCancellationSucceedsExactlyOnce() throws Exception {
        var service = new PositionBookService(new InMemoryPositionBookRepository());
        service.process(new TradeEvent(1, TradeEventType.BUY, "ACC1", "SEC1", 100));

        int workers = 20;
        var start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(workers)) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        service.process(new TradeEvent(1, TradeEventType.CANCEL, "", "", 0));
                        return true;
                    } catch (ConflictException expected) {
                        return false;
                    }
                }));
            }
            start.countDown();
            long successes = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        }

        assertEquals(0, service.getPosition("ACC1", "SEC1").quantity());
        assertEquals(1, service.getPositionDetails("ACC1", "SEC1").events().stream()
                .filter(event -> event.type() == TradeEventType.CANCEL)
                .count());
    }
}
