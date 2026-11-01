package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.snapshot.SnapshotService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "snapshot", description = "Фоновый снимок котировок: show / exit")
public final class SnapshotCommand implements Callable<Integer> {
    private final TInvestApiClient client;
    @Option(names = "--interval-ms", defaultValue = "2000") private long intervalMs;
    public SnapshotCommand(TInvestApiClient client) { this.client = client; }

    @Override public Integer call() throws Exception {
        try (var service = new SnapshotService(() -> client.fetchQuotes().stream().collect(
                Collectors.toMap(q -> q.getInstrument().getTicker(), q -> q.getPrice())),
                Duration.ofMillis(intervalMs))) {
            service.start();
            var in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            System.out.println("Фоновое обновление запущено. show — последний снимок; exit — выход.");
            String line;
            while ((line = in.readLine()) != null) {
                String command = line.trim().toLowerCase(Locale.ROOT);
                if (command.equals("exit")) { break; }
                if (command.isEmpty()) { continue; }
                if (!command.equals("show")) { System.out.println("Команды: show / exit"); continue; }
                service.current().ifPresentOrElse(snapshot -> {
                    System.out.println("Версия " + snapshot.version() + " от " + snapshot.loadedAt());
                    snapshot.prices().forEach((ticker, price) -> System.out.println(ticker + " = " + price));
                }, () -> System.out.println("Первый снимок ещё загружается"));
                service.lastFailure().ifPresent(error -> System.out.println("Последняя ошибка: " + error));
            }
        }
        return 0;
    }
}
