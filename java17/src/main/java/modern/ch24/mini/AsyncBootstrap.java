package modern.ch24.mini;

import modern.ch23.nio.Adapter;
import modern.ch23.nio.NioEndpoint;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 在第 23 章的迷你 NIO 连接器上演示「异步」的本质，只用 2 个 Worker 线程：
 *   /sync    在 Worker 线程里睡 2 秒（占着线程等）
 *   /async   返回一个 2 秒后由定时器完成的 Future（Worker 线程立刻归还）
 *
 * 运行：./run.sh ch24.mini.AsyncBootstrap [端口]
 *       ./run.sh ch24.LoadClient http://localhost:8080/sync 10
 *       ./run.sh ch24.LoadClient http://localhost:8080/async 10
 */
public final class AsyncBootstrap {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "timer");
            t.setDaemon(true);
            return t;
        });

        Adapter adapter = Adapter.async(req -> {
            String entered = Thread.currentThread().getName();
            if (req.uri().startsWith("/sync")) {
                sleep(2000);                                           // 同步：线程被占用 2 秒
                return CompletableFuture.completedFuture(text("sync：进入 " + entered + "，完成 " + Thread.currentThread().getName()));
            }
            if (req.uri().startsWith("/async")) {
                var future = new CompletableFuture<Adapter.RawResponse>();
                // 相当于 request.startAsync() 之后把 AsyncContext 交给别的线程：2 秒后由 timer 线程 complete()
                timer.schedule(() -> future.complete(text("async：进入 " + entered + "，完成 " + Thread.currentThread().getName())),
                        2, TimeUnit.SECONDS);
                return future;                                         // Worker 线程马上返回
            }
            return CompletableFuture.completedFuture(new Adapter.RawResponse(404, "text/plain", new byte[0]));
        });

        var endpoint = new NioEndpoint(port, 2, 10_000, 20_000, adapter);
        endpoint.start();
        System.out.println("迷你异步服务器：http://localhost:" + port + "/sync 与 /async，Worker 线程 2 个。按回车停止……");
        System.in.read();
        endpoint.stop();
    }

    private static Adapter.RawResponse text(String s) {
        return new Adapter.RawResponse(200, "text/plain;charset=UTF-8", (s + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
