package modern.ch14.core;

import modern.ch04.connector.HttpConnector;
import modern.ch05.core.Container;
import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleException;
import modern.ch06.core.LifecycleListener;
import modern.ch06.core.LifecycleSupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static modern.ch06.core.LifecycleEvent.Type.AFTER_START;
import static modern.ch06.core.LifecycleEvent.Type.AFTER_STOP;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_START;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_STOP;
import static modern.ch06.core.LifecycleEvent.Type.START;
import static modern.ch06.core.LifecycleEvent.Type.STOP;

/**
 * 服务（原书 org.apache.catalina.core.StandardService）：一个容器 + 若干连接器。
 * 所有连接器共享同一个容器，所以同一套应用可以同时在多个端口 / 协议上提供服务。
 * 启动顺序：先容器后连接器（容器准备好了才接请求）；停止顺序：先连接器后容器。
 */
public class StandardService implements Lifecycle {

    private final LifecycleSupport lifecycle = new LifecycleSupport(this);
    private final List<HttpConnector> connectors = new CopyOnWriteArrayList<>();
    private String name;
    private Container container;
    private boolean initialized;
    private boolean started;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Container getContainer() { return container; }

    /** 设置容器时，把它同时交给所有已有的连接器 */
    public void setContainer(Container container) {
        this.container = container;
        connectors.forEach(c -> c.setContainer(container));
    }

    public void addConnector(HttpConnector connector) {
        connector.setContainer(container);
        connectors.add(connector);
        try {
            if (initialized) {
                connector.initialize();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (started) {
            connector.start();
        }
    }

    public List<HttpConnector> findConnectors() {
        return List.copyOf(connectors);
    }

    /** 初始化 = 让所有连接器绑定端口（原书注释：便于在 Unix 上先以 root 绑定 1024 以下的端口） */
    public void initialize() throws LifecycleException {
        if (initialized) {
            throw new LifecycleException("This service has already been initialized");
        }
        initialized = true;
        for (HttpConnector connector : connectors) {
            try {
                connector.initialize();
            } catch (IOException e) {
                throw new LifecycleException("连接器初始化失败: " + e);
            }
        }
    }

    @Override
    public void start() throws LifecycleException {
        if (started) {
            throw new LifecycleException("This service has already been started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_START, null);
        System.out.println("Starting service " + name);
        lifecycle.fireLifecycleEvent(START, null);
        started = true;
        Lifecycle.start(container);                       // 先容器
        connectors.forEach(HttpConnector::start);         // 后连接器
        lifecycle.fireLifecycleEvent(AFTER_START, null);
    }

    @Override
    public void stop() throws LifecycleException {
        if (!started) {
            throw new LifecycleException("This service has not yet been started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_STOP, null);
        lifecycle.fireLifecycleEvent(STOP, null);
        System.out.println("Stopping service " + name);
        started = false;
        for (HttpConnector connector : connectors) {      // 先连接器：不再接新请求
            try {
                connector.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Lifecycle.stop(container);                        // 后容器
        lifecycle.fireLifecycleEvent(AFTER_STOP, null);
    }

    @Override public void addLifecycleListener(LifecycleListener l) { lifecycle.addLifecycleListener(l); }
    @Override public List<LifecycleListener> findLifecycleListeners() { return lifecycle.findLifecycleListeners(); }
    @Override public void removeLifecycleListener(LifecycleListener l) { lifecycle.removeLifecycleListener(l); }
}
