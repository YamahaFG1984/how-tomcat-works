package modern.ch23.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 迷你版 NioEndpoint：用三类线程分工处理连接（Tomcat 8.5 起默认的 HTTP/1.1 连接器就是这个结构）。
 *
 *   Acceptor（1 个）  阻塞在 accept() 上，拿到连接后设为非阻塞，交给 Poller；连接数到上限时自己停下来
 *   Poller（1 个）    一个 Selector 盯住所有空闲连接，哪个连接有数据可读，就派一个 Worker 去处理
 *   Worker（线程池）  读数据、解析、调用 Adapter、写响应；请求头还没收全就立刻把线程还回去
 *
 * 和第 4 章最大的区别：等待客户端发数据的时候，<strong>不占用任何线程</strong>。
 */
public final class NioEndpoint {

    private final int port;
    private final int workers;
    private final int maxConnections;
    private final long keepAliveTimeout;
    private final Adapter adapter;

    private final AtomicInteger connections = new AtomicInteger();
    private final Semaphore connectionLimit;                       // Tomcat: LimitLatch
    private volatile boolean running;
    private ServerSocketChannel server;
    private ExecutorService executor;
    private Poller poller;

    public NioEndpoint(int port, int workers, int maxConnections, long keepAliveTimeoutMillis, Adapter adapter) {
        this.port = port;
        this.workers = workers;
        this.maxConnections = maxConnections;
        this.keepAliveTimeout = keepAliveTimeoutMillis;
        this.adapter = adapter;
        this.connectionLimit = new Semaphore(maxConnections);
    }

    public int getConnections() { return connections.get(); }
    public int getMaxConnections() { return maxConnections; }

    /** 实际监听的端口（构造时传 0 表示让操作系统分配，测试里常用） */
    public int getLocalPort() throws IOException {
        return ((InetSocketAddress) server.getLocalAddress()).getPort();
    }

    public void start() throws IOException {
        server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("127.0.0.1", port), 100);   // 100 = acceptCount，操作系统的等待队列
        running = true;

        var seq = new AtomicInteger();
        executor = Executors.newFixedThreadPool(workers, r -> new Thread(r, "nio-exec-" + seq.incrementAndGet()));
        poller = new Poller();
        new Thread(poller, "nio-Poller").start();
        new Thread(this::acceptLoop, "nio-Acceptor").start();
    }

    public void stop() throws IOException {
        running = false;
        server.close();                                            // 让 Acceptor 从 accept() 里出来
        poller.selector.wakeup();
        executor.shutdown();
    }

    /* ---------------- Acceptor ---------------- */

    private void acceptLoop() {
        while (running) {
            try {
                connectionLimit.acquire();                         // 到上限就阻塞在这里，新连接留在 backlog 里排队
                SocketChannel ch;
                try {
                    ch = server.accept();
                } catch (IOException e) {
                    connectionLimit.release();
                    continue;
                }
                ch.configureBlocking(false);
                ch.socket().setTcpNoDelay(true);
                connections.incrementAndGet();
                poller.register(new SocketWrapper(ch));
            } catch (InterruptedException | IOException e) {
                if (running) e.printStackTrace();
            }
        }
    }

    private void close(SocketWrapper w) {
        if (!w.channel.isOpen()) return;
        try {
            if (w.key != null) w.key.cancel();
            w.channel.close();
        } catch (IOException ignored) {
            // 关闭时出错没有补救办法，忽略
        } finally {
            connections.decrementAndGet();
            connectionLimit.release();
        }
    }

    /* ---------------- Poller ---------------- */

    private final class Poller implements Runnable {

        final Selector selector;
        /** 其他线程不能直接改 Selector 的注册状态（会和 select() 互相阻塞），所以把操作排队，由 Poller 自己执行 */
        final Queue<Runnable> events = new ConcurrentLinkedQueue<>();
        long lastTimeoutCheck = System.currentTimeMillis();

        Poller() throws IOException {
            selector = Selector.open();
        }

        void register(SocketWrapper w) {
            events.add(() -> {
                try {
                    w.key = w.channel.register(selector, SelectionKey.OP_READ, w);
                } catch (ClosedChannelException e) {
                    close(w);
                }
            });
            selector.wakeup();
        }

        /** Worker 处理完（或数据不完整）后调用：继续关注这个连接 */
        void readAgain(SocketWrapper w) {
            events.add(() -> {
                if (w.key.isValid()) w.key.interestOps(SelectionKey.OP_READ);
            });
            selector.wakeup();
        }

        @Override
        public void run() {
            while (running) {
                try {
                    for (Runnable r; (r = events.poll()) != null; ) r.run();
                    selector.select(1000);
                    var it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if (!key.isValid()) continue;
                        key.interestOps(0);                        // 交给 Worker 期间不再报告这个连接
                        executor.execute(new SocketProcessor((SocketWrapper) key.attachment()));
                    }
                    timeout();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            selector.keys().forEach(k -> close((SocketWrapper) k.attachment()));
            try {
                selector.close();
            } catch (IOException ignored) {
                // 退出时忽略
            }
        }

        /** 每秒扫一遍：空闲太久的长连接关掉（只看正在等待数据的，不打扰正在处理的） */
        private void timeout() {
            long now = System.currentTimeMillis();
            if (now - lastTimeoutCheck < 1000) return;
            lastTimeoutCheck = now;
            for (SelectionKey key : selector.keys()) {
                var w = (SocketWrapper) key.attachment();
                if (key.isValid() && key.interestOps() == SelectionKey.OP_READ && now - w.lastAccess > keepAliveTimeout) {
                    close(w);
                }
            }
        }
    }

    /* ---------------- Worker ---------------- */

    private final class SocketProcessor implements Runnable {

        private final SocketWrapper w;

        SocketProcessor(SocketWrapper w) {
            this.w = w;
        }

        @Override
        public void run() {
            try {
                ByteBuffer buf = w.readBuffer;
                buf.compact();                                     // 切到写模式，保留还没解析的字节
                int n = w.channel.read(buf);                       // 非阻塞：有多少读多少，立刻返回
                buf.flip();
                if (n < 0) {
                    close(w);                                      // 客户端关闭了连接
                    return;
                }
                w.lastAccess = System.currentTimeMillis();
                while (true) {
                    RawRequest req = RawRequest.tryParse(buf);
                    if (req == null) {
                        if (buf.remaining() == buf.capacity()) {   // 8KB 还装不下请求头
                            writeFully(encode(new Adapter.RawResponse(431, "text/plain", new byte[0]), false));
                            close(w);
                        } else {
                            poller.readAgain(w);                   // 不完整：线程走人，数据到了再说
                        }
                        return;
                    }
                    w.requests++;
                    CompletableFuture<Adapter.RawResponse> future = adapter.serviceAsync(req);
                    if (!future.isDone()) {
                        // 异步：Worker 线程现在就归还；业务完成时再派一个线程回来写响应（第 24 章）
                        future.whenComplete((r, e) -> executor.execute(() -> {
                            try {
                                if (respond(req, future)) run();       // 流水线里还有请求，接着处理
                            } catch (IOException | RuntimeException ex) {
                                close(w);
                            }
                        }));
                        return;
                    }
                    if (!respond(req, future)) {
                        return;
                    }
                    // 缓冲区里还有字节：流水线发来的下一个请求，继续循环
                }
            } catch (IOException | RuntimeException e) {
                close(w);
            }
        }

        /** 写响应；返回 true 表示连接保持、而且缓冲区里已经有下一个请求 */
        private boolean respond(RawRequest req, CompletableFuture<Adapter.RawResponse> future) throws IOException {
            Adapter.RawResponse resp;
            try {
                resp = future.join();
            } catch (CompletionException | CancellationException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                resp = new Adapter.RawResponse(500, "text/plain", cause.toString().getBytes(StandardCharsets.UTF_8));
            }
            boolean keepAlive = req.keepAlive() && running;
            writeFully(encode(resp, keepAlive));
            if (!keepAlive) {
                close(w);
                return false;
            }
            if (!w.readBuffer.hasRemaining()) {                    // 没有流水线里的下一个请求
                poller.readAgain(w);
                return false;
            }
            return true;
        }

        /** 非阻塞写可能只写出一部分；内核缓冲区满时临时用一个 Selector 等它可写（Tomcat 早期的 NioSelectorPool 也是这样） */
        private void writeFully(ByteBuffer out) throws IOException {
            while (out.hasRemaining()) {
                if (w.channel.write(out) == 0) {
                    try (Selector s = Selector.open()) {
                        w.channel.register(s, SelectionKey.OP_WRITE);
                        if (s.select(TimeUnit.SECONDS.toMillis(20)) == 0) throw new SocketTimeoutException("写超时");
                    }
                }
            }
        }
    }

    private static ByteBuffer encode(Adapter.RawResponse r, boolean keepAlive) {
        String head = "HTTP/1.1 " + r.status() + " " + reason(r.status()) + "\r\n"
                + "Content-Type: " + r.contentType() + "\r\n"
                + "Content-Length: " + r.body().length + "\r\n"
                + "Connection: " + (keepAlive ? "keep-alive" : "close") + "\r\n\r\n";
        byte[] h = head.getBytes(StandardCharsets.ISO_8859_1);
        return ByteBuffer.allocate(h.length + r.body().length).put(h).put(r.body()).flip();
    }

    private static String reason(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 431 -> "Request Header Fields Too Large";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }
}
