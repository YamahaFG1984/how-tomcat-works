package modern.ch06.core;

import java.util.List;

/**
 * 生命周期接口：组件可以被父组件启动、停止，并向监听器发布事件。
 * 原书用六个 String 常量表示事件类型，这里改为枚举 LifecycleEvent.Type。
 */
public interface Lifecycle {

    void addLifecycleListener(LifecycleListener listener);

    List<LifecycleListener> findLifecycleListeners();

    void removeLifecycleListener(LifecycleListener listener);

    void start() throws LifecycleException;

    void stop() throws LifecycleException;

    /** 原书里到处是 if (x instanceof Lifecycle) ((Lifecycle) x).start()；收拢成一个工具方法 */
    static void start(Object component) throws LifecycleException {
        if (component instanceof Lifecycle lifecycle) {
            lifecycle.start();
        }
    }

    static void stop(Object component) throws LifecycleException {
        if (component instanceof Lifecycle lifecycle) {
            lifecycle.stop();
        }
    }
}
