package academy.backend.market_pulse.concurrent;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Диспетчер считает сроки, пул занимается только HTTP. Все поля подписок защищены gate. */
public final class QuoteSubscriptionService implements AutoCloseable {
    @FunctionalInterface
    public interface PriceLoader {
        BigDecimal load(String ticker) throws Exception;
    }

    private static final long MAX_NANOS = Duration.ofDays(7).toNanos();
    private final Object gate = new Object();
    private final Map<String, Subscription> subscriptions = new LinkedHashMap<>();
    private final TreiberThreadPool pool;
    private final Thread dispatcher;
    private final PriceLoader loader;
    private final Consumer<String> output;
    private boolean closed;

    private static final class Subscription {
        private final String ticker;
        private final long interval;
        private final long expiresAt;
        private long nextRun;
        private boolean inFlight;

        private Subscription(String ticker, long interval, long lifetime) {
            this.ticker = ticker;
            this.interval = interval;
            nextRun = System.nanoTime();
            expiresAt = nextRun + lifetime;
        }
    }

    public QuoteSubscriptionService(int workers, PriceLoader loader, Consumer<String> output) {
        this.loader = Objects.requireNonNull(loader);
        this.output = Objects.requireNonNull(output);
        pool = new TreiberThreadPool(workers, error -> output.accept("Ошибка задачи: " + error));
        dispatcher = Thread.ofPlatform().name("quote-dispatcher").start(this::dispatch);
    }

    public void watch(String ticker, Duration interval, Duration lifetime) {
        long delay = positiveNanos(interval);
        long ttl = positiveNanos(lifetime);
        String normalized = Objects.requireNonNull(ticker).trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9._-]{1,32}")) {
            throw new IllegalArgumentException("Некорректный тикер");
        }
        synchronized (gate) {
            if (closed) {
                throw new IllegalStateException("Сервис закрыт");
            }
            if (subscriptions.containsKey(normalized)) {
                throw new IllegalArgumentException("Подписка уже существует: " + normalized);
            }
            subscriptions.put(normalized, new Subscription(normalized, delay, ttl));
            gate.notifyAll();
        }
    }

    private static long positiveNanos(Duration duration) {
        Objects.requireNonNull(duration);
        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("Интервал и срок: больше 0, не больше 7 дней");
        }
        return duration.toNanos();
    }

    private void dispatch() {
        try {
            while (true) {
                List<Subscription> due = new ArrayList<>();
                synchronized (gate) {
                    if (closed) {
                        return;
                    }
                    long now = System.nanoTime();
                    long waitNanos = MAX_NANOS;
                    var iterator = subscriptions.values().iterator();
                    while (iterator.hasNext()) {
                        Subscription subscription = iterator.next();
                        if (now - subscription.expiresAt >= 0) {
                            // Сохраняем выполняющуюся подписку до completion, без повторной регистрации.
                            if (!subscription.inFlight) {
                                iterator.remove();
                            }
                            continue;
                        }
                        if (!subscription.inFlight && now - subscription.nextRun >= 0) {
                            subscription.inFlight = true;
                            due.add(subscription);
                        }
                        long wakeAt = subscription.inFlight ? subscription.expiresAt
                                : Math.min(subscription.nextRun - now, subscription.expiresAt - now) + now;
                        waitNanos = Math.min(waitNanos, wakeAt - now);
                    }
                    if (due.isEmpty()) {
                        long millis = Math.max(1, Math.min(1_000, waitNanos / 1_000_000));
                        gate.wait(millis);
                        continue; // condition loop: notify не означает, что работа уже появилась
                    }
                }
                for (Subscription subscription : due) {
                    try {
                        pool.submit(() -> refresh(subscription));
                    } catch (IllegalStateException error) {
                        synchronized (gate) {
                            if (!closed) {
                                throw error;
                            }
                        }
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void refresh(Subscription subscription) {
        try {
            synchronized (gate) {
                // Срок мог истечь, пока задача лежала в стеке.
                if (closed || System.nanoTime() - subscription.expiresAt >= 0) {
                    return;
                }
            }
            BigDecimal price = loader.load(subscription.ticker);
            if (!Thread.currentThread().isInterrupted()) {
                output.accept("[" + subscription.ticker + "] " + price);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception error) {
            if (!Thread.currentThread().isInterrupted()) {
                output.accept("[" + subscription.ticker + "] Ошибка: " + error.getMessage());
            }
        } finally {
            synchronized (gate) {
                subscription.inFlight = false;
                subscription.nextRun = System.nanoTime() + subscription.interval; // fixed delay
                gate.notifyAll();
            }
        }
    }

    @Override
    public void close() throws InterruptedException {
        synchronized (gate) {
            closed = true;
            gate.notifyAll();
        }
        dispatcher.interrupt();
        pool.shutdown();
        dispatcher.join();
        pool.awaitTermination();
    }
}
