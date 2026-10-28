package modern.common;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * ServletRequest 的「空实现」：接口里的每个方法默认都抛 UnsupportedOperationException。
 *
 * 原书为了让 Request/Response 能编译，手写了几十个返回 null 的空方法；
 * Java 8+ 可以用接口默认方法把这些样板代码集中到一处，
 * 实现类只需要覆盖自己真正支持的那几个方法。
 * （本文件由脚本根据 jakarta.servlet 6.1 接口自动生成。）
 */
public interface ServletRequestStub extends ServletRequest {

    private static UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException("本章的简化实现不支持 " + method + "()");
    }

    @Override default Object getAttribute(String arg0) { throw unsupported("getAttribute"); }
    @Override default Enumeration<String> getAttributeNames() { throw unsupported("getAttributeNames"); }
    @Override default String getCharacterEncoding() { throw unsupported("getCharacterEncoding"); }
    @Override default void setCharacterEncoding(String arg0) throws UnsupportedEncodingException { throw unsupported("setCharacterEncoding"); }
    @Override default int getContentLength() { throw unsupported("getContentLength"); }
    @Override default long getContentLengthLong() { throw unsupported("getContentLengthLong"); }
    @Override default String getContentType() { throw unsupported("getContentType"); }
    @Override default ServletInputStream getInputStream() throws IOException { throw unsupported("getInputStream"); }
    @Override default String getParameter(String arg0) { throw unsupported("getParameter"); }
    @Override default Enumeration<String> getParameterNames() { throw unsupported("getParameterNames"); }
    @Override default String[] getParameterValues(String arg0) { throw unsupported("getParameterValues"); }
    @Override default Map<String, String[]> getParameterMap() { throw unsupported("getParameterMap"); }
    @Override default String getProtocol() { throw unsupported("getProtocol"); }
    @Override default String getScheme() { throw unsupported("getScheme"); }
    @Override default String getServerName() { throw unsupported("getServerName"); }
    @Override default int getServerPort() { throw unsupported("getServerPort"); }
    @Override default BufferedReader getReader() throws IOException { throw unsupported("getReader"); }
    @Override default String getRemoteAddr() { throw unsupported("getRemoteAddr"); }
    @Override default String getRemoteHost() { throw unsupported("getRemoteHost"); }
    @Override default void setAttribute(String arg0, Object arg1) { throw unsupported("setAttribute"); }
    @Override default void removeAttribute(String arg0) { throw unsupported("removeAttribute"); }
    @Override default Locale getLocale() { throw unsupported("getLocale"); }
    @Override default Enumeration<Locale> getLocales() { throw unsupported("getLocales"); }
    @Override default boolean isSecure() { throw unsupported("isSecure"); }
    @Override default RequestDispatcher getRequestDispatcher(String arg0) { throw unsupported("getRequestDispatcher"); }
    @Override default int getRemotePort() { throw unsupported("getRemotePort"); }
    @Override default String getLocalName() { throw unsupported("getLocalName"); }
    @Override default String getLocalAddr() { throw unsupported("getLocalAddr"); }
    @Override default int getLocalPort() { throw unsupported("getLocalPort"); }
    @Override default ServletContext getServletContext() { throw unsupported("getServletContext"); }
    @Override default AsyncContext startAsync() throws IllegalStateException { throw unsupported("startAsync"); }
    @Override default AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException { throw unsupported("startAsync"); }
    @Override default boolean isAsyncStarted() { throw unsupported("isAsyncStarted"); }
    @Override default boolean isAsyncSupported() { throw unsupported("isAsyncSupported"); }
    @Override default AsyncContext getAsyncContext() { throw unsupported("getAsyncContext"); }
    @Override default DispatcherType getDispatcherType() { throw unsupported("getDispatcherType"); }
    @Override default String getRequestId() { throw unsupported("getRequestId"); }
    @Override default String getProtocolRequestId() { throw unsupported("getProtocolRequestId"); }
    @Override default ServletConnection getServletConnection() { throw unsupported("getServletConnection"); }
}
