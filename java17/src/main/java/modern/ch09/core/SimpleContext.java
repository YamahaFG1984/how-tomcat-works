package modern.ch09.core;

import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleException;
import modern.ch09.session.StandardManager;

/** 第 8 章的 Context 加上会话管理器：启动时启动管理器（恢复会话），停止时停止管理器（保存会话） */
public class SimpleContext extends modern.ch08.core.SimpleContext {

    private String path = "";
    private StandardManager manager;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public StandardManager getManager() { return manager; }
    public void setManager(StandardManager manager) { this.manager = manager; }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start();
        Lifecycle.start(manager);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        Lifecycle.stop(manager);
        super.stop();
    }
}
