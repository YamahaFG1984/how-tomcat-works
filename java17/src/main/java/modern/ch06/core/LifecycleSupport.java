package modern.ch06.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 管理监听器、发布事件的工具类。
 *
 * 原书的实现：每次增删监听器都「复制出一个新数组」再替换旧数组，发布事件时先 clone 一份再遍历——
 * 这正是写时复制（copy-on-write）。JDK 5 起它有现成的实现：CopyOnWriteArrayList。
 * 遍历时拿到的是一个快照，即使监听器在回调里注销自己也不会抛 ConcurrentModificationException。
 */
public final class LifecycleSupport {

    private final Lifecycle lifecycle;
    private final List<LifecycleListener> listeners = new CopyOnWriteArrayList<>();

    public LifecycleSupport(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void addLifecycleListener(LifecycleListener listener) {
        listeners.add(listener);
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove(listener);
    }

    public List<LifecycleListener> findLifecycleListeners() {
        return List.copyOf(listeners);
    }

    public void fireLifecycleEvent(LifecycleEvent.Type type, Object data) {
        var event = new LifecycleEvent(lifecycle, type, data);
        for (LifecycleListener listener : listeners) {
            listener.lifecycleEvent(event);
        }
    }
}
