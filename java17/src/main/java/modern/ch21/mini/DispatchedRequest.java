package modern.ch21.mini;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 迷你版 ApplicationHttpRequest：分派时包在原请求外面的那一层。
 *  - 路径相关的方法返回目标路径（forward）或原路径（include）
 *  - 分派路径上的查询参数和原参数合并，<strong>新参数优先</strong>
 *  - forward/include 的「特殊属性」只存在这一层，返回调用方后自动消失
 * 普通属性（setAttribute）直接落到原请求上，所以调用方和目标 Servlet 能互相传数据。
 */
final class DispatchedRequest extends HttpServletRequestWrapper {

    private final DispatcherType type;
    private final String requestURI;
    private final String servletPath;
    private final String pathInfo;
    private final String queryString;
    private final Map<String, Object> special = new HashMap<>();
    private Map<String, String[]> params;

    /** include 或按名字分派：路径信息不变 */
    DispatchedRequest(HttpServletRequest request, DispatcherType type) {
        this(request, type, request.getRequestURI(), request.getServletPath(),
                request.getPathInfo(), request.getQueryString());
    }

    DispatchedRequest(HttpServletRequest request, DispatcherType type, String requestURI,
                      String servletPath, String pathInfo, String queryString) {
        super(request);
        this.type = type;
        this.requestURI = requestURI;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.params = request.getParameterMap();
        if (type == DispatcherType.FORWARD) mergeQuery(queryString);
    }

    void special(String name, Object value) {
        if (value != null) special.put(name, value);
    }

    void mergeQuery(String query) {
        if (query == null || query.isEmpty()) return;
        var merged = new LinkedHashMap<String, String[]>(MockRequest.QueryString.parse(query));
        getRequest().getParameterMap().forEach(merged::putIfAbsent);
        params = Collections.unmodifiableMap(merged);
    }

    @Override public DispatcherType getDispatcherType() { return type; }
    @Override public String getRequestURI() { return requestURI; }
    @Override public String getServletPath() { return servletPath; }
    @Override public String getPathInfo() { return pathInfo; }
    @Override public String getQueryString() { return queryString; }

    @Override public Map<String, String[]> getParameterMap() { return params; }
    @Override public String[] getParameterValues(String name) { return params.get(name); }
    @Override public Enumeration<String> getParameterNames() { return Collections.enumeration(params.keySet()); }
    @Override public String getParameter(String name) {
        String[] v = params.get(name);
        return v == null ? null : v[0];
    }

    @Override public Object getAttribute(String name) {
        Object v = special.get(name);
        return v != null ? v : super.getAttribute(name);
    }
}
