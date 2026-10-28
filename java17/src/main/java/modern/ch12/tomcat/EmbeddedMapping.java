package modern.ch12.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tomcat 11 的映射与后台处理：
 *  - 同一个 MappingServlet 映射到精确、前缀、扩展名、默认四种模式，观察 HttpServletMapping；
 *  - backgroundProcessorDelay = 1，每秒 Context 会收到一次 "periodic" 生命周期事件。
 *
 * 运行：./run.sh ch12.tomcat.EmbeddedMapping   访问 /myApp/exact、/myApp/modern/a/b、/myApp/x.hello、/myApp/
 */
public final class EmbeddedMapping {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch12").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.getEngine().setBackgroundProcessorDelay(1);

        Context context = tomcat.addContext("/myApp", Path.of("myApp").toAbsolutePath().toString());
        Tomcat.addServlet(context, "Mapping", "MappingServlet");
        for (String pattern : new String[] {"/exact", "/modern/*", "*.hello", "/", ""}) {
            context.addServletMappingDecoded(pattern, "Mapping");          // "" 表示上下文根
        }

        var periodic = new AtomicInteger();
        context.addLifecycleListener(event -> {
            if (Lifecycle.PERIODIC_EVENT.equals(event.getType()) && periodic.incrementAndGet() <= 3) {
                System.out.println("periodic 事件 #" + periodic.get() + "，线程 " + Thread.currentThread().getName());
            }
        });

        tomcat.start();
        System.out.println("Tomcat 11 已启动：http://localhost:" + port + "/myApp/modern/a/b");
        tomcat.getServer().await();
    }
}
