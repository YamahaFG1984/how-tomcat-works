package modern.design.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 组合模式 + 生命周期：一棵组件树（对应 Tomcat 的 ContainerBase：Engine ⊃ Host ⊃ Context ⊃ Wrapper）。
 *   - 启动：先自己（startSelf），再按添加顺序启动子组件
 *   - 停止：先按相反顺序停止子组件，再停自己
 *   - 运行中添加的子组件会被立即启动（Tomcat 的 addChild 也是这样，热部署靠它）
 */
public abstract class CompositeComponent<C extends Lifecycle> extends LifecycleBase {

    private final List<C> children = new CopyOnWriteArrayList<>();

    protected CompositeComponent(String name) {
        super(name);
    }

    public void addChild(C child) {
        children.add(child);
        if (getState().isAvailable()) {
            child.start();
        }
    }

    public boolean removeChild(C child) {
        boolean removed = children.remove(child);
        if (removed) child.stop();
        return removed;
    }

    public List<C> children() {
        return List.copyOf(children);
    }

    public Optional<C> findChild(String name) {
        return children.stream().filter(c -> c.getName().equals(name)).findFirst();
    }

    @Override
    protected final void startInternal() {
        startSelf();
        for (C c : children) c.start();
    }

    @Override
    protected final void stopInternal() {
        List<C> reversed = new ArrayList<>(children);
        java.util.Collections.reverse(reversed);
        for (C c : reversed) {
            try {
                c.stop();
            } catch (RuntimeException e) {
                // 一个子组件停不下来，不能让其余的也停不下来：记录后继续
                System.err.println("停止 " + c.getName() + " 失败：" + e.getMessage());
            }
        }
        stopSelf();
    }

    /** 自己的启动逻辑：在子组件之前执行（例如先准备好子组件依赖的资源） */
    protected void startSelf() { }

    /** 自己的停止逻辑：在所有子组件停止之后执行 */
    protected void stopSelf() { }
}
