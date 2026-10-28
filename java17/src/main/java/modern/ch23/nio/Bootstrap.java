package modern.ch23.nio;

import java.nio.charset.StandardCharsets;

/**
 * 启动迷你 NIO 连接器。Adapter 直接用 lambda 写，相当于一个最小的「容器」。
 *   /       返回处理它的 Worker 线程名、当前连接数、JVM 线程总数
 *   /slow   睡 2 秒：演示 NIO 解决不了「业务本身慢」，那是第 24 章异步 Servlet 的事
 *
 * 运行：./run.sh ch23.nio.Bootstrap [端口] [worker 数] [最大连接数]     按回车停止
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int workers = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        int maxConnections = args.length > 2 ? Integer.parseInt(args[2]) : 10_000;

        NioEndpoint[] self = new NioEndpoint[1];
        Adapter adapter = req -> {
            if (req.uri().startsWith("/slow")) Thread.sleep(2000);
            if (!req.uri().equals("/") && !req.uri().startsWith("/slow")) {
                return new Adapter.RawResponse(404, "text/plain", "not found\n".getBytes(StandardCharsets.UTF_8));
            }
            String body = "Hello from " + Thread.currentThread().getName()
                    + "，当前连接 " + self[0].getConnections() + "，JVM 线程 " + Thread.activeCount() + "\n";
            return new Adapter.RawResponse(200, "text/plain;charset=UTF-8", body.getBytes(StandardCharsets.UTF_8));
        };
        var endpoint = self[0] = new NioEndpoint(port, workers, maxConnections, 20_000, adapter);
        endpoint.start();
        System.out.printf("迷你 NIO 连接器：http://localhost:%d/   worker=%d maxConnections=%d%n", port, workers, maxConnections);

        // 每 2 秒报告一次（有变化才打印）：连接数可以上万，线程数始终是 workers + 3 左右
        var monitor = new Thread(() -> {
            String last = "";
            while (true) {
                String now = "连接数 " + endpoint.getConnections() + "，JVM 线程 " + Thread.activeCount();
                if (!now.equals(last)) System.out.println("[monitor] " + now);
                last = now;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "monitor");
        monitor.setDaemon(true);
        monitor.start();

        System.out.println("按回车键停止……");
        System.in.read();
        endpoint.stop();
    }
}
