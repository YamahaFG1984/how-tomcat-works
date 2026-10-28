package modern.ch28.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteAddrValve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 一个「准备上线」的嵌入式 Tomcat：本章的调优、监控、加固清单落到代码里。
 *   连接器   显式设置线程数、队列、超时、请求头大小上限
 *   访问日志  AccessLogValve，带处理耗时 %{ms}T
 *   错误页   ErrorReportValve 不显示异常详情和版本号
 *   管理端点  /admin/metrics 输出 Prometheus 格式的指标，RemoteAddrValve 只允许指定来源访问
 *
 * 运行：./run.sh ch28.tomcat.EmbeddedOps [端口] [允许访问 /admin 的 IP 正则]
 */
public final class EmbeddedOps {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String adminAllow = args.length > 1 ? args[1] : "127\\.0\\.0\\.1|0:0:0:0:0:0:0:1";
        Path base = Files.createTempDirectory("tomcat-ch28");

        var tomcat = new Tomcat();
        tomcat.setBaseDir(base.toString());
        tomcat.setPort(port);

        // ① 连接器：不要依赖默认值，写出来就是一份文档
        Connector c = tomcat.getConnector();
        c.setProperty("address", "127.0.0.1");
        c.setProperty("maxThreads", "50");
        c.setProperty("minSpareThreads", "10");
        c.setProperty("acceptCount", "100");
        c.setProperty("maxConnections", "2000");
        c.setProperty("connectionTimeout", "10000");      // 防慢速攻击：10 秒内必须发完请求行
        c.setProperty("keepAliveTimeout", "15000");
        c.setProperty("maxHttpHeaderSize", "8192");
        c.setProperty("maxParameterCount", "1000");        // 防 hash 碰撞类的参数洪水

        Host host = tomcat.getHost();

        // ② 访问日志：带上处理耗时，排查慢请求的第一手资料。
        //    注意 Tomcat 10 起 %D 的单位是微秒（和 httpd 一致），毫秒要写 %{ms}T
        var access = new AccessLogValve();
        access.setDirectory(base.resolve("logs").toString());
        access.setPrefix("access");
        access.setSuffix(".log");
        access.setPattern("%h %t \"%r\" %s %b %{ms}Tms \"%{User-Agent}i\"");
        access.setBuffered(false);
        host.getPipeline().addValve(access);

        // ③ 错误页：不泄露异常堆栈和 Tomcat 版本（StandardHost 发现已有这个阀，就不会再加默认的）
        var errorReport = new ErrorReportValve();
        errorReport.setShowReport(false);
        errorReport.setShowServerInfo(false);
        host.getPipeline().addValve(errorReport);

        Context app = tomcat.addContext("", Files.createTempDirectory("ch28-app").toString());
        Tomcat.addServlet(app, "hello", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                if (req.getRequestURI().equals("/boom")) throw new IllegalStateException("数据库密码是 hunter2");
                if (req.getRequestURI().equals("/slow")) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                req.getSession();                                 // 让 Manager 有活动会话可数
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().println("ok " + req.getRequestURI());
            }
        });
        app.addServletMappingDecoded("/", "hello");

        // ④ 管理端点：单独的 Context，只允许运维网段访问（Manager 应用也应该这样保护）
        Context admin = tomcat.addContext("/admin", Files.createTempDirectory("ch28-admin").toString());
        var ipFilter = new RemoteAddrValve();
        ipFilter.setAllow(adminAllow);
        admin.getPipeline().addValve(ipFilter);
        Tomcat.addServlet(admin, "metrics", new MetricsServlet(tomcat.getEngine().getName()));
        admin.addServletMappingDecoded("/metrics", "metrics");

        tomcat.start();
        System.out.println("访问日志：" + base.resolve("logs"));
        System.out.println("Tomcat 11 已启动：curl http://localhost:" + port + "/admin/metrics   （/admin 允许：" + adminAllow + "）");
        tomcat.getServer().await();
    }
}
