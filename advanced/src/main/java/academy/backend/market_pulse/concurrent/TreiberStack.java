package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** LIFO: неизменяемые узлы публикуются через CAS. Узлы не переиспользуются (нет ABA из-за reuse). */
public final class TreiberStack<T> {
    private record Node<T>(T value, Node<T> next) { }

    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    public void push(T value) {
        Objects.requireNonNull(value);
        Node<T> observed;
        Node<T> update;
        do {
            observed = head.get();
            update = new Node<>(value, observed);
        } while (!head.compareAndSet(observed, update));
    }

    public T pop() {
        Node<T> observed;
        do {
            observed = head.get();
            if (observed == null) {
                return null;
            }
        } while (!head.compareAndSet(observed, observed.next()));
        return observed.value();
    }

    public void clear() {
        head.set(null);
    }
}
