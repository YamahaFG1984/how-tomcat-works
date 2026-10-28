package modern.ch28.leak;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 让 Tomcat 自己发现第 8 章「重载」时的泄漏：把 LeakyServlet 放进 WEB-INF/classes，
 * 请求一次之后调用 context.reload()。WebappClassLoader 停止时会扫描所有线程，
 * 对残留的 ThreadLocal 和应用启动的线程打出 SEVERE 日志（并尽量清理）。
 *
 * ThreadLocal 检测要读 Thread 的私有字段，Java 9+ 需要 --add-opens（catalina.sh 默认会加）：
 *   JAVA_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED" ./run.sh ch28.leak.EmbeddedLeakDetection
 */
public final class EmbeddedLeakDetection {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path docBase = Files.createTempDirectory("ch28-leakapp");
        Path classes = Path.of(LeakyServlet.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path target = Files.createDirectories(docBase.resolve("WEB-INF/classes/modern/ch28/leak"));
        for (String name : new String[] {"LeakyServlet.class", "LeakyServlet$Buffer.class"}) {
            Files.copy(classes.resolve("modern/ch28/leak").resolve(name), target.resolve(name));
        }

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch28leak").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context ctx = tomcat.addContext("/leaky", docBase.toString());
        Tomcat.addServlet(ctx, "leaky", "modern.ch28.leak.LeakyServlet").setLoadOnStartup(1);   // 按类名，由 WebappClassLoader 加载
        ctx.addServletMappingDecoded("/", "leaky");
        tomcat.start();

        var client = HttpClient.newHttpClient();
        for (int i = 0; i < 3; i++) {                           // 多发几次，尽量落到不同的容器线程上
            System.out.println("响应：" + client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/leaky/")).build(),
                    HttpResponse.BodyHandlers.ofString()).body().trim());
        }

        System.out.println("\n=== 调用 context.reload()，观察 Tomcat 的泄漏检测日志 ===");
        ctx.reload();
        Thread.sleep(500);
        tomcat.stop();
        tomcat.destroy();
    }
}
