package modern.ch08.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.startup.Constants;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用 Tomcat 11 部署 myApp，并打开 reloadable：Tomcat 会用它自己的 WebappLoader / WebappClassLoader，
 * 由后台线程（第 12 章的 backgroundProcess）定期检查类是否被修改。
 *
 * 运行：./run.sh ch08.tomcat.EmbeddedReload   访问 http://localhost:8080/myApp/Hello
 */
public final class EmbeddedReload {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path myApp = Path.of(System.getProperty("user.dir"), "myApp").toAbsolutePath();

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch08").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.getEngine().setBackgroundProcessorDelay(3);       // 后台检查间隔：3 秒（默认 10 秒）

        Context context = tomcat.addContext("/myApp", myApp.toString());
        var config = new ContextConfig();                         // 读取 WEB-INF/web.xml（第 15 章）
        config.setDefaultWebXml(Constants.NoDefaultWebXml);       // 不加载 Tomcat 的全局默认 web.xml
        context.addLifecycleListener(config);
        context.setReloadable(true);

        tomcat.start();
        var loader = (WebappClassLoaderBase) context.getLoader().getClassLoader();
        System.out.println("Tomcat 11 WebappClassLoader: " + loader.getClass().getName());
        System.out.println("  delegate = " + loader.getDelegate());
        for (var url : loader.getURLs()) {
            System.out.println("  repository: " + url);
        }
        System.out.println("访问 http://localhost:" + port + "/myApp/Hello");
        tomcat.getServer().await();
    }
}
