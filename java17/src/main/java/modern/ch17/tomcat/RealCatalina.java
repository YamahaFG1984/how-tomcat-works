package modern.ch17.tomcat;

import org.apache.catalina.startup.Catalina;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 直接使用 Tomcat 11 的 org.apache.catalina.startup.Catalina：和原书讲的是同一个类。
 * 准备一个临时的 CATALINA_BASE（只有 conf/server.xml），然后 load() → start()。
 * start() 内部和原书一样：启动 Server → 注册关闭钩子 → await() → 移除钩子 → stop()。
 *
 * 运行：./run.sh ch17.tomcat.RealCatalina [http端口] [关闭端口]
 * 停止：./run.sh ch14.startup.Stopper [关闭端口]
 */
public final class RealCatalina {

    public static void main(String[] args) throws Exception {
        String port = args.length > 0 ? args[0] : "8080";
        String shutdownPort = args.length > 1 ? args[1] : "8005";

        Path base = Files.createTempDirectory("catalina-base");
        Files.createDirectories(base.resolve("conf"));
        Files.copy(Path.of("conf/tomcat11/server.xml"), base.resolve("conf/server.xml"), StandardCopyOption.REPLACE_EXISTING);

        System.setProperty("catalina.home", base.toString());
        System.setProperty("catalina.base", base.toString());
        System.setProperty("demo.port", port);
        System.setProperty("demo.shutdownPort", shutdownPort);
        System.setProperty("demo.appBase", Path.of("webapps").toAbsolutePath().toString());

        var catalina = new Catalina();
        catalina.setAwait(true);          // start() 结束前调用 server.await()
        catalina.load();                  // createStartDigester() 解析 conf/server.xml，server.init()
        System.out.println("Catalina 解析出的 Server: " + catalina.getServer()
                + "，关闭端口 " + catalina.getServer().getPort());
        catalina.start();                 // 阻塞，直到收到 SHUTDOWN
        System.out.println("Tomcat 11 Catalina 已退出");
    }
}
