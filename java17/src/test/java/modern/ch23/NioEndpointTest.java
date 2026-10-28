package modern.ch23;

import modern.ch23.nio.Adapter;
import modern.ch23.nio.NioEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 第 23、24 章：迷你 NIO 连接器的流水线、慢客户端和异步 Adapter */
class NioEndpointTest {

    private NioEndpoint endpoint;
    private final List<Socket> sockets = new ArrayList<>();

    @BeforeEach
    void start() throws Exception {
        var timer = Executors.newSingleThreadScheduledExecutor();
        Adapter adapter = Adapter.async(req -> {
            if (req.uri().equals("/async")) {
                var f = new CompletableFuture<Adapter.RawResponse>();
                timer.schedule(() -> f.complete(text("async")), 300, TimeUnit.MILLISECONDS);
                return f;
            }
            return CompletableFuture.completedFuture(text("ok " + req.uri()));
        });
        endpoint = new NioEndpoint(0, 2, 1000, 5000, adapter);       // 只有 2 个 Worker
        endpoint.start();
    }

    @AfterEach
    void stop() throws Exception {
        for (Socket s : sockets) s.close();
        endpoint.stop();
    }

    private static Adapter.RawResponse text(String s) {
        return new Adapter.RawResponse(200, "text/plain", s.getBytes(StandardCharsets.UTF_8));
    }

    private Socket connect() throws Exception {
        var s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", endpoint.getLocalPort()), 2000);
        s.setSoTimeout(5000);
        sockets.add(s);
        return s;
    }

    @Test
    void pipelinedRequestsOnOneConnection() throws Exception {
        Socket s = connect();
        OutputStream out = s.getOutputStream();
        out.write(("GET /a HTTP/1.1\r\nHost: x\r\n\r\nGET /b HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        String all = new String(s.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(all.contains("ok /a") && all.contains("ok /b"), all);
    }

    @Test
    void slowClientsDoNotBlockWorkers() throws Exception {
        for (int i = 0; i < 50; i++) {                                  // 50 个只发半个请求头的连接，Worker 只有 2 个
            connect().getOutputStream().write("GET / HTTP/1.1\r\nHost: x\r\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        assertTrue(roundTrip("/normal").contains("ok /normal"));
    }

    @Test
    void asyncResponsesDoNotHoldWorkers() throws Exception {
        long t0 = System.nanoTime();
        List<CompletableFuture<String>> all = new ArrayList<>();
        for (int i = 0; i < 6; i++) {                                    // 6 个各需 300ms 的请求，Worker 只有 2 个
            all.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return roundTrip("/async");
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, Executors.newCachedThreadPool()));
        }
        for (var f : all) assertTrue(f.get().contains("async"));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(ms < 800, "同步的话至少要 900ms，实际 " + ms + "ms");
    }

    private String roundTrip(String path) throws Exception {
        Socket s = connect();
        s.getOutputStream().write(("GET " + path + " HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        InputStream in = s.getInputStream();
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void reportsConnectionCount() throws Exception {
        connect();
        Thread.sleep(200);
        assertEquals(1, endpoint.getConnections());
    }
}
