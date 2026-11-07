package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/** Две задачи публикуются в стек; arbiter проверяет сохранность обеих. */
@JCStressTest
@Outcome(id = "3", expect = Expect.ACCEPTABLE, desc = "Обе задачи доступны")
@Outcome(id = {"1", "2"}, expect = Expect.ACCEPTABLE_INTERESTING, desc = "Один push потерян")
@Outcome(expect = Expect.FORBIDDEN, desc = "Неожиданный исход")
@State
public class TreiberPushPlain {
    private record Node(int task, Node next) { }

    private Node head; // Намеренно ошибочно: volatile тоже не сделает всю операцию атомарной.

    @Actor
    public void firstActor() {
        push(1);
    }

    @Actor
    public void secondActor() {
        push(2);
    }

    private void push(int task) {
        head = new Node(task, head);
    }

    @Arbiter
    public void readResult(I_Result result) {
        int mask = 0;
        for (Node node = head; node != null; node = node.next()) {
            mask |= node.task();
        }
        result.r1 = mask;
    }
}
