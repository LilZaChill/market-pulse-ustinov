package academy.backend.market_pulse.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Stock;

/**
 * Проверки фильтра по классам эквивалентности и граничным значениям.
 */
class PriceFilterTest {

    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(5.0);

    @ParameterizedTest(name = "{0}, доходность {1} -> {2}")
    @CsvSource({
            "LE, 4.99, true",
            "LE, 5.00, true",
            "LE, 5.01, false",
            "EQ, 4.99, false",
            "EQ, 5.00, true",
            "EQ, 5.01, false",
            "GE, 4.99, false",
            "GE, 5.00, true",
            "GE, 5.01, true"
    })
    void stockIsMatchedByDividendYield(PriceFilter.Operator operator, double dividendYield, boolean expected) {
        // Given
        var stock = new Stock("SBER", "Сбер", Currency.RUB, "Финансы", BigDecimal.valueOf(dividendYield));

        // When
        var matches = new PriceFilter(operator, THRESHOLD).matches(stock);

        // Then
        assertThat(matches).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BOND", "ETF"})
    void nonStockIsNeverMatched(String type) {
        // Given
        var instrument = InstrumentFactories.create(type, "TICKER", "NAME", Currency.RUB);

        for (var operator : PriceFilter.Operator.values()) {
            // When
            var matches = new PriceFilter(operator, THRESHOLD).matches(instrument);

            // Then
            assertThat(matches).as("тип %s, оператор %s", type, operator).isFalse();
        }
    }

    @Test
    void equalYieldMatchesRegardlessOfScale() {
        // Given
        var stock = new Stock("SBER", "Сбер", Currency.RUB, "Финансы", new BigDecimal("5.00"));

        // When
        var matches = new PriceFilter(PriceFilter.Operator.EQ, THRESHOLD).matches(stock);

        // Then
        assertThat(matches).isTrue();
    }
}
