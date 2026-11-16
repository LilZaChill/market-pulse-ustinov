package academy.backend.market_pulse.refactoring.selfwork;

/**
 * Задание для самостоятельной работы №2: один и тот же {@code switch} по типу повторяется в трёх
 * методах. Приёмы: Replace Conditional with Polymorphism (или enum с полями), Extract Class. Тесты
 * {@code InstrumentRulesTest} должны оставаться зелёными.
 */
public class InstrumentRulesV1 {

    public int lotSize(String type) {
        return rulesFor(type).lotSize();
    }

    public int settlementDays(String type) {
        return rulesFor(type).settlementDays();
    }

    public String label(String type) {
        return rulesFor(type).label();
    }

    private Rules rulesFor(String type) {
        switch (type) {
            case "STOCK":
                return new Rules(10, 2, "Акция");
            case "BOND":
                return new Rules(1, 1, "Облигация");
            case "ETF":
                return new Rules(1, 2, "Фонд");
            default:
                throw new IllegalArgumentException("Неизвестный тип инструмента: " + type);
        }
    }

    private record Rules(int lotSize, int settlementDays, String label) {
    }
}
