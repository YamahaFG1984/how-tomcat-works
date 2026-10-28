package modern.ch01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 请求。原书的 Request 是一个可变对象：先 new，再 parse()，再 getUri()。
 * 这里改成 record：解析成功就得到一个字段齐全、不可变的对象。
 */
public record Request(String method, String uri, String protocol) {

    /**
     * 读取请求行和请求头（打印到控制台），返回解析后的请求；连接上没有任何数据时返回 null。
     * HTTP 报文头部规定用 ISO-8859-1 编码，所以这里按字节一一映射成字符。
     */
    public static Request parse(InputStream input) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.ISO_8859_1));
        String requestLine = reader.readLine();          // 例如 "GET /index.html HTTP/1.1"
        if (requestLine == null || requestLine.isBlank()) {
            return null;
        }
        System.out.println(requestLine);
        // 请求头一直读到空行为止；第 1 章只把它们打印出来
        for (String line; (line = reader.readLine()) != null && !line.isEmpty(); ) {
            System.out.println(line);
        }
        System.out.println();

        String[] parts = requestLine.split(" ");
        return parts.length == 3
                ? new Request(parts[0], parts[1], parts[2])
                : new Request(parts[0], parts.length > 1 ? parts[1] : "/", "HTTP/1.0");
    }
}
