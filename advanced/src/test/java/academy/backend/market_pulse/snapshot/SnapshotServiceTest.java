package academy.backend.market_pulse.snapshot;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotServiceTest {
    @Test void publishesCopyAndCancelsLoader() throws Exception {
        var loading = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var call = new java.util.concurrent.atomic.AtomicInteger();
        var mutable = new java.util.HashMap<>(Map.of("SBER", BigDecimal.TEN));
        var service = new SnapshotService(() -> {
            if (call.incrementAndGet() == 1) { return mutable; }
            loading.countDown();
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException e) { interrupted.countDown(); Thread.currentThread().interrupt(); }
            return Map.of();
        }, Duration.ofMillis(1));
        service.start();
        try {
            var first = service.awaitFirst();
            assertEquals(1, first.version());
            assertEquals(BigDecimal.TEN, first.prices().get("SBER"));
            assertThrows(UnsupportedOperationException.class, () -> first.prices().clear());
            mutable.clear();
            assertEquals(BigDecimal.TEN, first.prices().get("SBER"));
            assertTrue(loading.await(2, TimeUnit.SECONDS));
            assertSame(first, service.current().orElseThrow());
        } finally { service.close(); }
        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        assertEquals(1, service.current().orElseThrow().version());
    }
    @Test void closeWakesAwaiterBeforeFirstResult() throws Exception {
        var entered = new CountDownLatch(1);
        var service = new SnapshotService(() -> {
            entered.countDown();
            try { new CountDownLatch(1).await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return Map.of();
        }, Duration.ofSeconds(1));
        service.start();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        service.close();
        assertThrows(IllegalStateException.class, service::awaitFirst);
        service.close();
    }
}
