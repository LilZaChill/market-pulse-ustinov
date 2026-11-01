package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/** Две записи и два чтения: сравнить допустимость исхода (0, 0). */
@JCStressTest
@Outcome(id = {"1, 1", "0, 1", "1, 0"}, expect = Expect.ACCEPTABLE, desc = "Согласованный исход")
@Outcome(id = "0, 0", expect = Expect.FORBIDDEN, desc = "Обе записи не наблюдались")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class DekkerVolatile {
    private volatile int x;
    private volatile int y;

    @Actor
    public void firstActor(II_Result result) {
        x = 1;
        result.r1 = y;
    }

    @Actor
    public void secondActor(II_Result result) {
        y = 1;
        result.r2 = x;
    }
}
