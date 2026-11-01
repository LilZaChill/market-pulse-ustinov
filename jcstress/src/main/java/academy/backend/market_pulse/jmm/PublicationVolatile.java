package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/** Одноразовая публикация: читатель проверяет флаг ровно один раз, без spin. */
@JCStressTest
@Outcome(id = {"-1", "42"}, expect = Expect.ACCEPTABLE, desc = "Флаг ещё не виден или данные опубликованы")
@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Флаг виден, данные не видны")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class PublicationVolatile {
    private int data;
    private volatile boolean ready;

    @Actor
    public void writer() {
        data = 42;
        ready = true;
    }

    @Actor
    public void reader(I_Result result) {
        result.r1 = ready ? data : -1;
    }
}
