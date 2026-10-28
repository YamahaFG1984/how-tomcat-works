package modern.ch03.connector.http;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import modern.ch03.connector.ResponseStream;
import modern.common.HttpServletResponseStub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 实现 HttpServletResponse 的响应对象。
 *
 * Servlet 写的内容先进入内存缓冲区；finishResponse() 时再一次性发送
 * 「状态行 + 响应头（含 Content-Length）+ 正文」。原书本章的 finishResponse() 里
 * sendHeaders() 那一行是被注释掉的——也就是说 Servlet 的响应仍然没有状态行。
 */
public class HttpResponse implements HttpServletResponseStub {

    private static final String SERVER_INFO = "Pyrmont Servlet Container (Java 17)";

    private final OutputStream output;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private HttpRequest request;
    private PrintWriter writer;
    private ServletOutputStream stream;
    private int status = SC_OK;
    private String message;
    private String contentType;
    private String characterEncoding = "UTF-8";
    private boolean committed;

    public HttpResponse(OutputStream output) {
        this.output = output;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    /* ======== 容器内部方法 ======== */

    /** 把 webroot 下的静态文件作为响应正文 */
    public void sendStaticResource(Path webRoot) throws IOException {
        Path file = webRoot.resolve(request.getRequestURI().substring(1)).normalize();
        if (file.startsWith(webRoot) && Files.isRegularFile(file)) {
            setContentType(Files.probeContentType(file));
            Files.copy(file, body);
        } else {
            sendError(SC_NOT_FOUND, "File Not Found");
        }
    }

    /** Servlet 执行完毕后调用：发送状态行、响应头和缓冲的正文 */
    public void finishResponse() throws IOException {
        if (committed) {
            return;
        }
        flushWriter();
        writeHead(false);
        body.writeTo(output);
        output.flush();
    }

    /** 把 PrintWriter 里还没刷出去的字符刷进 body 缓冲区 */
    protected void flushWriter() {
        if (writer != null) {
            writer.flush();
        }
    }

    /**
     * 写出状态行和响应头。chunked 为 false 时根据缓冲区大小写 Content-Length，
     * 为 true 时写 Transfer-Encoding: chunked（第 4 章用到）。写完即视为已提交。
     */
    protected void writeHead(boolean chunked) throws IOException {
        var head = new StringBuilder();
        head.append(request.getProtocol()).append(' ').append(status).append(' ')
            .append(message != null ? message : reasonPhrase(status)).append("\r\n");
        if (contentType != null) {
            head.append("Content-Type: ").append(getContentType()).append("\r\n");
        }
        if (chunked) {
            head.append("Transfer-Encoding: chunked\r\n");
        } else {
            head.append("Content-Length: ").append(body.size()).append("\r\n");
        }
        head.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))).append("\r\n");
        headers.forEach((name, values) -> values.forEach(v -> head.append(name).append(": ").append(v).append("\r\n")));
        head.append("\r\n");
        output.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        committed = true;
    }

    /** 子类访问缓冲区和底层输出流 */
    protected ByteArrayOutputStream body() {
        return body;
    }

    protected OutputStream output() {
        return output;
    }

    protected HttpRequest request() {
        return request;
    }

    static String reasonPhrase(int status) {
        return switch (status) {
            case SC_OK -> "OK";
            case SC_CREATED -> "Created";
            case SC_NO_CONTENT -> "No Content";
            case SC_MOVED_PERMANENTLY -> "Moved Permanently";
            case SC_FOUND -> "Found";
            case SC_NOT_MODIFIED -> "Not Modified";
            case SC_BAD_REQUEST -> "Bad Request";
            case SC_UNAUTHORIZED -> "Unauthorized";
            case SC_FORBIDDEN -> "Forbidden";
            case SC_NOT_FOUND -> "Not Found";
            case SC_METHOD_NOT_ALLOWED -> "Method Not Allowed";
            case SC_INTERNAL_SERVER_ERROR -> "Internal Server Error";
            case SC_SERVICE_UNAVAILABLE -> "Service Unavailable";
            default -> "Unknown";
        };
    }

    /* ======== ServletResponse ======== */

    @Override
    public PrintWriter getWriter() {
        if (stream != null) {
            throw new IllegalStateException("getOutputStream() 已经被调用过了");
        }
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(body, Charset.forName(characterEncoding)));
        }
        return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() 已经被调用过了");
        }
        if (stream == null) {
            stream = new ResponseStream(body);
        }
        return stream;
    }

    @Override
    public void setContentType(String type) {
        if (type == null) {
            contentType = null;
            return;
        }
        int i = type.toLowerCase(Locale.ROOT).indexOf("charset=");
        if (i >= 0) {
            characterEncoding = type.substring(i + 8).trim();
            type = type.substring(0, type.indexOf(';') >= 0 ? type.indexOf(';') : i).trim();
        }
        contentType = type;
    }

    @Override
    public String getContentType() {
        if (contentType == null) {
            return null;
        }
        return contentType.startsWith("text/") ? contentType + "; charset=" + characterEncoding : contentType;
    }

    @Override public String getCharacterEncoding() { return characterEncoding; }
    @Override public void setCharacterEncoding(String charset) { this.characterEncoding = charset; }
    @Override public void setContentLength(int len) { /* 由 finishResponse 根据缓冲区大小计算 */ }
    @Override public void setContentLengthLong(long len) { /* 同上 */ }
    @Override public boolean isCommitted() { return committed; }
    @Override public int getBufferSize() { return Integer.MAX_VALUE; }
    @Override public void setBufferSize(int size) { }
    @Override public void flushBuffer() throws IOException { /* 本章全部缓冲，第 4 章的子类会覆盖它 */ }
    @Override public void resetBuffer() { flushWriter(); body.reset(); }
    @Override public void setLocale(Locale loc) { }
    @Override public Locale getLocale() { return Locale.getDefault(); }

    @Override
    public void reset() {
        body.reset();
        headers.clear();
        status = SC_OK;
        message = null;
    }

    /* ======== HttpServletResponse ======== */

    @Override public void setStatus(int sc) { this.status = sc; this.message = null; }
    @Override public int getStatus() { return status; }

    @Override
    public void sendError(int sc, String msg) {
        resetBuffer();                    // 规范：清空已缓冲的正文，但保留已设置的响应头（如 WWW-Authenticate）
        status = sc;
        message = msg;
        setContentType("text/html");
        getWriter().print("<h1>" + sc + " " + (msg != null ? msg : reasonPhrase(sc)) + "</h1>");
    }

    @Override
    public void sendError(int sc) {
        sendError(sc, null);
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) {
        if (clearBuffer) {
            resetBuffer();
        }
        setStatus(sc);
        setHeader("Location", location);
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, new ArrayList<>(List.of(value)));
    }

    @Override
    public void addHeader(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override public void setIntHeader(String name, int value) { setHeader(name, String.valueOf(value)); }
    @Override public void addIntHeader(String name, int value) { addHeader(name, String.valueOf(value)); }
    @Override public boolean containsHeader(String name) { return headers.containsKey(name); }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return values == null ? null : values.get(0);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return List.copyOf(headers.getOrDefault(name, List.of()));
    }

    @Override
    public Collection<String> getHeaderNames() {
        return List.copyOf(headers.keySet());
    }

    @Override
    public void addCookie(Cookie cookie) {
        var sb = new StringBuilder(cookie.getName()).append('=').append(cookie.getValue());
        if (cookie.getPath() != null) {
            sb.append("; Path=").append(cookie.getPath());
        }
        if (cookie.getMaxAge() >= 0) {
            sb.append("; Max-Age=").append(cookie.getMaxAge());
        }
        if (cookie.isHttpOnly()) {
            sb.append("; HttpOnly");
        }
        addHeader("Set-Cookie", sb.toString());
    }

    @Override public String encodeURL(String url) { return url; }
    @Override public String encodeRedirectURL(String url) { return url; }

    /** HttpProcessor 用它作为 Server 响应头的值 */
    public static String serverInfo() {
        return SERVER_INFO;
    }
}
