package modern.design.kit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 生命周期的模板方法实现（对应 Tomcat 的 org.apache.catalina.util.LifecycleBase）：
 *   - init/start/stop 是 final 的，负责状态检查、状态切换、发事件、异常处理
 *   - 子类只实现 initInternal/startInternal/stopInternal，写「这个组件自己要做的事」
 * 这样状态机的规则只写一次，几十个组件都不会写错。
 */
public abstract class LifecycleBase implements Lifecycle {

    private final String name;
    private volatile LifecycleState state = LifecycleState.NEW;
    /** 写少读多、遍历时可能有人增删：写时复制（第 6 章 LifecycleSupport 的现代版） */
    private final List<Consumer<LifecycleEvent>> listeners = new CopyOnWriteArrayList<>();

    protected LifecycleBase(String name) {
        this.name = name;
    }

    @Override public final String getName() { return name; }
    @Override public final LifecycleState getState() { return state; }
    @Override public final void addLifecycleListener(Consumer<LifecycleEvent> l) { listeners.add(l); }

    @Override
    public final synchronized void init() {
        if (state != LifecycleState.NEW) {
            return;                                               // 已经初始化过：幂等
        }
        transition(LifecycleState.INITIALIZING);
        try {
            initInternal();
        } catch (RuntimeException e) {
            fail(e);
        }
        transition(LifecycleState.INITIALIZED);
    }

    @Override
    public final synchronized void start() {
        if (state == LifecycleState.STARTED || state == LifecycleState.STARTING) {
            return;                                               // 重复 start：幂等
        }
        if (state == LifecycleState.NEW) {
            init();                                               // 没 init 就 start：替使用者补上
        }
        transition(LifecycleState.STARTING);
        try {
            startInternal();
        } catch (RuntimeException e) {
            fail(e);
        }
        transition(LifecycleState.STARTED);
    }

    @Override
    public final synchronized void stop() {
        if (state == LifecycleState.STOPPED || state == LifecycleState.STOPPING) {
            return;                                               // 关闭钩子和正常流程可能都调用：必须幂等（第 16 章）
        }
        if (state == LifecycleState.NEW || state == LifecycleState.INITIALIZED) {
            state = LifecycleState.STOPPED;                       // 从没启动过：什么都不用做
            return;
        }
        transition(LifecycleState.STOPPING);
        try {
            stopInternal();
        } catch (RuntimeException e) {
            fail(e);
        }
        transition(LifecycleState.STOPPED);
    }

    protected void initInternal() { }

    protected abstract void startInternal();

    protected abstract void stopInternal();

    private void transition(LifecycleState to) {
        LifecycleState from = state;
        if (!from.next().contains(to)) {
            throw new IllegalStateException(name + "：不能从 " + from + " 转到 " + to);
        }
        state = to;
        var event = new LifecycleEvent(this, from, to);
        for (var l : listeners) {
            l.accept(event);
        }
    }

    private void fail(RuntimeException e) {
        transition(LifecycleState.FAILED);
        throw new IllegalStateException(name + " 失败：" + e.getMessage(), e);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + ", " + state + "]";
    }
}
