package modern.ch14.core;

import modern.ch06.core.Lifecycle;
import modern.ch06.core.LifecycleException;
import modern.ch06.core.LifecycleListener;
import modern.ch06.core.LifecycleSupport;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static modern.ch06.core.LifecycleEvent.Type.AFTER_START;
import static modern.ch06.core.LifecycleEvent.Type.AFTER_STOP;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_START;
import static modern.ch06.core.LifecycleEvent.Type.BEFORE_STOP;
import static modern.ch06.core.LifecycleEvent.Type.START;
import static modern.ch06.core.LifecycleEvent.Type.STOP;

/**
 * 服务器（原书 org.apache.catalina.core.StandardServer）：整个 Catalina 的最顶层组件。
 *   initialize() → start() → await()（阻塞，直到在关闭端口收到正确的关闭命令）→ stop()
 */
public class StandardServer implements Lifecycle {

    private final LifecycleSupport lifecycle = new LifecycleSupport(this);
    private final List<StandardService> services = new CopyOnWriteArrayList<>();
    private int port = 8005;
    private String shutdown = "SHUTDOWN";
    private boolean initialized;
    private boolean started;

    public void setPort(int port) { this.port = port; }
    public int getPort() { return port; }
    public void setShutdown(String shutdown) { this.shutdown = shutdown; }
    public String getShutdown() { return shutdown; }

    public void addService(StandardService service) {
        services.add(service);
    }

    public List<StandardService> findServices() {
        return List.copyOf(services);
    }

    public void initialize() throws LifecycleException {
        if (initialized) {
            throw new LifecycleException("This server has already been initialized");
        }
        initialized = true;
        for (StandardService service : services) {
            service.initialize();
        }
    }

    @Override
    public void start() throws LifecycleException {
        if (started) {
            throw new LifecycleException("This server has already been started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_START, null);
        lifecycle.fireLifecycleEvent(START, null);
        started = true;
        for (StandardService service : services) {
            service.start();
        }
        lifecycle.fireLifecycleEvent(AFTER_START, null);
    }

    @Override
    public void stop() throws LifecycleException {
        if (!started) {
            throw new LifecycleException("This server has not yet been started");
        }
        lifecycle.fireLifecycleEvent(BEFORE_STOP, null);
        lifecycle.fireLifecycleEvent(STOP, null);
        started = false;
        for (StandardService service : services) {
            service.stop();
        }
        lifecycle.fireLifecycleEvent(AFTER_STOP, null);
    }

    /**
     * 在关闭端口上等待关闭命令（原书 Listing 14.5）。
     * 每个连接最多读 1024 个字符、最多等 10 秒，遇到控制字符就停止读取——防止有人连上来不说话或者发一大堆数据。
     */
    public void await() throws IOException {
        if (port < 0) {
            return;                                     // port = -1：不开关闭端口（现代 Tomcat 推荐的安全做法）
        }
        try (var serverSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            System.out.println("StandardServer: 在 127.0.0.1:" + port + " 等待关闭命令");
            while (true) {
                String command;
                try (Socket socket = serverSocket.accept()) {
                    socket.setSoTimeout(10_000);
                    command = readCommand(socket.getInputStream());
                } catch (IOException e) {
                    System.err.println("StandardServer.await: " + e);
                    continue;
                }
                // 常数时间比较，不给攻击者通过响应时间猜命令的机会
                if (MessageDigest.isEqual(command.getBytes(StandardCharsets.UTF_8), shutdown.getBytes(StandardCharsets.UTF_8))) {
                    System.out.println("StandardServer: 收到关闭命令");
                    return;
                }
                System.err.println("StandardServer.await: Invalid command '" + command + "' received");
            }
        }
    }

    private static String readCommand(InputStream in) throws IOException {
        var command = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            int ch = in.read();
            if (ch < 32) {                              // 控制字符或 EOF（-1）结束
                break;
            }
            command.append((char) ch);
        }
        return command.toString();
    }

    @Override public void addLifecycleListener(LifecycleListener l) { lifecycle.addLifecycleListener(l); }
    @Override public List<LifecycleListener> findLifecycleListeners() { return lifecycle.findLifecycleListeners(); }
    @Override public void removeLifecycleListener(LifecycleListener l) { lifecycle.removeLifecycleListener(l); }
}
