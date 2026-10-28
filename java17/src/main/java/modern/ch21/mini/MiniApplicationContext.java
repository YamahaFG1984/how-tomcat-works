package modern.ch21.mini;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import modern.ch12.core.MappingResult;
import modern.ch12.core.StandardContextMapper;
import modern.common.SimpleServletConfig;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 迷你版 ApplicationContext：Tomcat 里每个 Web 应用（StandardContext）对应一个 ServletContext 实现。
 * 这里只保留本章关心的三件事：
 *   ① 应用级属性（所有 Servlet、所有请求共享）
 *   ② 初始化参数（web.xml 里的 &lt;context-param&gt;）
 *   ③ getRequestDispatcher：按路径（复用第 12 章的映射规则）或按名字找到目标 Servlet
 */
public final class MiniApplicationContext {

    private final String contextPath;
    private final Map<String, HttpServlet> servlets = new LinkedHashMap<>();
    private final Map<String, String> mappings = new LinkedHashMap<>();       // 模式 → Servlet 名字
    private final StandardContextMapper mapper = new StandardContextMapper(mappings::get);
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<String, String> initParams = new ConcurrentHashMap<>();

    public MiniApplicationContext(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContextPath() { return contextPath; }

    public void addServlet(String name, HttpServlet servlet, String... patterns) throws ServletException {
        servlet.init(new SimpleServletConfig(name));
        servlets.put(name, servlet);
        for (String p : patterns) mappings.put(p, name);
    }

    public Object getAttribute(String name) { return attributes.get(name); }
    public void setAttribute(String name, Object value) { attributes.put(name, value); }
    public String getInitParameter(String name) { return initParams.get(name); }
    public void setInitParameter(String name, String value) { initParams.put(name, value); }

    /** path 必须以 "/" 开头，相对于上下文根，可以带查询串 */
    public RequestDispatcher getRequestDispatcher(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;                                  // 规范：相对路径由 ServletRequest 版本处理
        }
        int q = path.indexOf('?');
        String uri = q < 0 ? path : path.substring(0, q);
        String query = q < 0 ? null : path.substring(q + 1);
        return mapper.map(uri)
                .map(m -> new MiniRequestDispatcher(this, servlets.get(m.wrapperName()), m.wrapperName(),
                        contextPath + uri, m.servletPath(), m.pathInfo(), query))
                .orElse(null);
    }

    /** 按 Servlet 名字分派：不改变任何路径信息，也不设置 forward/include 属性 */
    public RequestDispatcher getNamedDispatcher(String name) {
        HttpServlet servlet = servlets.get(name);
        return servlet == null ? null : new MiniRequestDispatcher(this, servlet, name, null, null, null, null);
    }

    /** 模拟一次来自客户端的请求：映射 → 调用 Servlet → 「关闭」响应 */
    public MockResponse handle(String method, String uriWithQuery) throws ServletException, IOException {
        var request = new MockRequest(method, contextPath, contextPath + uriWithQuery);
        var response = new MockResponse();
        String relative = request.getRequestURI().substring(contextPath.length());
        Optional<MappingResult> m = mapper.map(relative);
        if (m.isEmpty() || relative.startsWith("/WEB-INF/")) {
            // StandardContextValve 拒绝客户端直接访问 WEB-INF，但服务端分派可以
            response.setStatus(404);
            return response;
        }
        request.setMapping(m.get().servletPath(), m.get().pathInfo());
        servlets.get(m.get().wrapperName()).service(request, response);
        response.flushBuffer();
        return response;
    }
}
