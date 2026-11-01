package academy.backend.market_pulse.snapshot;

import academy.backend.market_pulse.client.impl.HttpClientTInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotHttpTest {
    @Test void publishesHttpDataAndInterruptsNextRequest() throws Exception {
        var calls = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/instruments", exchange -> {
            if (calls.incrementAndGet() > 1) {
                entered.countDown();
                try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                exchange.close();
                return;
            }
            byte[] body = "[{\"ticker\":\"SBER\",\"name\":\"Sber\",\"type\":\"STOCK\",\"currency\":\"RUB\",\"price\":250,\"changePercent\":1}]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        var client = new HttpClientTInvestApiClient(new TInvestApiProperties("127.0.0.1",
                server.getAddress().getPort(), Duration.ofSeconds(1), Duration.ofSeconds(5)));
        var service = new SnapshotService(() -> client.fetchQuotes().stream().collect(
                Collectors.toMap(q -> q.getInstrument().getTicker(), q -> q.getPrice())), Duration.ofMillis(1));
        try {
            service.start();
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            var first = service.current().orElseThrow();
            assertEquals(0, new java.math.BigDecimal("250").compareTo(first.prices().get("SBER")));
            assertTimeoutPreemptively(Duration.ofSeconds(2), service::close);
            assertSame(first, service.current().orElseThrow());
        } finally { service.close(); release.countDown(); server.stop(0); }
    }
}
