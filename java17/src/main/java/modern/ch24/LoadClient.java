package modern.ch24;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * 同时发出 N 个请求，统计总耗时和每个请求的耗时分布。
 * 运行：./run.sh ch24.LoadClient http://localhost:8080/sync 20
 */
public final class LoadClient {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://localhost:8080/sync";
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 20;

        // 每个请求用独立连接，避免 HTTP 客户端自己的连接池限制并发
        var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build();
        long t0 = System.nanoTime();
        List<CompletableFuture<long[]>> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long start = System.nanoTime();
            all.add(client.sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).build(),
                            HttpResponse.BodyHandlers.discarding())
                    .thenApply(r -> new long[] {r.statusCode(), (System.nanoTime() - start) / 1_000_000})
                    .exceptionally(e -> new long[] {-1, (System.nanoTime() - start) / 1_000_000}));
        }
        Map<Long, Integer> byStatus = new TreeMap<>();
        Map<Long, Integer> bySecond = new TreeMap<>();                 // 按完成时间取整到秒
        for (var f : all) {
            long[] r = f.join();
            byStatus.merge(r[0], 1, Integer::sum);
            bySecond.merge(Math.round(r[1] / 1000.0), 1, Integer::sum);
        }
        System.out.printf("%s × %d：总耗时 %.1f 秒，状态码 %s，完成时间分布（秒→个数）%s%n",
                url, n, (System.nanoTime() - t0) / 1e9, byStatus, bySecond);
    }
}
