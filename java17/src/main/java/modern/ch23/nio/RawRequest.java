package modern.ch23.nio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 请求行 + 请求头。相当于 Coyote 的 org.apache.coyote.Request：只有协议层面的原始数据，
 * 没有 Session、没有 Servlet 映射——那些是 CoyoteAdapter 交给 Catalina 之后的事。
 */
public record RawRequest(String method, String uri, String protocol, Map<String, String> headers) {

    public String header(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    /** HTTP/1.1 默认长连接，除非 Connection: close；HTTP/1.0 默认短连接 */
    public boolean keepAlive() {
        String c = header("connection");
        return "HTTP/1.1".equals(protocol) ? !"close".equalsIgnoreCase(c) : "keep-alive".equalsIgnoreCase(c);
    }

    /**
     * 尝试从缓冲区里解析出一个完整的请求头。数据还不完整就返回 null，缓冲区位置保持不变，
     * 等下一批字节到了再试——这就是非阻塞解析：读不全不等待，先把线程还回去。
     */
    static RawRequest tryParse(ByteBuffer buf) {
        int end = indexOfHeaderEnd(buf);
        if (end < 0) {
            return null;
        }
        byte[] head = new byte[end - buf.position()];
        buf.get(head);
        buf.position(end + 4);                                     // 跳过 \r\n\r\n
        String[] lines = new String(head, StandardCharsets.ISO_8859_1).split("\r\n");
        String[] first = lines[0].split(" ");
        if (first.length != 3) {
            throw new IllegalArgumentException("请求行格式错误: " + lines[0]);
        }
        var headers = new LinkedHashMap<String, String>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                headers.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT), lines[i].substring(colon + 1).trim());
            }
        }
        return new RawRequest(first[0], first[1], first[2], headers);
    }

    private static int indexOfHeaderEnd(ByteBuffer buf) {
        for (int i = buf.position(); i + 3 < buf.limit(); i++) {
            if (buf.get(i) == '\r' && buf.get(i + 1) == '\n' && buf.get(i + 2) == '\r' && buf.get(i + 3) == '\n') {
                return i;
            }
        }
        return -1;
    }
}
