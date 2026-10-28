package modern.ch13.core;

import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Contained;
import modern.ch05.core.Container;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;
import modern.ch12.core.StandardContext;

/**
 * Host 的基础阀（原书 Listing 13.4）：找到 Context → 设置请求的上下文路径 →
 * 把线程上下文类加载器换成该应用的类加载器 → 调用 Context。
 * （原书还会在这里更新会话的最后访问时间；Java 17 版的 StandardManager.findSession 已经做了这件事。）
 */
public class StandardHostValve implements Valve, Contained {

    private StandardHost host;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext valveContext) throws Exception {
        StandardContext context = host.map(request.getRequestURI());
        if (context == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No context for " + request.getRequestURI());
            return;
        }
        if (!context.isRunning()) {
            // 应用被停止了（例如通过第 19 章的 Manager）：503，而不是让请求进入一个已停止的应用
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, null);
            return;
        }
        request.setContextPath(context.getPath());

        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(context.getLoader().getClassLoader());
        try {
            context.invoke(request, response);
        } finally {
            thread.setContextClassLoader(original);       // 原书没有恢复；线程池复用线程时会串味
        }
    }

    @Override public Container getContainer() { return host; }
    @Override public void setContainer(Container container) { this.host = (StandardHost) container; }
}
