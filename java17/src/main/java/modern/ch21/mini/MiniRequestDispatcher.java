package modern.ch21.mini;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletResponseWrapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 迷你版 ApplicationDispatcher。forward 和 include 都不产生新的 HTTP 请求，
 * 而是把<strong>同一对</strong> request/response 包装一下，直接调用目标 Servlet 的 service()。
 *
 *   forward：清空缓冲区 → 换掉路径信息（目标路径）→ 调用 → 关闭响应，调用方之后再写什么都没用
 *   include：路径信息保持原样（目标路径放进属性）→ 响应包装成「不许改状态和头」→ 调用 → 回到调用方继续写
 */
public final class MiniRequestDispatcher implements RequestDispatcher {

    private final MiniApplicationContext context;
    private final HttpServlet servlet;
    private final String servletName;
    private final String requestURI;      // 带上下文路径的目标 URI；按名字分派时为 null
    private final String servletPath;
    private final String pathInfo;
    private final String queryString;

    MiniRequestDispatcher(MiniApplicationContext context, HttpServlet servlet, String servletName,
                          String requestURI, String servletPath, String pathInfo, String queryString) {
        this.context = context;
        this.servlet = servlet;
        this.servletName = servletName;
        this.requestURI = requestURI;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
    }

    @Override
    public void forward(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        var request = (HttpServletRequest) req;
        var response = (HttpServletResponse) resp;
        if (response.isCommitted()) {
            throw new IllegalStateException("响应已经提交，不能再 forward（Tomcat 抛同样的异常）");
        }
        response.resetBuffer();                           // 调用方之前写进缓冲区的内容全部作废

        DispatchedRequest wrapped;
        if (requestURI == null) {
            wrapped = new DispatchedRequest(request, DispatcherType.FORWARD);  // 按名字：路径不变
        } else {
            wrapped = new DispatchedRequest(request, DispatcherType.FORWARD,
                    requestURI, servletPath, pathInfo, queryString);
            // 嵌套 forward 时保留最初的值：属性描述的是「客户端原本请求的是什么」
            if (request.getAttribute(FORWARD_REQUEST_URI) == null) {
                wrapped.special(FORWARD_REQUEST_URI, request.getRequestURI());
                wrapped.special(FORWARD_CONTEXT_PATH, request.getContextPath());
                wrapped.special(FORWARD_SERVLET_PATH, request.getServletPath());
                wrapped.special(FORWARD_PATH_INFO, request.getPathInfo());
                wrapped.special(FORWARD_QUERY_STRING, request.getQueryString());
            }
        }
        servlet.service(wrapped, response);
        // forward 返回时响应就结束了：调用方之后再写的内容都会被丢弃
        ServletResponse inner = response;
        while (inner instanceof ServletResponseWrapper w) inner = w.getResponse();
        if (inner instanceof MockResponse mock) mock.finish(); else response.flushBuffer();
    }

    @Override
    public void include(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        var request = (HttpServletRequest) req;
        var wrapped = new DispatchedRequest(request, DispatcherType.INCLUDE);   // 路径方法仍返回原值
        if (requestURI != null) {
            wrapped.special(INCLUDE_REQUEST_URI, requestURI);
            wrapped.special(INCLUDE_CONTEXT_PATH, context.getContextPath());
            wrapped.special(INCLUDE_SERVLET_PATH, servletPath);
            wrapped.special(INCLUDE_PATH_INFO, pathInfo);
            wrapped.special(INCLUDE_QUERY_STRING, queryString);
            wrapped.mergeQuery(queryString);
        }
        servlet.service(wrapped, new IncludedResponse((HttpServletResponse) resp));
    }

    @Override
    public String toString() {
        return "Dispatcher[" + servletName + (requestURI == null ? "" : " " + requestURI) + "]";
    }

    /** 方便演示：取出 forward/include 属性 */
    public static Map<String, Object> dispatchAttributes(HttpServletRequest request) {
        var out = new LinkedHashMap<String, Object>();
        for (String name : new String[] {FORWARD_REQUEST_URI, FORWARD_SERVLET_PATH, FORWARD_QUERY_STRING,
                INCLUDE_REQUEST_URI, INCLUDE_SERVLET_PATH, INCLUDE_QUERY_STRING}) {
            Object v = request.getAttribute(name);
            if (v != null) out.put(name.substring("jakarta.servlet.".length()), v);
        }
        return out;
    }
}
