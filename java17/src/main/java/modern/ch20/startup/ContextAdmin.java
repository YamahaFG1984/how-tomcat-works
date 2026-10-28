package modern.ch20.startup;

import modern.ch12.core.StandardContext;

/**
 * 适配器：不修改 StandardContext 本身，而是用一个小类把它包装成 MXBean。
 * 这和模型 MBean「不改被管理的类」的初衷一致，但比手写 ModelMBeanInfo 简单得多。
 */
public record ContextAdmin(StandardContext context) implements ContextMXBean {

    @Override public String getPath() { return context.getPath().isEmpty() ? "/" : context.getPath(); }
    @Override public String getDocBase() { return context.getDocBase().toString(); }
    @Override public boolean isRunning() { return context.isRunning(); }

    @Override
    public int getActiveSessions() {
        return context.getManager() == null ? 0 : context.getManager().findSessions().size();
    }

    @Override public void start() throws Exception { context.start(); }
    @Override public void stop() throws Exception { context.stop(); }
    @Override public void reload() { context.reload(); }
}
