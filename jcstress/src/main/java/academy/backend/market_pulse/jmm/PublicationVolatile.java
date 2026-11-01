package academy.backend.market_pulse.jmm;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;
import static org.openjdk.jcstress.annotations.Expect.*;
@JCStressTest
@Outcome(id={"-1", "42"}, expect=ACCEPTABLE, desc="Флаг ещё не виден или данные опубликованы")
@Outcome(id="0", expect=FORBIDDEN, desc="Флаг виден, данные не видны")
@Outcome(expect=FORBIDDEN, desc="Прочие исходы")
@State
public class PublicationVolatile {
    int data;
    volatile boolean ready;
    @Actor public void writer() { data=42; ready=true; }
    @Actor public void reader(I_Result r) { r.r1=ready ? data : -1; }
}
