package modern.ch21.mini;

import jakarta.servlet.DispatcherType;
import modern.common.HttpServletRequestStub;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存里的「客户端请求」：相当于连接器解析完报文后交给容器的那个 Request。
 * 只实现本章用到的方法，其余方法由 HttpServletRequestStub 抛 UnsupportedOperationException。
 */
public final class MockRequest implements HttpServletRequestStub {

    private final String method;
    private final String requestURI;
    private final String contextPath;
    private final String queryString;
    private final Map<String, String[]> params;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private String servletPath = "";
    private String pathInfo;

    public MockRequest(String method, String contextPath, String uriWithQuery) {
        int q = uriWithQuery.indexOf('?');
        this.method = method;
        this.contextPath = contextPath;
        this.requestURI = q < 0 ? uriWithQuery : uriWithQuery.substring(0, q);
        this.queryString = q < 0 ? null : uriWithQuery.substring(q + 1);
        this.params = Collections.unmodifiableMap(QueryString.parse(queryString));
    }

    /** 映射完成后由「容器」填入，相当于 Tomcat 里 Mapper 写回 Request 的 MappingData */
    void setMapping(String servletPath, String pathInfo) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
    }

    @Override public String getMethod() { return method; }
    @Override public String getRequestURI() { return requestURI; }
    @Override public String getContextPath() { return contextPath; }
    @Override public String getServletPath() { return servletPath; }
    @Override public String getPathInfo() { return pathInfo; }
    @Override public String getQueryString() { return queryString; }
    @Override public DispatcherType getDispatcherType() { return DispatcherType.REQUEST; }
    @Override public String getProtocol() { return "HTTP/1.1"; }

    @Override public String getParameter(String name) {
        String[] v = params.get(name);
        return v == null ? null : v[0];
    }
    @Override public String[] getParameterValues(String name) { return params.get(name); }
    @Override public Map<String, String[]> getParameterMap() { return params; }
    @Override public Enumeration<String> getParameterNames() { return Collections.enumeration(params.keySet()); }

    @Override public Object getAttribute(String name) { return attributes.get(name); }
    @Override public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }
    @Override public void setAttribute(String name, Object value) {
        if (value == null) attributes.remove(name); else attributes.put(name, value);
    }
    @Override public void removeAttribute(String name) { attributes.remove(name); }

    /** 极简的查询串解析（不做 URL 解码，够演示用） */
    static final class QueryString {
        static Map<String, String[]> parse(String query) {
            Map<String, List<String>> tmp = new LinkedHashMap<>();
            if (query != null && !query.isEmpty()) {
                for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    String k = eq < 0 ? pair : pair.substring(0, eq);
                    String v = eq < 0 ? "" : pair.substring(eq + 1);
                    tmp.computeIfAbsent(k, x -> new java.util.ArrayList<>()).add(v);
                }
            }
            Map<String, String[]> out = new LinkedHashMap<>();
            tmp.forEach((k, v) -> out.put(k, v.toArray(String[]::new)));
            return out;
        }
    }
}
