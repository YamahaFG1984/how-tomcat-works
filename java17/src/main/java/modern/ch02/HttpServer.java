package modern.ch02;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * HttpServer1 和 HttpServer2 共用的主循环：以 "/servlet/" 开头的请求交给 Servlet 处理器，
 * 其余交给静态资源处理器。原书把这段循环在两个类里各写了一遍。
 */
final class HttpServer {

    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

    private final Processor servletProcessor;
    private final Processor staticProcessor = new StaticResourceProcessor();

    HttpServer(Processor servletProcessor) {
        this.servletProcessor = servletProcessor;
    }

    void await(int port) throws IOException {
        try (var serverSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            System.out.println("监听 http://localhost:" + port + "/  试试 /index.html 和 /servlet/PrimitiveServlet");
            boolean shutdown = false;
            while (!shutdown) {
                try (Socket socket = serverSocket.accept()) {
                    Request request = Request.parse(socket.getInputStream());
                    if (request == null) {
                        continue;
                    }
                    Response response = new Response(socket.getOutputStream());
                    Processor processor = request.uri().startsWith("/servlet/") ? servletProcessor : staticProcessor;
                    processor.process(request, response);
                    shutdown = SHUTDOWN_COMMAND.equals(request.uri());
                } catch (Exception e) {
                    e.printStackTrace();          // 原书在这里 System.exit(1)，一个坏请求就会让整个服务器退出
                }
            }
        }
    }

    static int port(String[] args) {
        return args.length > 0 ? Integer.parseInt(args[0]) : 8080;
    }
}
