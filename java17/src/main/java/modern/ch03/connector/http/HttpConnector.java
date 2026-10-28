package modern.ch03.connector.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 连接器：在自己的线程里等待连接，每接到一个 socket 就交给一个新的 HttpProcessor。
 * 本章仍然是串行处理——上一个请求处理完才会 accept 下一个（第 4 章改为处理器池）。
 */
public class HttpConnector implements Runnable {

    private final int port;
    private volatile boolean stopped;

    public HttpConnector(int port) {
        this.port = port;
    }

    public String getScheme() {
        return "http";
    }

    @Override
    public void run() {
        try (var serverSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            System.out.println("ch03 连接器监听 http://localhost:" + port + "/");
            while (!stopped) {
                Socket socket = serverSocket.accept();
                new HttpProcessor(this).process(socket);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void start() {
        new Thread(this, "http-connector-" + port).start();
    }
}
