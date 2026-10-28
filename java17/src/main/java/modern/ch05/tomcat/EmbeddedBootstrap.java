package modern.ch05.tomcat;

import jakarta.servlet.Servlet;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用真正的 Tomcat 11（嵌入式）搭出和 Bootstrap2 一样的结构：
 * 一个 Context、两个 Wrapper、两个附加阀。Context/Wrapper/Pipeline/Valve 这些概念在 20 年后依然是 Tomcat 的骨架。
 *
 * 运行：./run.sh ch05.tomcat.EmbeddedBootstrap   访问 /Primitive 或 /Modern
 */
public final class EmbeddedBootstrap {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path webroot = Path.of(System.getProperty("user.dir"), "webroot");
        var loader = new URLClassLoader(new URL[] {webroot.toUri().toURL()}, EmbeddedBootstrap.class.getClassLoader());

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch05").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context context = tomcat.addContext("", null);          // 上下文路径 ""，没有文档根目录

        Wrapper primitive = Tomcat.addServlet(context, "Primitive", newServlet(loader, "PrimitiveServlet"));
        Wrapper modern = Tomcat.addServlet(context, "Modern", newServlet(loader, "ModernServlet"));
        context.addServletMappingDecoded("/Primitive", primitive.getName());
        context.addServletMappingDecoded("/Modern", modern.getName());

        context.getPipeline().addValve(new HeaderLoggerValve());
        context.getPipeline().addValve(new ClientIPLoggerValve());

        tomcat.start();
        System.out.println("Tomcat 11 已启动：http://localhost:" + port + "/Primitive");
        tomcat.getServer().await();
    }

    private static Servlet newServlet(ClassLoader loader, String className) throws ReflectiveOperationException {
        return (Servlet) loader.loadClass(className).getDeclaredConstructor().newInstance();
    }
}
