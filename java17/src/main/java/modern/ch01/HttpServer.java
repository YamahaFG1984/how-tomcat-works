package modern.ch01;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

/**
 * 第 1 章：一个最简单的 Web 服务器（对应原书 ex01.pyrmont.HttpServer）。
 *
 * 和原书相比的改动：
 *  - try-with-resources 自动关闭 ServerSocket / Socket，不再手写 finally；
 *  - 用 java.nio.file.Path 表示 WEB_ROOT；
 *  - Request 是一个不可变的 record，解析逻辑放进静态工厂方法；
 *  - Response 会发送真正的状态行和响应头，并防止 "../" 路径穿越。
 *
 * 运行：./run.sh ch01.HttpServer   然后访问 http://localhost:8080/index.html
 * 停止：访问 http://localhost:8080/SHUTDOWN
 */
public final class HttpServer {

    /** 静态资源目录：当前工作目录下的 webroot */
    public static final Path WEB_ROOT = Path.of(System.getProperty("user.dir"), "webroot");

    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new HttpServer().await(port);
    }

    public void await(int port) throws IOException {
        // backlog = 1，只监听本机回环地址，和原书一致
        try (var serverSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            System.out.println("ch01 HttpServer 监听 http://localhost:" + port + "/  WEB_ROOT=" + WEB_ROOT);
            boolean shutdown = false;
            while (!shutdown) {
                try (Socket socket = serverSocket.accept()) {
                    Request request = Request.parse(socket.getInputStream());
                    if (request == null) {
                        continue;   // 客户端连上就断开，什么也没发
                    }
                    new Response(socket.getOutputStream()).sendStaticResource(request);
                    shutdown = SHUTDOWN_COMMAND.equals(request.uri());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("收到 " + SHUTDOWN_COMMAND + "，服务器退出");
    }
}
