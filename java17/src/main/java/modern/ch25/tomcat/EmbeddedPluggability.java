package modern.ch25.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Constants;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * 让 Tomcat 自己做 Servlet 3.0 的「可插拔性」扫描。启动前在临时目录里拼出一个 Web 应用：
 *
 *   WEB-INF/web.xml                   只有 display-name；metadata-complete 由第二个参数决定
 *   WEB-INF/classes/…/webapp/*.class  AnnotatedServlet（@WebServlet）、ReportModule（实现 PluginModule）
 *   WEB-INF/lib/plugin.jar
 *       META-INF/services/jakarta.servlet.ServletContainerInitializer  → PluginInitializer
 *       META-INF/web-fragment.xml                                      → 声明 FragmentServlet
 *       modern/ch25/plugin/*.class
 *
 * 运行：./run.sh ch25.tomcat.EmbeddedPluggability [端口] [metadataComplete=false]
 */
public final class EmbeddedPluggability {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        boolean metadataComplete = args.length > 1 && Boolean.parseBoolean(args[1]);
        Path app = buildWebapp(metadataComplete);

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch25").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context ctx = tomcat.addContext("/app", app.toString());
        var config = new ContextConfig();                      // 第 15 章的老朋友：解析 web.xml、扫描注解、运行 SCI
        config.setDefaultWebXml(Constants.NoDefaultWebXml);
        ctx.addLifecycleListener(config);

        tomcat.start();
        System.out.println("metadata-complete = " + metadataComplete + "，Context 里的 Servlet：");
        for (Container w : ctx.findChildren()) {
            System.out.println("  " + w.getName());
        }
        System.out.println("试试：curl http://localhost:" + port + "/app/annotated   /app/fragment   /app/report");
        tomcat.getServer().await();
    }

    private static Path buildWebapp(boolean metadataComplete) throws IOException {
        Path classes = Path.of(EmbeddedPluggability.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        Path app = Files.createTempDirectory("ch25-webapp");
        Path webInf = Files.createDirectories(app.resolve("WEB-INF"));

        Files.writeString(webInf.resolve("web.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.1" metadata-complete="%s">
                  <display-name>ch25 pluggability demo</display-name>
                </web-app>
                """.formatted(metadataComplete));

        // WEB-INF/classes：应用自己的类
        copyPackage(classes, "modern/ch25/webapp", webInf.resolve("classes"));

        // WEB-INF/lib/plugin.jar：第三方库自带 SCI 和 web-fragment.xml
        Path lib = Files.createDirectories(webInf.resolve("lib"));
        try (var jar = new JarOutputStream(Files.newOutputStream(lib.resolve("plugin.jar")))) {
            put(jar, "META-INF/services/jakarta.servlet.ServletContainerInitializer",
                    "modern.ch25.plugin.PluginInitializer\n");
            put(jar, "META-INF/web-fragment.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <web-fragment xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.1">
                      <name>plugin</name>
                      <servlet>
                        <servlet-name>fragment</servlet-name>
                        <servlet-class>modern.ch25.plugin.FragmentServlet</servlet-class>
                      </servlet>
                      <servlet-mapping>
                        <servlet-name>fragment</servlet-name>
                        <url-pattern>/fragment</url-pattern>
                      </servlet-mapping>
                    </web-fragment>
                    """);
            try (Stream<Path> files = Files.list(classes.resolve("modern/ch25/plugin"))) {
                for (Path f : files.toList()) {
                    jar.putNextEntry(new JarEntry("modern/ch25/plugin/" + f.getFileName()));
                    Files.copy(f, jar);
                    jar.closeEntry();
                }
            }
        }
        return app;
    }

    private static void copyPackage(Path classes, String pkgPath, Path target) throws IOException {
        Path dir = Files.createDirectories(target.resolve(pkgPath));
        try (Stream<Path> files = Files.list(classes.resolve(pkgPath))) {
            for (Path f : files.toList()) Files.copy(f, dir.resolve(f.getFileName()));
        }
    }

    private static void put(JarOutputStream jar, String name, String content) throws IOException {
        jar.putNextEntry(new JarEntry(name));
        OutputStream out = jar;
        out.write(content.getBytes(StandardCharsets.UTF_8));
        jar.closeEntry();
    }
}
