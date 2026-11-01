package academy.backend.market_pulse.demo.concurrency;

/** ResultProvider из прошлогодних слайдов: данные публикуются через volatile-флаг. */
public final class PublicationDemo {
    private static final class ResultProvider {
        private int result;
        private volatile boolean resultsReady;

        private void complete(int value) {
            result = value;
            resultsReady = true;
        }

        private int await() throws InterruptedException {
            while (!resultsReady) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Ожидание результата отменено");
                }
                Thread.onSpinWait();
            }
            return result;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ResultProvider provider = new ResultProvider();
        // Spin оставлен для сопоставления со слайдами; в прикладном ожидании используем блокировку.
        Thread reader = Thread.ofPlatform().name("result-reader").unstarted(() -> {
            try {
                System.out.println("Опубликовано: " + provider.await());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        reader.start();
        try {
            provider.complete(42);
            reader.join();
        } finally {
            reader.interrupt();
            reader.join();
        }
        // Ошибочный вариант с обычным флагом запускаем только в jcstress (PublicationPlain).
    }
}
