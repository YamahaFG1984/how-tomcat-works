package modern.ch02;

import modern.common.ServletResponseStub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 容器内部的响应对象。
 *
 * 原书的 getWriter() 返回 new PrintWriter(output, true)：只有 println 会刷新，
 * 所以 PrimitiveServlet 最后那句 print("Violets are blue.") 永远到不了浏览器。
 * 这里改为先把 Servlet 的输出写进内存缓冲区，service() 返回后由容器调用 finish()
 * 一次性发出「状态行 + 响应头 + 正文」，顺带也能算出 Content-Length。
 */
public class Response implements ServletResponseStub {

    private final OutputStream output;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private PrintWriter writer;
    private String contentType;

    public Response(OutputStream output) {
        this.output = output;
    }

    /** 容器内部方法：发送静态资源 */
    public void sendStaticResource(String uri) throws IOException {
        Path file = Constants.WEB_ROOT.resolve(uri.substring(1)).normalize();
        if (file.startsWith(Constants.WEB_ROOT) && Files.isRegularFile(file)) {
            contentType = Files.probeContentType(file);
            body.write(Files.readAllBytes(file));
            finish(200, "OK");
        } else {
            contentType = "text/html; charset=UTF-8";
            body.write("<h1>File Not Found</h1>".getBytes(StandardCharsets.UTF_8));
            finish(404, "Not Found");
        }
    }

    /** 容器内部方法：Servlet 执行完毕后，把缓冲的内容真正写到 socket */
    public void finish(int status, String reason) throws IOException {
        if (writer != null) {
            writer.flush();
        }
        String head = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + (contentType != null ? contentType : "text/plain; charset=UTF-8") + "\r\n"
                + "Content-Length: " + body.size() + "\r\n"
                + "Connection: close\r\n\r\n";
        output.write(head.getBytes(StandardCharsets.ISO_8859_1));
        body.writeTo(output);
        output.flush();
    }

    /* ---------- ServletResponse 中本章真正实现的几个方法 ---------- */

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(body, StandardCharsets.UTF_8));
        }
        return writer;
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }
}
