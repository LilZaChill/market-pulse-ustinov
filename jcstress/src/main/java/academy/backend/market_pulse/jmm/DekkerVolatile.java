package academy.backend.market_pulse.jmm;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;
import static org.openjdk.jcstress.annotations.Expect.*;
@JCStressTest
@Outcome(id={"1, 1", "0, 1", "1, 0"}, expect=ACCEPTABLE, desc="Согласованный исход")
@Outcome(id="0, 0", expect=FORBIDDEN, desc="Обе записи не наблюдались")
@Outcome(expect=FORBIDDEN, desc="Прочие исходы")
@State
public class DekkerVolatile {
    volatile int x, y;
    @Actor public void one(II_Result r) { x=1; r.r1=y; }
    @Actor public void two(II_Result r) { y=1; r.r2=x; }
}
