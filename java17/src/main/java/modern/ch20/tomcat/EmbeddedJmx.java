package modern.ch20.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Tomcat 11 的 JMX：Tomcat 启动时把 Server、Service、Connector、Engine、Host、Context、Manager、
 * 线程池……都注册成 MBean（domain 就是 Engine 的名字，嵌入式下为 "Tomcat"）。
 * 这里额外部署一个 /myadmin 应用（JmxAdminServlet），相当于原书的 MyAdmin。
 *
 * 运行：./run.sh ch20.tomcat.EmbeddedJmx
 *   curl 'http://localhost:8080/myadmin/admin?action=listAllContexts'
 *   也可以用 jconsole 连接这个进程，在 MBeans 页签里找到 Tomcat 域
 */
public final class EmbeddedJmx {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch20").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context app1 = tomcat.addContext("/app1", Path.of("webapps/app1").toAbsolutePath().toString());
        Tomcat.addServlet(app1, "Primitive", "PrimitiveServlet");
        app1.addServletMappingDecoded("/Primitive", "Primitive");

        Context admin = tomcat.addContext("/myadmin", Path.of("myApp").toAbsolutePath().toString());
        admin.setPrivileged(true);
        Tomcat.addServlet(admin, "myAdmin", "JmxAdminServlet");
        admin.addServletMappingDecoded("/admin", "myAdmin");

        tomcat.start();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String domain = tomcat.getEngine().getName();
        System.out.println("Tomcat 注册的 MBean 数量：" + mbs.queryNames(new ObjectName(domain + ":*"), null).size());
        for (String type : new String[] {"Server", "Service", "Engine", "Host", "Connector", "Manager", "ThreadPool"}) {
            mbs.queryNames(new ObjectName(domain + ":type=" + type + ",*"), null).stream().limit(2)
                    .forEach(n -> System.out.println("  " + n));
        }
        var serverName = new ObjectName(domain + ":type=Server");
        System.out.println("serverInfo = " + mbs.getAttribute(serverName, "serverInfo"));
        System.out.println("Tomcat 11 已启动：curl 'http://localhost:" + port + "/myadmin/admin?action=listAllContexts'");
        tomcat.getServer().await();
    }
}
