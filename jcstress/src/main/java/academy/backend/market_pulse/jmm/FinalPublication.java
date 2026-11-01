package academy.backend.market_pulse.jmm;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.*;
import static org.openjdk.jcstress.annotations.Expect.*;
@JCStressTest
@Outcome(id={"-1", "42"}, expect=ACCEPTABLE, desc="Нет объекта или корректное final-поле")
@Outcome(expect=FORBIDDEN, desc="Частично сконструированный объект запрещён для final-поля")
@State
public class FinalPublication {
    static class Holder { final int value; Holder() { value=42; } }
    Holder holder;
    @Actor public void writer() { holder=new Holder(); }
    @Actor public void reader(I_Result r) { Holder h=holder; r.r1=h==null ? -1 : h.value; }
}
