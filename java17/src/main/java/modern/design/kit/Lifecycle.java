package modern.design.kit;

import java.util.function.Consumer;

/**
 * 统一的组件模型：所有有资源要管理的组件都实现它（第 6 章）。
 * 父组件 start 时级联启动子组件，所以使用者只需要启动最顶层的那一个。
 */
public interface Lifecycle extends AutoCloseable {

    void init();

    void start();

    void stop();

    LifecycleState getState();

    String getName();

    /** 观察者：不修改组件就能在它的状态变化时插入行为（第 6、15、18 章都靠它扩展） */
    void addLifecycleListener(Consumer<LifecycleEvent> listener);

    /** 让组件可以用在 try-with-resources 里 */
    @Override
    default void close() {
        stop();
    }
}
