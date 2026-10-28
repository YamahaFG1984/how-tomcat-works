package modern.ch04.connector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ServerSocketFactory;

/**
 * Tomcat 4 默认连接器的 Java 17 版。
 *
 * 原书：Stack<HttpProcessor> 处理器池 + 每个处理器一个线程 + wait/notifyAll 交接 socket。
 * 这里：ThreadPoolExecutor + SynchronousQueue，行为完全一致——
 *   - 启动时预先创建 minProcessors 个线程（prestartAllCoreThreads）；
 *   - 有空闲线程就直接把 socket「交到它手上」（SynchronousQueue 没有容量，只做交接）；
 *   - 没有空闲线程就新建，直到 maxProcessors；
 *   - 达到上限后拒绝：关闭 socket，记一条日志。
 */
public class HttpConnector implements Runnable {

    private final int port;
    private int minProcessors = 5;
    private int maxProcessors = 20;
    private int bufferSize = 2048;
    private int connectionTimeout = 20_000;      // keep-alive 连接空闲多久后关闭（毫秒）
    private boolean allowChunking = true;

    private Container container;
    private ServerSocket serverSocket;
    private ThreadPoolExecutor processors;
    private Thread thread;
    private volatile boolean stopped;

    public HttpConnector(int port) {
        this.port = port;
    }

    /* ---------- 配置 ---------- */

    public void setContainer(Container container) { this.container = container; }
    public Container getContainer() { return container; }
    public void setMinProcessors(int n) { this.minProcessors = n; }
    public void setMaxProcessors(int n) { this.maxProcessors = n; }
    public int getBufferSize() { return bufferSize; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public boolean isChunkingAllowed() { return allowChunking; }
    public boolean isStopped() { return stopped; }

    /* ---------- 生命周期：initialize → start → stop ---------- */

    /** 原书的 initialize() 调用 open()，从服务器套接字工厂取得 ServerSocket */
    public void initialize() throws IOException {
        if (serverSocket != null) {
            throw new IllegalStateException("HTTP connector has already been initialized");
        }
        serverSocket = ServerSocketFactory.getDefault()
                .createServerSocket(port, 100, InetAddress.getLoopbackAddress());
    }

    public void start() {
        var counter = new AtomicInteger();
        processors = new ThreadPoolExecutor(minProcessors, maxProcessors, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> new Thread(r, "HttpProcessor[" + port + "][" + counter.getAndIncrement() + "]"));
        processors.prestartAllCoreThreads();          // 对应原书 start() 里的 while (curProcessors < minProcessors)
        thread = new Thread(this, "HttpConnector[" + port + "]");
        thread.start();
        System.out.println("ch04 连接器启动：http://localhost:" + port
                + "/  处理器 min=" + minProcessors + " max=" + maxProcessors);
    }

    public void stop() throws InterruptedException {
        stopped = true;
        try {
            serverSocket.close();                     // 让阻塞在 accept() 上的连接器线程退出
        } catch (IOException ignored) {
        }
        processors.shutdown();
        processors.awaitTermination(5, TimeUnit.SECONDS);
        thread.join();
        System.out.println("ch04 连接器已停止");
    }

    @Override
    public void run() {
        while (!stopped) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketException e) {
                break;                                // serverSocket 被 stop() 关闭
            } catch (IOException e) {
                continue;
            }
            try {
                processors.execute(new HttpProcessor(this, socket));   // 相当于原书的 processor.assign(socket)
            } catch (RejectedExecutionException e) {
                System.out.println("httpConnector.noProcessor：没有可用的处理器，关闭这个连接");
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
