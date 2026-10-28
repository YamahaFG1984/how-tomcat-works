package modern.ch26.tomcat;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/**
 * 用 HTTP/2 同时发 N 个请求：它们复用同一个 TCP 连接，在上面并行地跑 N 个「流」。
 * 为了连上自签名证书，这里信任所有证书——<strong>只能用于本地演示</strong>。
 *
 * 运行：./run.sh ch26.tomcat.Http2Client https://localhost:8443/slow 20
 */
public final class Http2Client {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "https://localhost:8443/slow";
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 20;

        var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).sslContext(trustAll()).build();
        // 先发一个请求把连接建起来，后面的请求才会复用它
        System.out.println(client.send(HttpRequest.newBuilder(URI.create(url + "?ms=0")).build(),
                HttpResponse.BodyHandlers.ofString()).body().trim());

        long t0 = System.nanoTime();
        List<CompletableFuture<HttpResponse<String>>> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            all.add(client.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString()));
        }
        Set<String> versions = new TreeSet<>();
        Set<String> ports = new TreeSet<>();
        for (var f : all) {
            var r = f.join();
            versions.add(r.version().toString());
            ports.add(r.body().replaceAll(".*客户端端口=(\\d+).*\\s*", "$1"));
        }
        System.out.printf("%d 个请求，总耗时 %.1f 秒，协议 %s，服务器看到的客户端端口 %s%n",
                n, (System.nanoTime() - t0) / 1e9, versions, ports);
    }

    private static SSLContext trustAll() throws Exception {
        TrustManager tm = new X509TrustManager() {
            @Override public void checkClientTrusted(X509Certificate[] c, String a) { }
            @Override public void checkServerTrusted(X509Certificate[] c, String a) { }
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] {tm}, new SecureRandom());
        return ctx;
    }
}
