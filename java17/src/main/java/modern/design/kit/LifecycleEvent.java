package modern.design.kit;

/** 状态变化事件：哪个组件、从什么状态到什么状态 */
public record LifecycleEvent(Lifecycle source, LifecycleState from, LifecycleState to) { }
