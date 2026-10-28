package modern.ch23.nio;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 「慢客户端」：打开 N 个连接，每个只发半个请求头就不动了（像网络很差的手机，或者 Slowloris 攻击）。
 * 然后用一个正常客户端发请求，看服务器还能不能及时响应。
 *
 * 运行：./run.sh ch23.nio.SlowClients [端口] [连接数] [保持秒数]
 */
public final class SlowClients {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int count = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        int holdSeconds = args.length > 2 ? Integer.parseInt(args[2]) : 5;

        List<Socket> sockets = new ArrayList<>();
        int opened = 0;
        for (int i = 0; i < count; i++) {
            try {
                var s = new Socket();
                s.connect(new InetSocketAddress("127.0.0.1", port), 2000);
                OutputStream out = s.getOutputStream();
                out.write("GET / HTTP/1.1\r\nHost: localhost\r\n".getBytes(StandardCharsets.ISO_8859_1)); // 缺最后的空行
                out.flush();
                sockets.add(s);
                opened++;
            } catch (Exception e) {
                System.out.println("第 " + (i + 1) + " 个连接失败：" + e);
                break;
            }
        }
        System.out.println("已打开 " + opened + " 个只发了半个请求头的连接");

        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        long t0 = System.nanoTime();
        try {
            var resp = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                    .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            System.out.printf("正常请求：HTTP %d，耗时 %d ms，响应：%s", resp.statusCode(),
                    (System.nanoTime() - t0) / 1_000_000, resp.body());
        } catch (Exception e) {
            System.out.printf("正常请求失败（%d ms）：%s%n", (System.nanoTime() - t0) / 1_000_000, e);
        }

        Thread.sleep(holdSeconds * 1000L);
        for (Socket s : sockets) s.close();
    }
}
