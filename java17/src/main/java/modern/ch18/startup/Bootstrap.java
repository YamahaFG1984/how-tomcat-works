package modern.ch18.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch13.core.StandardEngine;
import modern.ch13.core.StandardHost;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * 第 18 章：Host 上不再手写任何 Context，全部交给 HostConfig 从 appBase 里发现并部署。
 * 启动前在 target/ch18-webapps 里准备三种形式的应用：
 *   app1/       目录（复制自 webapps/app1）
 *   app2.war    WAR 包（把 webapps/app1 打包）
 *   hello.xml   描述符（docBase 指向 myApp）
 *
 * 运行：./run.sh ch18.startup.Bootstrap [端口]
 * 试试：在运行中把 target/ch18-webapps/app1 复制成 app3，几秒后访问 /app3/Primitive；再删掉 app3。
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path appBase = prepareAppBase();

        var host = new StandardHost("localhost");
        host.setAppBase(appBase.toString());
        var deployer = new StandardHostDeployer(host);
        var hostConfig = new HostConfig(deployer, appBase);
        hostConfig.setCheckInterval(2);                     // 原书默认 15 秒
        host.addLifecycleListener(hostConfig);              // 原书 HostRuleSet 里自动加的监听器

        var engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("localhost");

        var connector = new HttpConnector(port);
        connector.setContainer(engine);
        connector.initialize();
        connector.start();
        engine.start();
        System.out.println("已部署的应用：" + deployer.findDeployedApps());

        System.in.read();
        engine.stop();
        connector.stop();
    }

    public static Path prepareAppBase() throws IOException {
        Path appBase = Path.of("target", "ch18-webapps").toAbsolutePath();
        deleteRecursively(appBase);
        Files.createDirectories(appBase);
        copyRecursively(Path.of("webapps/app1"), appBase.resolve("app1"));
        jar(Path.of("webapps/app1"), appBase.resolve("app2.war"));
        Files.writeString(appBase.resolve("hello.xml"),
                "<Context docBase=\"" + Path.of("myApp").toAbsolutePath() + "\"/>\n");
        return appBase;
    }

    private static void jar(Path dir, Path war) throws IOException {
        try (var out = new JarOutputStream(Files.newOutputStream(war)); Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                out.putNextEntry(new JarEntry(dir.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, out);
                out.closeEntry();
            }
        }
    }

    public static void copyRecursively(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path source : files.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }

    static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
