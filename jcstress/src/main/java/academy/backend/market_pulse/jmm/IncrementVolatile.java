package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/** Составной инкремент: volatile не заменяет атомарное read-modify-write. */
@JCStressTest
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Оба изменения сохранены")
@Outcome(id = "1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Потерянное изменение")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class IncrementVolatile {
    private volatile int counter;

    @Actor
    public void firstActor() {
        counter++;
    }

    @Actor
    public void secondActor() {
        counter++;
    }

    @Arbiter
    public void readResult(I_Result result) {
        result.r1 = counter;
    }
}
