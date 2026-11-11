package academy.backend.market_pulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.model.Stock;

/**
 * Проверки очереди и кеша котировок с замоканным внешним источником.
 */
@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private QuoteSource source;

    private QuoteService service;
    private Quote sber;

    @BeforeEach
    void setUp() {
        service = new QuoteService(source);
        var stock = new Stock("SBER", "Сбер", Currency.RUB, "Финансы", BigDecimal.valueOf(5.0));
        sber = new Quote(stock, new BigDecimal("250.00"), new BigDecimal("1.20"));
    }

    @Test
    void repeatedRequestsFetchQuoteOnlyOnce() {
        // Given
        when(source.fetch("SBER")).thenReturn(Optional.of(sber));

        // When
        service.request("SBER");
        service.request("SBER");
        service.processAll();

        // Then
        assertThat(service.cached("SBER")).contains(sber);
        verify(source, times(1)).fetch("SBER");
    }

    @Test
    void repeatedQuoteForReturnsCachedQuote() {
        // Given
        when(source.fetch("SBER")).thenReturn(Optional.of(sber));

        // When
        var first = service.quoteFor("SBER");
        var second = service.quoteFor("SBER");

        // Then
        assertThat(first).contains(sber);
        assertThat(second).contains(sber);
        verify(source, times(1)).fetch("SBER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SBER", "sber", "SbEr"})
    void tickerIsNormalizedToUpperCase(String ticker) {
        // Given
        when(source.fetch("SBER")).thenReturn(Optional.of(sber));

        // When
        var quote = service.quoteFor(ticker);

        // Then
        assertThat(quote).contains(sber);
        assertThat(service.cached("sber")).contains(sber);
        verify(source, times(1)).fetch("SBER");
    }

    @Test
    void unknownTickerIsNotCached() {
        // Given
        when(source.fetch("XXXX")).thenReturn(Optional.empty());

        // When
        var first = service.quoteFor("XXXX");
        var second = service.quoteFor("XXXX");

        // Then
        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
        assertThat(service.cached("XXXX")).isEmpty();
        verify(source, times(2)).fetch("XXXX");
    }

    @Test
    void requestDoesNotFetchUntilProcessing() {
        // Given
        var ticker = "SBER";

        // When
        service.request(ticker);

        // Then
        assertThat(service.cached(ticker)).isEmpty();
        verifyNoInteractions(source);
    }
}
