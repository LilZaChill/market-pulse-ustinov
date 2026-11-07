package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class QuoteSubscriptionServiceTest {
    @Test
    void noOverlapAndNoQueuedRequestAfterExpiry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> output = new CopyOnWriteArrayList<>();
        try (QuoteSubscriptionService service = new QuoteSubscriptionService(2, ticker -> {
            calls.incrementAndGet();
            started.countDown();
            release.await();
            return BigDecimal.TEN;
        }, output::add)) {
            service.watch("SBER", Duration.ofMillis(10), Duration.ofMillis(100));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            Thread.sleep(180);
            assertEquals(1, calls.get());
            release.countDown();
            Thread.sleep(100);
            assertEquals(1, calls.get());
        }
        assertTrue(output.stream().anyMatch(line -> line.contains("10")));
    }

    @Test
    void queuedRequestIsSkippedIfItExpiresBeforeWorkerIsFree() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch firstDone = new CountDownLatch(1);
        AtomicInteger expiredCalls = new AtomicInteger();
        try (QuoteSubscriptionService service = new QuoteSubscriptionService(1, ticker -> {
            if (ticker.equals("SBER")) {
                started.countDown();
                release.await();
            } else {
                expiredCalls.incrementAndGet();
            }
            return BigDecimal.ONE;
        }, line -> firstDone.countDown())) {
            service.watch("SBER", Duration.ofSeconds(1), Duration.ofSeconds(1));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            service.watch("GAZP", Duration.ofMillis(10), Duration.ofMillis(40));
            Thread.sleep(100); // Намеренно пересекаем дедлайн, пока единственный worker занят.
            release.countDown();
            assertTrue(firstDone.await(2, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertEquals(0, expiredCalls.get());
        }
    }

    @Test
    void errorsAreRetriedAndExitInterruptsRequest() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch retryStarted = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        List<String> output = new CopyOnWriteArrayList<>();
        QuoteSubscriptionService service = new QuoteSubscriptionService(1, ticker -> {
            if (calls.incrementAndGet() == 1) { throw new IllegalStateException("network failed"); }
            retryStarted.countDown();
            try { Thread.sleep(60_000); } catch (InterruptedException e) { interrupted.countDown(); throw e; }
            return BigDecimal.ONE;
        }, output::add);
        try {
            service.watch("sber", Duration.ofMillis(10), Duration.ofSeconds(5));
            assertTrue(retryStarted.await(2, TimeUnit.SECONDS));
            assertTrue(output.stream().anyMatch(line -> line.contains("network failed")));
        } finally {
            service.close();
        }
        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class,
                () -> service.watch("GAZP", Duration.ofSeconds(1), Duration.ofSeconds(1)));
    }
}
