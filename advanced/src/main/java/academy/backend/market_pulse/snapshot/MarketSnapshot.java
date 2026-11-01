package academy.backend.market_pulse.snapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Полностью подготовленная версия данных; не содержит изменяемых Instrument. */
public record MarketSnapshot(long version, Instant loadedAt, Map<String, BigDecimal> prices) {
    public MarketSnapshot { prices = Map.copyOf(prices); }
}
