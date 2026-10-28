package modern.ch03.connector.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 包装 socket 输入流，提供 readRequestLine() 和 readHeader() 两个方法。
 * 和原书一样，它是一个 InputStream：读完头部之后，剩下的字节就是请求体，
 * 后面 getInputStream() / 解析表单参数时会从这里继续读。
 */
public class SocketInputStream extends BufferedInputStream {

    /** 一行最多 8KB，超出就认为是恶意请求（Tomcat 默认 maxHttpHeaderSize 也是 8KB） */
    private static final int MAX_LINE = 8192;

    public SocketInputStream(InputStream in, int bufferSize) {
        super(in, bufferSize);
    }

    /** 读第一行，例如 "GET /index.html?x=1 HTTP/1.1"；连接上什么都没有时返回 null */
    public HttpRequestLine readRequestLine() throws IOException {
        String line;
        do {
            line = readLine();
            if (line == null) {
                return null;
            }
        } while (line.isEmpty());       // 规范允许请求行之前出现多余的空行
        String[] parts = line.split(" ", 3);
        if (parts.length != 3) {
            throw new IOException("请求行格式错误: " + line);
        }
        return new HttpRequestLine(parts[0], parts[1], parts[2]);
    }

    /** 读下一个请求头；遇到空行（头部结束）返回 null */
    public HttpHeader readHeader() throws IOException {
        String line = readLine();
        if (line == null || line.isEmpty()) {
            return null;
        }
        int colon = line.indexOf(':');
        if (colon <= 0) {
            throw new IOException("请求头缺少冒号: " + line);
        }
        return new HttpHeader(line.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                line.substring(colon + 1).trim());
    }

    /** 读到 LF 为止（去掉结尾的 CR），按 ISO-8859-1 把字节映射成字符 */
    private String readLine() throws IOException {
        var buf = new ByteArrayOutputStream(128);
        int b;
        while ((b = read()) != -1 && b != '\n') {
            if (buf.size() >= MAX_LINE) {
                throw new IOException("请求行或请求头太长");
            }
            buf.write(b);
        }
        if (b == -1 && buf.size() == 0) {
            return null;
        }
        String line = buf.toString(StandardCharsets.ISO_8859_1);
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}
