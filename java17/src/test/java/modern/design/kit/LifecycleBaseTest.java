package modern.design.kit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 设计工具包：生命周期状态机、组合组件的启停顺序 */
class LifecycleBaseTest {

    static final class Part extends LifecycleBase {
        final List<String> log;
        final boolean failOnStart;

        Part(String name, List<String> log, boolean failOnStart) {
            super(name);
            this.log = log;
            this.failOnStart = failOnStart;
        }

        @Override protected void startInternal() {
            if (failOnStart) throw new IllegalStateException("boom");
            log.add("start " + getName());
        }

        @Override protected void stopInternal() { log.add("stop " + getName()); }
    }

    static final class Tree extends CompositeComponent<Part> {
        final List<String> log;

        Tree(List<String> log) {
            super("tree");
            this.log = log;
        }

        @Override protected void startSelf() { log.add("start tree"); }
        @Override protected void stopSelf() { log.add("stop tree"); }
    }

    @Test
    void startIsIdempotentAndInitsAutomatically() {
        var log = new ArrayList<String>();
        var p = new Part("p", log, false);
        var states = new ArrayList<LifecycleState>();
        p.addLifecycleListener(e -> states.add(e.to()));
        p.start();
        p.start();
        assertEquals(List.of("start p"), log);
        assertEquals(List.of(LifecycleState.INITIALIZING, LifecycleState.INITIALIZED,
                LifecycleState.STARTING, LifecycleState.STARTED), states);
        p.stop();
        p.stop();                                              // 关闭钩子和正常流程都调用：幂等
        assertEquals(List.of("start p", "stop p"), log);
    }

    @Test
    void failureMovesToFailedState() {
        var p = new Part("bad", new ArrayList<>(), true);
        assertThrows(IllegalStateException.class, p::start);
        assertEquals(LifecycleState.FAILED, p.getState());
        p.stop();
        assertEquals(LifecycleState.STOPPED, p.getState());
    }

    @Test
    void compositeStartsInOrderAndStopsInReverse() {
        var log = new ArrayList<String>();
        var tree = new Tree(log);
        tree.addChild(new Part("a", log, false));
        tree.addChild(new Part("b", log, false));
        tree.start();
        tree.addChild(new Part("c", log, false));              // 运行中添加：立即启动
        tree.stop();
        assertEquals(List.of("start tree", "start a", "start b", "start c",
                "stop c", "stop b", "stop a", "stop tree"), log);
    }
}
