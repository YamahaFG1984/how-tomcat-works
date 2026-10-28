package modern.ch18.tomcat;

import modern.ch18.startup.Bootstrap;
import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.startup.HostConfig;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tomcat 11 的 HostConfig：嵌入式 Tomcat 默认不自动部署，这里手动给 Host 加上 HostConfig，
 * 让它扫描 appBase（目录和 WAR）以及 conf/Catalina/localhost/*.xml（描述符），并开启自动部署。
 *
 * 运行：./run.sh ch18.tomcat.EmbeddedHostConfig [端口]
 */
public final class EmbeddedHostConfig {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path base = Files.createTempDirectory("tomcat-ch18");
        Path appBase = base.resolve("webapps");
        Files.createDirectories(appBase);
        Bootstrap.copyRecursively(Path.of("webapps/app1"), appBase.resolve("app1"));          // 目录
        Files.copy(Path.of("target/ch18-webapps/app2.war"), appBase.resolve("app2.war"));     // WAR（由迷你版准备）
        var tomcat = new Tomcat();
        tomcat.setBaseDir(base.toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.getEngine().setBackgroundProcessorDelay(1);   // 每秒发一次 periodic 事件 → HostConfig.check()

        Host host = tomcat.getHost();
        host.setAppBase(appBase.toString());
        host.setAutoDeploy(true);
        host.addLifecycleListener(new HostConfig());

        // Tomcat 5 起描述符不放在 appBase，而是 $CATALINA_BASE/conf/<Engine 名>/<Host 名>/。
        // 独立部署时 Engine 叫 Catalina；嵌入式 Tomcat 的 Engine 叫 "Tomcat"
        Path xmlBase = base.resolve("conf").resolve(tomcat.getEngine().getName()).resolve(host.getName());
        Files.createDirectories(xmlBase);
        Files.writeString(xmlBase.resolve("hello.xml"),
                "<Context docBase=\"" + Path.of("myApp").toAbsolutePath() + "\"/>\n");

        tomcat.start();
        System.out.print("Tomcat 11 已部署：");
        for (Container child : host.findChildren()) {
            System.out.print(" [" + child.getName() + "]");
        }
        System.out.println("\nappBase = " + appBase);
        tomcat.getServer().await();
    }
}
