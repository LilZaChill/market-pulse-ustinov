package academy.backend.market_pulse.jmm;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;
import static org.openjdk.jcstress.annotations.Expect.*;
@JCStressTest
@Outcome(id="2", expect=ACCEPTABLE, desc="Оба изменения сохранены")
@Outcome(id="1", expect=ACCEPTABLE_INTERESTING, desc="Потерянное изменение")
@Outcome(expect=FORBIDDEN, desc="Прочие исходы")
@State
public class IncrementPlain {
    int counter;
    @Actor public void one() { counter++; }
    @Actor public void two() { counter++; }
    @Arbiter public void result(I_Result r) { r.r1=counter; }
}
