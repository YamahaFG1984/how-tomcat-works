package modern.common;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;

/**
 * HttpServletRequest 的「空实现」：接口里的每个方法默认都抛 UnsupportedOperationException。
 *
 * 原书为了让 Request/Response 能编译，手写了几十个返回 null 的空方法；
 * Java 8+ 可以用接口默认方法把这些样板代码集中到一处，
 * 实现类只需要覆盖自己真正支持的那几个方法。
 * （本文件由脚本根据 jakarta.servlet 6.1 接口自动生成。）
 */
public interface HttpServletRequestStub extends HttpServletRequest, ServletRequestStub {

    private static UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException("本章的简化实现不支持 " + method + "()");
    }

    @Override default String getAuthType() { throw unsupported("getAuthType"); }
    @Override default Cookie[] getCookies() { throw unsupported("getCookies"); }
    @Override default long getDateHeader(String arg0) { throw unsupported("getDateHeader"); }
    @Override default String getHeader(String arg0) { throw unsupported("getHeader"); }
    @Override default Enumeration<String> getHeaders(String arg0) { throw unsupported("getHeaders"); }
    @Override default Enumeration<String> getHeaderNames() { throw unsupported("getHeaderNames"); }
    @Override default int getIntHeader(String arg0) { throw unsupported("getIntHeader"); }
    @Override default String getMethod() { throw unsupported("getMethod"); }
    @Override default String getPathInfo() { throw unsupported("getPathInfo"); }
    @Override default String getPathTranslated() { throw unsupported("getPathTranslated"); }
    @Override default String getContextPath() { throw unsupported("getContextPath"); }
    @Override default String getQueryString() { throw unsupported("getQueryString"); }
    @Override default String getRemoteUser() { throw unsupported("getRemoteUser"); }
    @Override default boolean isUserInRole(String arg0) { throw unsupported("isUserInRole"); }
    @Override default Principal getUserPrincipal() { throw unsupported("getUserPrincipal"); }
    @Override default String getRequestedSessionId() { throw unsupported("getRequestedSessionId"); }
    @Override default String getRequestURI() { throw unsupported("getRequestURI"); }
    @Override default StringBuffer getRequestURL() { throw unsupported("getRequestURL"); }
    @Override default String getServletPath() { throw unsupported("getServletPath"); }
    @Override default HttpSession getSession(boolean arg0) { throw unsupported("getSession"); }
    @Override default HttpSession getSession() { throw unsupported("getSession"); }
    @Override default String changeSessionId() { throw unsupported("changeSessionId"); }
    @Override default boolean isRequestedSessionIdValid() { throw unsupported("isRequestedSessionIdValid"); }
    @Override default boolean isRequestedSessionIdFromCookie() { throw unsupported("isRequestedSessionIdFromCookie"); }
    @Override default boolean isRequestedSessionIdFromURL() { throw unsupported("isRequestedSessionIdFromURL"); }
    @Override default boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException { throw unsupported("authenticate"); }
    @Override default void login(String arg0, String arg1) throws ServletException { throw unsupported("login"); }
    @Override default void logout() throws ServletException { throw unsupported("logout"); }
    @Override default Collection<Part> getParts() throws IOException, ServletException { throw unsupported("getParts"); }
    @Override default Part getPart(String arg0) throws IOException, ServletException { throw unsupported("getPart"); }
    @Override default <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException { throw unsupported("upgrade"); }
}
