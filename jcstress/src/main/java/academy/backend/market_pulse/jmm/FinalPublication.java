package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/** Специальная гарантия final: конструктор не публикует this. */
@JCStressTest
@Outcome(id = {"-1", "42"}, expect = Expect.ACCEPTABLE, desc = "Нет объекта или корректное final-поле")
@Outcome(expect = Expect.FORBIDDEN, desc = "Частично сконструированный объект запрещён для final-поля")
@State
public class FinalPublication {
    private Holder holder;

    @Actor
    public void writer() {
        holder = new Holder();
    }

    @Actor
    public void reader(I_Result result) {
        Holder observed = holder;
        result.r1 = observed == null ? -1 : observed.value;
    }

    private static final class Holder {
        private final int value;

        private Holder() {
            value = 42;
        }
    }
}
