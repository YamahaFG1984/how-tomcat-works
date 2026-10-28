package modern.ch21.mini;

import modern.common.HttpServletResponseStub;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存里的响应：响应体先写进缓冲区，flushBuffer() 之后视为「已提交」（响应头已经发给客户端）。
 * 「已提交」是理解 forward 的关键：一旦提交，状态码和响应头就改不了了。
 */
public final class MockResponse implements HttpServletResponseStub {

    private final StringWriter committedBody = new StringWriter();
    private StringWriter buffer = new StringWriter();
    private PrintWriter writer = new PrintWriter(buffer);
    private final Map<String, String> headers = new LinkedHashMap<>();
    private int status = 200;
    private String contentType;
    private boolean committed;

    @Override public PrintWriter getWriter() { return writer; }
    @Override public boolean isCommitted() { return committed; }

    @Override public void flushBuffer() {
        writer.flush();
        committedBody.append(buffer.toString());
        buffer = new StringWriter();
        writer = new PrintWriter(buffer);
        committed = true;
    }

    @Override public void resetBuffer() {
        if (committed) throw new IllegalStateException("响应已提交，不能再清空缓冲区");
        buffer.getBuffer().setLength(0);
    }

    @Override public void reset() {
        resetBuffer();
        headers.clear();
        status = 200;
        contentType = null;
    }

    @Override public void setStatus(int sc) { if (!committed) status = sc; }
    @Override public int getStatus() { return status; }
    @Override public void setHeader(String name, String value) { if (!committed) headers.put(name, value); }
    @Override public void addHeader(String name, String value) { setHeader(name, value); }
    @Override public String getHeader(String name) { return headers.get(name); }
    @Override public Collection<String> getHeaderNames() { return List.copyOf(headers.keySet()); }
    @Override public void setContentType(String type) { if (!committed) contentType = type; }
    @Override public String getContentType() { return contentType; }
    @Override public void setCharacterEncoding(String charset) { }
    @Override public String getCharacterEncoding() { return "UTF-8"; }

    @Override public void sendRedirect(String location, int sc, boolean clearBuffer) {
        if (committed) throw new IllegalStateException("响应已提交，不能重定向");
        if (clearBuffer) resetBuffer();
        status = sc;
        headers.put("Location", location);
        flushBuffer();
    }

    /** forward 结束后调用：响应已经完成，之后的写入一律丢弃（Tomcat 里是 Response.finishResponse + 输出挂起） */
    public void finish() {
        flushBuffer();
        writer = new PrintWriter(java.io.Writer.nullWriter());
    }

    /** 已提交的内容 + 还在缓冲区里的内容 */
    public String body() {
        writer.flush();
        return committedBody + buffer.toString();
    }

    @Override public String toString() {
        return "HTTP " + status + " " + headers + (contentType == null ? "" : " Content-Type=" + contentType);
    }
}
