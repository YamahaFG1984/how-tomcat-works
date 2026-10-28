package modern.ch12.core;

import modern.ch03.connector.http.HttpRequest;
import modern.ch05.core.SimpleWrapper;
import modern.ch08.loader.WebappLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第 9 章的 Context（带会话管理器）再加上第 12 章讲的三件事：
 *  ① 完整的四条映射规则；
 *  ② 重载期间暂停处理请求（paused），请求在管道入口等待；
 *  ③ backgroundProcess()：由共享后台线程调用，代替载入器和管理器各自的线程。
 */
public class StandardContext extends modern.ch09.core.SimpleContext implements BackgroundProcessable {

    private final Map<String, String> servletMappings = new ConcurrentHashMap<>();
    private final StandardContextMapper mapper = new StandardContextMapper(servletMappings);
    private volatile boolean paused;
    private boolean privileged;                 // 第 19 章：特权应用可以使用 ContainerServlet
    private boolean reloadable;
    private int managerChecksFrequency = 6;     // 每 6 次后台处理检查一次会话过期
    private int count;

    public StandardContext() {
        // Tomcat 5 起，「重载中就等待」的检查从 StandardContext.invoke 移到了管道里
        getPipeline().addValve((request, response, next) -> {
            while (paused) {
                Thread.sleep(100);
            }
            next.invokeNext(request, response);
        });
    }

    public void setReloadable(boolean reloadable) { this.reloadable = reloadable; }
    public boolean isPaused() { return paused; }
    public boolean isPrivileged() { return privileged; }
    public void setPrivileged(boolean privileged) { this.privileged = privileged; }

    @Override
    public void addServletMapping(String pattern, String name) {
        servletMappings.put(pattern, name);
        super.addServletMapping(pattern, name);
    }

    /** 覆盖第 5 章只支持精确匹配的 map()：应用四条规则，并把 servletPath / pathInfo 写回请求 */
    @Override
    public SimpleWrapper map(HttpRequest request) {
        String relativeURI = request.getRequestURI().substring(request.getContextPath().length());
        return mapper.map(relativeURI).map(result -> {
            request.setServletPath(result.servletPath());
            request.setPathInfo(result.pathInfo());
            return (SimpleWrapper) findChild(result.wrapperName());
        }).orElse(null);
    }

    @Override
    public synchronized void reload() {
        paused = true;                           // 新请求先在管道入口等着
        try {
            super.reload();
        } finally {
            paused = false;
        }
    }

    /** 原书 Listing 12.4：每次检查类是否修改；每 managerChecksFrequency 次让管理器清理过期会话 */
    @Override
    public void backgroundProcess() {
        count = (count + 1) % managerChecksFrequency;
        if (getManager() != null && count == 0) {
            getManager().processExpires();
        }
        if (reloadable && getLoader() instanceof WebappLoader loader && loader.modified()) {
            reload();
        }
    }
}
