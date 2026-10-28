package modern.ch06.core;

/** 只有一个方法，所以可以直接用 lambda 注册监听器 */
@FunctionalInterface
public interface LifecycleListener {

    void lifecycleEvent(LifecycleEvent event);
}
