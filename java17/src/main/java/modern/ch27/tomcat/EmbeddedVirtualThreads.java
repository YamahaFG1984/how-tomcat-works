package modern.ch27.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Tomcat 11 用虚拟线程处理请求：连接器属性 useVirtualThreads="true"。
 * Servlet 代码一行不改，仍然是同步阻塞的写法。第二个参数为 false 时改用 maxThreads=20 的平台线程池对比。
 *   /sleep?ms=2000   阻塞 2 秒，返回处理线程
 *
 * 运行（需要 JDK 21+）：./run.sh ch27.tomcat.EmbeddedVirtualThreads [端口] [true|false]
 *       ./run.sh ch24.LoadClient http://localhost:8080/sleep 200
 */
public final class EmbeddedVirtualThreads {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        boolean virtual = args.length < 2 || Boolean.parseBoolean(args[1]);
        if (virtual && Runtime.version().feature() < 21) {
            // 否则连接器启动时抛 UnsupportedOperationException
            System.err.println("useVirtualThreads 需要 JDK 21+，当前是 " + Runtime.version() + "；可以加参数 false 使用平台线程");
            System.exit(1);
        }

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch27").toString());
        tomcat.setPort(port);
        Connector connector = tomcat.getConnector();
        connector.setProperty("address", "127.0.0.1");
        if (virtual) {
            connector.setProperty("useVirtualThreads", "true");   // 每个请求一个新的虚拟线程，没有池
        } else {
            connector.setProperty("maxThreads", "20");            // 对照组：20 个平台线程
        }

        Context ctx = tomcat.addContext("", Files.createTempDirectory("ch27-docbase").toString());
        Tomcat.addServlet(ctx, "sleep", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                String ms = req.getParameter("ms");
                try {
                    Thread.sleep(ms == null ? 2000 : Long.parseLong(ms));   // 普通的阻塞调用
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().println(Thread.currentThread());
            }
        });
        ctx.addServletMappingDecoded("/", "sleep");

        tomcat.start();
        System.out.println((virtual ? "虚拟线程" : "平台线程 maxThreads=20") + "：curl http://localhost:" + port + "/sleep?ms=100");
        tomcat.getServer().await();
    }
}
