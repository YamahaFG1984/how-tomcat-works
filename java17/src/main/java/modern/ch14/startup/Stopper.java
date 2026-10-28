package modern.ch14.startup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 向关闭端口发送 SHUTDOWN（原书 ex14.pyrmont.startup.Stopper，来自 Catalina.stop()）。
 * 对第 14 章的迷你服务器和 Tomcat 11 都有效。
 * 运行：./run.sh ch14.startup.Stopper [端口] [命令]
 */
public final class Stopper {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8005;
        String shutdown = args.length > 1 ? args[1] : "SHUTDOWN";
        try (var socket = new Socket(InetAddress.getLoopbackAddress(), port);
             var out = socket.getOutputStream()) {
            out.write(shutdown.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            System.out.println("The server was successfully shut down.");
        } catch (IOException e) {
            System.out.println("Error. The server has not been started.");
        }
    }
}
