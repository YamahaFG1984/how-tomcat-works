package modern.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * HttpServletResponse 的「空实现」：接口里的每个方法默认都抛 UnsupportedOperationException。
 *
 * 原书为了让 Request/Response 能编译，手写了几十个返回 null 的空方法；
 * Java 8+ 可以用接口默认方法把这些样板代码集中到一处，
 * 实现类只需要覆盖自己真正支持的那几个方法。
 * （本文件由脚本根据 jakarta.servlet 6.1 接口自动生成。）
 */
public interface HttpServletResponseStub extends HttpServletResponse, ServletResponseStub {

    private static UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException("本章的简化实现不支持 " + method + "()");
    }

    @Override default void addCookie(Cookie arg0) { throw unsupported("addCookie"); }
    @Override default boolean containsHeader(String arg0) { throw unsupported("containsHeader"); }
    @Override default String encodeURL(String arg0) { throw unsupported("encodeURL"); }
    @Override default String encodeRedirectURL(String arg0) { throw unsupported("encodeRedirectURL"); }
    @Override default void sendError(int arg0, String arg1) throws IOException { throw unsupported("sendError"); }
    @Override default void sendError(int arg0) throws IOException { throw unsupported("sendError"); }
    @Override default void sendRedirect(String arg0, int arg1, boolean arg2) throws IOException { throw unsupported("sendRedirect"); }
    @Override default void setDateHeader(String arg0, long arg1) { throw unsupported("setDateHeader"); }
    @Override default void addDateHeader(String arg0, long arg1) { throw unsupported("addDateHeader"); }
    @Override default void setHeader(String arg0, String arg1) { throw unsupported("setHeader"); }
    @Override default void addHeader(String arg0, String arg1) { throw unsupported("addHeader"); }
    @Override default void setIntHeader(String arg0, int arg1) { throw unsupported("setIntHeader"); }
    @Override default void addIntHeader(String arg0, int arg1) { throw unsupported("addIntHeader"); }
    @Override default void setStatus(int arg0) { throw unsupported("setStatus"); }
    @Override default int getStatus() { throw unsupported("getStatus"); }
    @Override default String getHeader(String arg0) { throw unsupported("getHeader"); }
    @Override default Collection<String> getHeaders(String arg0) { throw unsupported("getHeaders"); }
    @Override default Collection<String> getHeaderNames() { throw unsupported("getHeaderNames"); }
}
