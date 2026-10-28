package modern.ch13.core;

import modern.ch05.core.Container;
import modern.ch05.core.ContainerBase;
import modern.ch05.core.Valve;
import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleException;
import modern.ch06.core.LifecycleListener;
import modern.ch06.core.LifecycleSupport;

import java.util.List;

import static modern.ch06.core.LifecycleEvent.Type.AFTER_START;
import static modern.ch06.core.LifecycleEvent.Type.AFTER_STOP;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_START;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_STOP;
import static modern.ch06.core.LifecycleEvent.Type.START;
import static modern.ch06.core.LifecycleEvent.Type.STOP;

/** Host 和 Engine 共用的「带生命周期的容器」：启动时启动所有子容器和阀，停止时倒过来 */
public abstract class LifecycleContainerBase extends ContainerBase implements Lifecycle {

    private final LifecycleSupport lifecycle = new LifecycleSupport(this);
    private boolean started;

    public boolean isStarted() {
        return started;
    }

    @Override public void addLifecycleListener(LifecycleListener l) { lifecycle.addLifecycleListener(l); }
    @Override public List<LifecycleListener> findLifecycleListeners() { return lifecycle.findLifecycleListeners(); }
    @Override public void removeLifecycleListener(LifecycleListener l) { lifecycle.removeLifecycleListener(l); }

    @Override
    public synchronized void start() throws LifecycleException {
        if (started) {
            throw new LifecycleException(getClass().getSimpleName() + " already started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_START, null);
        started = true;
        for (Container child : findChildren()) {
            Lifecycle.start(child);
        }
        for (Valve valve : pipeline.getValves()) {
            Lifecycle.start(valve);
        }
        lifecycle.fireLifecycleEvent(START, null);
        lifecycle.fireLifecycleEvent(AFTER_START, null);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started) {
            throw new LifecycleException(getClass().getSimpleName() + " not started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_STOP, null);
        lifecycle.fireLifecycleEvent(STOP, null);
        started = false;
        for (Valve valve : pipeline.getValves()) {
            Lifecycle.stop(valve);
        }
        for (Container child : findChildren()) {
            Lifecycle.stop(child);
        }
        lifecycle.fireLifecycleEvent(AFTER_STOP, null);
    }
}
