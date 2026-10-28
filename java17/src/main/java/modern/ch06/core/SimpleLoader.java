package modern.ch06.core;

import modern.ch05.core.Loader;

import java.util.List;

/** 第 5 章的载入器加上生命周期：start/stop 只打印一行字，但它从此可以被容器统一启停 */
public class SimpleLoader implements Loader, Lifecycle {

    private final modern.ch05.core.SimpleLoader delegate = new modern.ch05.core.SimpleLoader();

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    @Override public void addLifecycleListener(LifecycleListener listener) { }
    @Override public List<LifecycleListener> findLifecycleListeners() { return List.of(); }
    @Override public void removeLifecycleListener(LifecycleListener listener) { }

    @Override
    public void start() {
        System.out.println("Starting SimpleLoader");
    }

    @Override
    public void stop() {
        System.out.println("Stopping SimpleLoader");
    }
}
