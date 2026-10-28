package modern.design.kit;

import java.util.EnumSet;
import java.util.Set;

/**
 * 组件的生命周期状态。原书第 6 章只有一个 started 布尔值；Tomcat 7 起换成了 LifecycleState 枚举，
 * 每个状态明确列出「可以转到哪里」，非法调用（比如没 start 就 stop）会得到清晰的错误，而不是悄悄出错。
 */
public enum LifecycleState {
    NEW, INITIALIZING, INITIALIZED, STARTING, STARTED, STOPPING, STOPPED, FAILED;

    /** 从当前状态出发允许的下一个状态 */
    public Set<LifecycleState> next() {
        return switch (this) {
            case NEW -> EnumSet.of(INITIALIZING, STOPPED);              // 没启动过也可以直接「停止」（空操作）
            case INITIALIZING -> EnumSet.of(INITIALIZED, FAILED);
            case INITIALIZED, STOPPED -> EnumSet.of(STARTING);
            case STARTING -> EnumSet.of(STARTED, FAILED);
            case STARTED -> EnumSet.of(STOPPING);
            case STOPPING -> EnumSet.of(STOPPED, FAILED);
            case FAILED -> EnumSet.of(STOPPING, STOPPED);
        };
    }

    public boolean isAvailable() {
        return this == STARTED;
    }
}
