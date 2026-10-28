package modern.ch15.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Constants;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 原书第 15 章应用的 Tomcat 11 版：只给 Context 加一个 ContextConfig 监听器，
 * 它用 Digester（WebRuleSet）解析 web.xml，自动创建 StandardWrapper。
 * 运行：./run.sh ch15.tomcat.EmbeddedContextConfig
 */
public final class EmbeddedContextConfig {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch15").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context context = tomcat.addContext("/app1", Path.of("webapps/app1").toAbsolutePath().toString());
        var config = new ContextConfig();
        config.setDefaultWebXml(Constants.NoDefaultWebXml);   // 不读 conf/web.xml（嵌入式没有 CATALINA_BASE/conf）
        context.addLifecycleListener(config);

        tomcat.start();
        System.out.println("ContextConfig 创建的 Wrapper：");
        for (Container child : context.findChildren()) {
            System.out.println("  " + child.getName() + " → " + child.getClass().getSimpleName());
        }
        System.out.println("访问 http://localhost:" + port + "/app1/Modern");
        tomcat.getServer().await();
    }
}
