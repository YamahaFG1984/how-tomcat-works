package modern.ch03.connector.http;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import modern.ch03.connector.RequestStream;
import modern.common.HttpServletRequestStub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 实现 HttpServletRequest 的请求对象。HttpProcessor 负责填充请求行和请求头；
 * 参数则「懒解析」：Servlet 第一次调用 getParameter 系列方法时才去解析查询串和请求体。
 */
public class HttpRequest implements HttpServletRequestStub {

    private final InputStream input;
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private Map<String, String[]> parameters;          // null 表示还没解析
    private RequestStream stream;

    private String method;
    private String requestURI;
    private String queryString;
    private String protocol;
    private String requestedSessionId;
    private boolean requestedSessionIdFromURL;
    private boolean requestedSessionIdFromCookie;
    private long contentLength = -1;
    private String contentType;
    private String characterEncoding;
    private String remoteAddr = "127.0.0.1";
    private int serverPort = 8080;
    private String contextPath = "";                 // 第 13 章：由 Host 映射时设置
    private String servletPath;                      // 第 12 章：由映射器设置
    private String pathInfo;
    private Principal userPrincipal;                 // 第 10 章：认证成功后由认证器设置
    private Predicate<String> roleCheck = role -> false;

    public HttpRequest(InputStream input) {
        this.input = input;
    }

    /* ======== 由 HttpProcessor 调用的 setter（容器内部方法） ======== */

    public void setMethod(String method) { this.method = method; }
    public void setRequestURI(String uri) { this.requestURI = uri; }
    public void setQueryString(String qs) { this.queryString = qs; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public void setRequestedSessionId(String id) { this.requestedSessionId = id; }
    public void setRequestedSessionURL(boolean flag) { this.requestedSessionIdFromURL = flag; }
    public void setRequestedSessionCookie(boolean flag) { this.requestedSessionIdFromCookie = flag; }
    public void setContentLength(long length) { this.contentLength = length; }
    public void setContentType(String type) { this.contentType = type; }
    public void setRemoteAddr(String addr) { this.remoteAddr = addr; }
    public void setServletPath(String servletPath) { this.servletPath = servletPath; }
    public void setContextPath(String contextPath) { this.contextPath = contextPath; }
    public void setPathInfo(String pathInfo) { this.pathInfo = pathInfo; }
    public void setServerPort(int port) { this.serverPort = port; }

    /** 第 10 章：认证器认证成功后调用，roleCheck 用来回答 isUserInRole() */
    public void setUserPrincipal(Principal principal, Predicate<String> roleCheck) {
        this.userPrincipal = principal;
        this.roleCheck = roleCheck;
    }

    public void addHeader(String name, String value) {
        headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(value);
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    /** 请求处理完后，把 Servlet 没读完的请求体丢掉（第 4 章 keep-alive 需要） */
    public void finishRequest() throws IOException {
        ((RequestStream) getInputStream()).drain();
    }

    /* ======== 参数：懒解析 ======== */

    private Map<String, String[]> parameters() {
        if (parameters != null) {
            return parameters;
        }
        Map<String, String[]> results = RequestUtil.newParameterMap();
        Charset charset = characterEncoding != null ? Charset.forName(characterEncoding) : StandardCharsets.UTF_8;
        RequestUtil.parseParameters(results, queryString, charset);

        String type = contentType == null ? "" : contentType.split(";", 2)[0].trim();
        if ("POST".equals(method) && contentLength > 0 && "application/x-www-form-urlencoded".equals(type)) {
            try {
                // readNBytes（Java 11）会一直读到够数或流结束为止，代替原书的 while 循环
                byte[] body = getInputStream().readNBytes((int) contentLength);
                if (body.length < contentLength) {
                    throw new IllegalStateException("Content length mismatch");
                }
                RequestUtil.parseParameters(results, new String(body, StandardCharsets.ISO_8859_1), charset);
            } catch (IOException e) {
                throw new UncheckedIOException("Content read fail", e);
            }
        }
        // 原书用一个可「上锁」的 ParameterMap 防止 Servlet 修改参数；
        // 现代 Java 直接用 Map.copyOf 得到一个不可变 Map，任何 put/remove 都抛 UnsupportedOperationException。
        // （Map.copyOf 不保证顺序，所以先包一层 unmodifiableMap 保留参数出现的顺序）
        parameters = Collections.unmodifiableMap(results);
        return parameters;
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters().get(name);
        return values == null ? null : values[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = parameters().get(name);
        return values == null ? null : values.clone();
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters();
    }

    /* ======== 请求头与 Cookie ======== */

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null ? null : values.get(0);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(headers.getOrDefault(name.toLowerCase(Locale.ROOT), List.of()));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        return value == null ? -1 : Integer.parseInt(value);
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        return value == null ? -1
                : ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
    }

    @Override
    public Cookie[] getCookies() {
        return cookies.isEmpty() ? null : cookies.toArray(Cookie[]::new);
    }

    /* ======== 请求行相关 ======== */

    @Override public String getMethod() { return method; }
    @Override public String getRequestURI() { return requestURI; }
    @Override public String getQueryString() { return queryString; }
    @Override public String getProtocol() { return protocol; }
    @Override public String getScheme() { return "http"; }
    /** 第 13 章：虚拟主机靠 Host 请求头区分，去掉端口部分；没有 Host 头时返回 null */
    @Override
    public String getServerName() {
        String host = getHeader("host");
        if (host == null) {
            return null;
        }
        int colon = host.lastIndexOf(':');
        return colon > 0 && !host.endsWith("]") ? host.substring(0, colon) : host;
    }

    @Override public int getServerPort() { return serverPort; }
    @Override public String getRemoteAddr() { return remoteAddr; }
    @Override public String getRemoteHost() { return remoteAddr; }
    @Override public boolean isSecure() { return false; }
    @Override public String getContextPath() { return contextPath; }
    @Override public String getServletPath() { return servletPath != null ? servletPath : requestURI; }
    @Override public String getPathInfo() { return pathInfo; }
    @Override public String getRequestedSessionId() { return requestedSessionId; }
    @Override public boolean isRequestedSessionIdFromURL() { return requestedSessionIdFromURL; }
    @Override public boolean isRequestedSessionIdFromCookie() { return requestedSessionIdFromCookie; }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer("http://" + (getServerName() != null ? getServerName() : "localhost") + ":" + serverPort + requestURI);
    }

    /* ======== 请求体 ======== */

    @Override public int getContentLength() { return (int) contentLength; }
    @Override public long getContentLengthLong() { return contentLength; }
    @Override public String getContentType() { return contentType; }
    @Override public String getCharacterEncoding() { return characterEncoding; }
    @Override public void setCharacterEncoding(String env) { this.characterEncoding = env; }

    @Override
    public ServletInputStream getInputStream() {
        if (stream == null) {
            stream = new RequestStream(input, contentLength);
        }
        return stream;
    }

    @Override
    public BufferedReader getReader() {
        Charset cs = characterEncoding != null ? Charset.forName(characterEncoding) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(getInputStream(), cs));
    }

    /* ======== 属性 ======== */

    @Override public Object getAttribute(String name) { return attributes.get(name); }
    @Override public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }
    @Override public void setAttribute(String name, Object o) { attributes.put(name, o); }
    @Override public void removeAttribute(String name) { attributes.remove(name); }
    @Override public Locale getLocale() { return Locale.getDefault(); }

    /* ======== 安全（第 10 章） ======== */

    @Override public Principal getUserPrincipal() { return userPrincipal; }
    @Override public String getRemoteUser() { return userPrincipal == null ? null : userPrincipal.getName(); }
    @Override public boolean isUserInRole(String role) { return userPrincipal != null && roleCheck.test(role); }
    @Override public String getAuthType() { return userPrincipal == null ? null : "BASIC"; }
}
