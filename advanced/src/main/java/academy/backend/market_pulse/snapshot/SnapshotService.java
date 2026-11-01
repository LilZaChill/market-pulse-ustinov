package academy.backend.market_pulse.snapshot;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Один писатель, много читателей. HTTP выполняется вне монитора, публикация — одной ссылкой. */
public final class SnapshotService implements AutoCloseable {
    private final Object gate = new Object();
    private final Supplier<Map<String, BigDecimal>> loader;
    private final Duration interval;
    private final Thread worker;
    private volatile MarketSnapshot current;
    private volatile String lastFailure;
    private volatile boolean stopped;
    private long version; // изменяет только worker

    public SnapshotService(Supplier<Map<String, BigDecimal>> loader, Duration interval) {
        this.loader = java.util.Objects.requireNonNull(loader);
        this.interval = java.util.Objects.requireNonNull(interval);
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Интервал должен быть положительным");
        }
        worker = Thread.ofPlatform().name("market-snapshot-updater").unstarted(this::update);
    }

    public void start() {
        synchronized (gate) {
            if (stopped) { throw new IllegalStateException("Сервис закрыт"); }
            worker.start(); // повторный запуск запрещён контрактом Thread
        }
    }

    /** Одна volatile-загрузка: читатель получает целую версию. */
    public Optional<MarketSnapshot> current() { return Optional.ofNullable(current); }
    public Optional<String> lastFailure() { return Optional.ofNullable(lastFailure); }

    /** Учебный мониторный протокол: вызывается после start, не из потока worker. */
    public MarketSnapshot awaitFirst() throws InterruptedException {
        synchronized (gate) {
            while (current == null && !stopped) { gate.wait(); }
            if (current == null) { throw new IllegalStateException("Закрыт до первого снимка"); }
            return current;
        }
    }

    private void update() {
        while (!stopped && !Thread.currentThread().isInterrupted()) {
            try {
                Map<String, BigDecimal> prices = loader.get();
                if (Thread.currentThread().isInterrupted()) { return; }
                MarketSnapshot next = new MarketSnapshot(version + 1, Instant.now(), prices);
                synchronized (gate) {
                    if (stopped) { return; }
                    current = next; // точка безопасной публикации
                    version++;
                    lastFailure = null;
                    gate.notifyAll();
                }
            } catch (RuntimeException e) {
                if (stopped || Thread.currentThread().isInterrupted()) { return; }
                lastFailure = e.toString(); // старый полный снимок остаётся доступным
            }
            try { Thread.sleep(interval); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    @Override public void close() throws InterruptedException {
        synchronized (gate) { stopped = true; gate.notifyAll(); }
        worker.interrupt();
        worker.join(); // loader обязан поддерживать отмену; выбранный HttpClient это делает
    }
}
