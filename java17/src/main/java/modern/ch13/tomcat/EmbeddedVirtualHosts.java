package modern.ch13.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tomcat 11：一个 Engine，两个虚拟主机。
 *   localhost       → /app1（webapps/app1）
 *   www.example.com → ""（myApp 作为 ROOT 应用），别名 example.com
 *
 * 运行：./run.sh ch13.tomcat.EmbeddedVirtualHosts
 *   curl http://localhost:8080/app1/Primitive
 *   curl -H 'Host: example.com' http://localhost:8080/Hello
 */
public final class EmbeddedVirtualHosts {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch13").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        // 默认主机 localhost（tomcat.getHost() 会自动创建并设为 Engine 的 defaultHost）
        Context app1 = tomcat.addContext(tomcat.getHost(), "/app1", Path.of("webapps/app1").toAbsolutePath().toString());
        Tomcat.addServlet(app1, "Primitive", "PrimitiveServlet");
        app1.addServletMappingDecoded("/Primitive", "Primitive");

        // 第二个虚拟主机
        var example = new StandardHost();
        example.setName("www.example.com");
        example.addAlias("example.com");
        example.setAppBase(Files.createTempDirectory("example-appbase").toString());
        tomcat.getEngine().addChild(example);
        Context root = tomcat.addContext(example, "", Path.of("myApp").toAbsolutePath().toString());
        Tomcat.addServlet(root, "Hello", "HelloServlet");
        root.addServletMappingDecoded("/Hello", "Hello");

        tomcat.start();
        System.out.println("Tomcat 11 已启动，Engine 默认主机 = " + tomcat.getEngine().getDefaultHost());
        tomcat.getServer().await();
    }
}
