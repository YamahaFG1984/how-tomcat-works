package modern.ch21.mini;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.util.Locale;

/**
 * 被 include 的 Servlet 只能写响应体：改状态码、响应头、内容类型的调用都被<strong>静默忽略</strong>
 * （规范如此要求，Tomcat 用 ApplicationHttpResponse 实现，included 标志为 true 时直接 return）。
 */
final class IncludedResponse extends HttpServletResponseWrapper {

    IncludedResponse(HttpServletResponse response) {
        super(response);
    }

    @Override public void setStatus(int sc) { }
    @Override public void setHeader(String name, String value) { }
    @Override public void addHeader(String name, String value) { }
    @Override public void setIntHeader(String name, int value) { }
    @Override public void addIntHeader(String name, int value) { }
    @Override public void setDateHeader(String name, long date) { }
    @Override public void addDateHeader(String name, long date) { }
    @Override public void addCookie(Cookie cookie) { }
    @Override public void setContentType(String type) { }
    @Override public void setContentLength(int len) { }
    @Override public void setContentLengthLong(long len) { }
    @Override public void setCharacterEncoding(String charset) { }
    @Override public void setLocale(Locale loc) { }
    @Override public void sendError(int sc) { }
    @Override public void sendError(int sc, String msg) { }
    @Override public void reset() { }
    @Override public void resetBuffer() { }
}
