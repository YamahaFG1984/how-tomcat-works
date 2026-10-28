package modern.ch23.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Tomcat 11 的 NIO 连接器：
 *   /        返回处理线程名
 *   /stack   打印从 NioEndpoint 到 Servlet 的完整调用栈（只保留 org.apache 的帧）
 *   /slow    睡 2 秒
 * 启动时列出所有 http-nio-* 线程，maxThreads 故意设为 4，方便和本章的迷你连接器、SlowClients 对照。
 *
 * 运行：./run.sh ch23.tomcat.EmbeddedNio
 */
public final class EmbeddedNio {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch23").toString());
        tomcat.setPort(port);

        Connector connector = tomcat.getConnector();                       // 默认协议 HTTP/1.1 = Http11NioProtocol
        connector.setProperty("address", "127.0.0.1");
        connector.setProperty("maxThreads", "4");                          // Worker 线程池上限
        connector.setProperty("minSpareThreads", "4");
        connector.setProperty("maxConnections", "10000");                  // Acceptor 的 LimitLatch
        connector.setProperty("acceptCount", "100");                       // 操作系统 backlog
        connector.setProperty("connectionTimeout", "20000");

        Context ctx = tomcat.addContext("", Files.createTempDirectory("ch23-docbase").toString());
        Tomcat.addServlet(ctx, "probe", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws IOException {
                if (req.getRequestURI().equals("/slow")) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                resp.setContentType("text/plain;charset=UTF-8");
                var out = resp.getWriter();
                out.println("Hello from " + Thread.currentThread().getName());
                if (req.getRequestURI().equals("/stack")) {
                    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                    for (int i = stack.length - 1; i >= 0; i--) {                // 从栈底往上：先发生的在前
                        String cls = stack[i].getClassName();
                        if (cls.startsWith("org.apache") || cls.startsWith("jakarta.servlet") || cls.startsWith("modern")) {
                            out.printf("  %s.%s%n", cls.replaceFirst("^org\\.apache\\.", "o.a."), stack[i].getMethodName());
                        }
                    }
                }
            }
        });
        ctx.addServletMappingDecoded("/", "probe");

        tomcat.start();
        System.out.println("协议处理器：" + connector.getProtocolHandler().getClass().getName());
        System.out.println("连接器线程：" + Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getName).filter(n -> n.startsWith("http-nio")).sorted()
                .collect(Collectors.joining(", ")));
        System.out.println("Tomcat 11 已启动：curl http://localhost:" + port + "/stack");
        tomcat.getServer().await();
    }
}
