package modern.ch22;

import modern.ch22.mini.MiniDefaultServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 第 22 章：在真实的嵌入式 Tomcat 里测试迷你 DefaultServlet 的条件请求和范围请求 */
class MiniDefaultServletTest {

    static Tomcat tomcat;
    static String base;
    static final HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @BeforeAll
    static void start() throws Exception {
        Path dir = Files.createTempDirectory("ch22-test");
        Files.writeString(dir.resolve("a.txt"), "0123456789");
        Files.createDirectories(dir.resolve("docs"));
        Files.writeString(dir.resolve("docs/index.html"), "<h1>docs</h1>");
        tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("ch22-tomcat").toString());
        tomcat.setPort(0);                                      // 随机端口
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context ctx = tomcat.addContext("", dir.toString());
        Tomcat.addServlet(ctx, "mini", new MiniDefaultServlet(dir));
        ctx.addServletMappingDecoded("/mini/*", "mini");
        tomcat.start();
        base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/mini";
    }

    @AfterAll
    static void stop() throws LifecycleException {
        tomcat.stop();
        tomcat.destroy();
    }

    private static HttpResponse<String> get(String path, String... headers) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path));
        for (int i = 0; i < headers.length; i += 2) b.header(headers[i], headers[i + 1]);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void etagRoundTripGives304() throws Exception {
        var first = get("/a.txt");
        assertEquals(200, first.statusCode());
        String etag = first.headers().firstValue("ETag").orElseThrow();
        assertEquals(304, get("/a.txt", "If-None-Match", etag).statusCode());
    }

    @Test
    void rangeRequests() throws Exception {
        var r = get("/a.txt", "Range", "bytes=2-4");
        assertEquals(206, r.statusCode());
        assertEquals("234", r.body());
        assertEquals("bytes 2-4/10", r.headers().firstValue("Content-Range").orElseThrow());
        assertEquals("789", get("/a.txt", "Range", "bytes=-3").body());
        assertEquals(416, get("/a.txt", "Range", "bytes=50-").statusCode());
    }

    @Test
    void directoryRedirectsThenServesWelcomeFile() throws Exception {
        var r = get("/docs");
        assertEquals(302, r.statusCode());
        assertEquals("/mini/docs/", r.headers().firstValue("Location").orElseThrow());
        assertEquals("<h1>docs</h1>", get("/docs/").body());
    }
}
