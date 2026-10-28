package modern.ch16.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 嵌入式 Tomcat 11 不会自动注册关闭钩子（独立部署时由 org.apache.catalina.startup.Catalina 注册），
 * 所以要自己加：收到 SIGTERM / Ctrl+C 时依次 stop() 和 destroy()，让 Servlet 的 destroy、会话持久化都能执行。
 *
 * 运行：./run.sh ch16.tomcat.EmbeddedShutdownHook   然后 kill 它的 pid
 */
public final class EmbeddedShutdownHook {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch16").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context app1 = tomcat.addContext("/app1", Path.of("webapps/app1").toAbsolutePath().toString());
        Tomcat.addServlet(app1, "Primitive", "PrimitiveServlet");
        app1.addServletMappingDecoded("/Primitive", "Primitive");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("shutdown hook: 停止 Tomcat 11");
            try {
                tomcat.stop();
                tomcat.destroy();
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
            System.out.println("shutdown hook: Tomcat 11 已停止");
        }, "TomcatShutdownHook"));

        tomcat.start();
        System.out.println("Tomcat 11 已启动，pid=" + ProcessHandle.current().pid());
        tomcat.getServer().await();
    }
}
