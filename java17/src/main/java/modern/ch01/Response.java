package modern.ch01;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HTTP 响应：把 WEB_ROOT 下的静态文件发给浏览器。
 *
 * 原书版本直接把文件字节写回去，连 "HTTP/1.1 200 OK" 状态行都没有，
 * 只是因为浏览器很宽容才能显示出来。这里补上了完整的响应格式：
 *
 *   状态行 CRLF
 *   响应头 CRLF ...
 *   CRLF
 *   实体正文
 */
public final class Response {

    private final OutputStream output;

    public Response(OutputStream output) {
        this.output = output;
    }

    public void sendStaticResource(Request request) throws IOException {
        Path file = resolve(request.uri());
        if (file != null && Files.isRegularFile(file)) {
            byte[] body = Files.readAllBytes(file);
            String type = Files.probeContentType(file);
            writeHead(200, "OK", type != null ? type : "application/octet-stream", body.length);
            output.write(body);
        } else {
            byte[] body = "<h1>File Not Found</h1>".getBytes(StandardCharsets.UTF_8);
            writeHead(404, "Not Found", "text/html; charset=UTF-8", body.length);
            output.write(body);
        }
        output.flush();
    }

    /** 把 URI 映射成 WEB_ROOT 下的文件；含 "../" 想跳出 WEB_ROOT 的一律返回 null */
    static Path resolve(String uri) {
        String path = uri.split("\\?", 2)[0];               // 去掉查询串
        Path file = HttpServer.WEB_ROOT.resolve(path.substring(1)).normalize();
        return file.startsWith(HttpServer.WEB_ROOT) ? file : null;
    }

    private void writeHead(int status, String reason, String contentType, int length) throws IOException {
        String head = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        output.write(head.getBytes(StandardCharsets.ISO_8859_1));
    }
}
