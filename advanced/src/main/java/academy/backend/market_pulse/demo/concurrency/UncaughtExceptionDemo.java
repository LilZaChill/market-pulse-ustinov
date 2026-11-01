package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.atomic.AtomicReference;

/** finally → обработчик в падающем потоке → TERMINATED; join не перебрасывает его исключение. */
public final class UncaughtExceptionDemo {
    private static void fail() { throw new IllegalStateException("Ошибка фоновой задачи"); }

    public static void main(String[] args) throws InterruptedException {
        var observed = new AtomicReference<Throwable>();
        Thread worker = Thread.ofPlatform().name("failing-worker")
                .uncaughtExceptionHandler((thread, error) -> {
                    System.out.println("handler: " + thread.getName() + ", выполняется в "
                            + Thread.currentThread().getName() + ", ошибка: " + error);
                    observed.set(error);
                }).unstarted(() -> {
                    try { fail(); }
                    finally { System.out.println("finally: освобождение ресурсов перед handler"); }
                });
        worker.start();
        worker.join(); // исключение работника не перебрасывается в main
        System.out.println("После join: " + worker.getState() + "; main продолжает работу");
        if (!(observed.get() instanceof IllegalStateException)
                || worker.getState() != Thread.State.TERMINATED) {
            throw new AssertionError("Обработчик должен получить ошибку, работник — завершиться");
        }
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                    System.out.println("default handler: " + thread.getName() + ": " + error));
            Thread other = Thread.ofPlatform().name("default-handler-worker")
                    .start(UncaughtExceptionDemo::fail);
            other.join();
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous); // настройка общая для JVM
        }
    }
}
