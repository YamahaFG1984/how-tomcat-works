package modern.ch26.ws;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 不依赖任何库的 WebSocket 服务器（RFC 6455 的最小子集），看清楚协议本身：
 *   ① 握手：一个普通的 HTTP GET，带 Upgrade: websocket 和 Sec-WebSocket-Key；
 *          服务器回 101，Sec-WebSocket-Accept = Base64(SHA-1(key + 固定 GUID))
 *   ② 之后同一个 TCP 连接上不再是 HTTP，而是一帧一帧的二进制数据
 * 支持文本帧、ping/pong、close；收到 "time" 回当前时间，其他内容原样回显。
 * 为了专注协议，用的是一个连接一个线程（第 4 章的模型）。
 *
 * 运行：./run.sh ch26.ws.MiniWebSocketServer [端口]
 *       ./run.sh ch26.ws.WsClient ws://localhost:8080/
 */
public final class MiniWebSocketServer {

    static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";   // RFC 6455 规定的常量

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        try (var server = new ServerSocket(port, 50, InetAddress.getLoopbackAddress())) {
            System.out.println("迷你 WebSocket 服务器：ws://localhost:" + port + "/");
            while (true) {
                Socket s = server.accept();
                new Thread(() -> handle(s), "ws-" + s.getPort()).start();
            }
        }
    }

    private static void handle(Socket socket) {
        try (socket) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            Map<String, String> headers = readHttpHeaders(in);
            String key = headers.get("sec-websocket-key");
            if (key == null || !"websocket".equalsIgnoreCase(headers.get("upgrade"))) {
                out.write("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                return;
            }
            String accept = acceptKey(key);
            out.write(("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            System.out.println("[握手] key=" + key + " → accept=" + accept);

            var data = new DataInputStream(in);
            while (true) {
                Frame f = Frame.read(data);
                switch (f.opcode()) {
                    case Frame.TEXT -> {
                        String msg = new String(f.payload(), StandardCharsets.UTF_8);
                        System.out.printf("[收到] fin=%s masked=%s len=%d \"%s\"%n", f.fin(), f.masked(), f.payload().length, msg);
                        String reply = msg.equals("time") ? "现在是 " + LocalTime.now().withNano(0) : "echo: " + msg;
                        Frame.write(out, Frame.TEXT, reply.getBytes(StandardCharsets.UTF_8));
                    }
                    case Frame.PING -> Frame.write(out, Frame.PONG, f.payload());
                    case Frame.CLOSE -> {
                        Frame.write(out, Frame.CLOSE, f.payload());   // 回一个 close 帧，然后关闭 TCP 连接
                        System.out.println("[关闭] 客户端发起关闭");
                        return;
                    }
                    default -> { }                                  // 二进制帧、pong 等忽略
                }
            }
        } catch (IOException e) {
            System.out.println("[断开] " + e.getMessage());
        }
    }

    static String acceptKey(String key) {
        try {
            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest((key + GUID).getBytes(StandardCharsets.ISO_8859_1));
            return Base64.getEncoder().encodeToString(sha1);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> readHttpHeaders(InputStream in) throws IOException {
        var sb = new StringBuilder();
        while (!sb.toString().endsWith("\r\n\r\n")) {
            int b = in.read();
            if (b < 0) throw new IOException("握手未完成就断开了");
            sb.append((char) b);
        }
        Map<String, String> headers = new HashMap<>();
        String[] lines = sb.toString().split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            int c = lines[i].indexOf(':');
            if (c > 0) headers.put(lines[i].substring(0, c).trim().toLowerCase(Locale.ROOT), lines[i].substring(c + 1).trim());
        }
        return headers;
    }
}
