package modern.design.kit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 通用的「管道 + 阀」（第 5 章）：可插拔的拦截器链，最后是固定的基础阀。
 *   - 每个阀拿到 next，可以在调用前后各做一些事，也可以不调用 next（短路）
 *   - 阀列表写时复制：运行中增删阀不影响正在执行的请求
 * 和 Tomcat 4 的 ValveContext 不同，这里没有可变的「当前位置」，每次调用都从一个不可变快照出发。
 */
public final class Pipeline<T> {

    @FunctionalInterface
    public interface Valve<T> {
        void invoke(T context, Next<T> next) throws Exception;
    }

    @FunctionalInterface
    public interface Next<T> {
        void invoke(T context) throws Exception;
    }

    private final List<Valve<T>> valves = new CopyOnWriteArrayList<>();
    private volatile Next<T> basic;

    public Pipeline(Next<T> basic) {
        this.basic = basic;
    }

    public Pipeline<T> addValve(Valve<T> valve) {
        valves.add(valve);
        return this;
    }

    public void setBasic(Next<T> basic) {
        this.basic = basic;
    }

    public void invoke(T context) throws Exception {
        Object[] snapshot = valves.toArray();                 // 本次调用用的阀列表，之后的修改不影响它
        invoke(snapshot, 0, context);
    }

    @SuppressWarnings("unchecked")
    private void invoke(Object[] snapshot, int index, T context) throws Exception {
        if (index == snapshot.length) {
            basic.invoke(context);
        } else {
            ((Valve<T>) snapshot[index]).invoke(context, c -> invoke(snapshot, index + 1, c));
        }
    }
}
