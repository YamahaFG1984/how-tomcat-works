package modern.ch09.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Constants;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Path;

/**
 * Tomcat 11 的会话管理：给 /myApp 配一个 StandardManager。
 * baseDir 固定为 target/tomcat-ch09，这样停止时写入 work/.../SESSIONS.ser，下次启动能读回来。
 *
 * 运行：./run.sh ch09.tomcat.EmbeddedSession   访问 /myApp/Session?value=x，按回车停止后再次运行
 */
public final class EmbeddedSession {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path myApp = Path.of(System.getProperty("user.dir"), "myApp").toAbsolutePath();

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Path.of("target", "tomcat-ch09").toAbsolutePath().toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context context = tomcat.addContext("/myApp", myApp.toString());
        var config = new ContextConfig();
        config.setDefaultWebXml(Constants.NoDefaultWebXml);
        context.addLifecycleListener(config);
        context.setSessionTimeout(30);                    // 分钟，相当于 web.xml 的 <session-timeout>

        var manager = new StandardManager();
        manager.setPathname("SESSIONS.ser");              // 相对于 Context 的 work 目录
        context.setManager(manager);
        context.setUseHttpOnly(true);                     // 会话 Cookie 带 HttpOnly（默认就是 true）

        tomcat.start();
        System.out.println("Tomcat 11 已启动：http://localhost:" + port + "/myApp/Session?value=hello   按回车停止");
        System.in.read();
        tomcat.stop();                                    // StandardManager.stopInternal() → unload() 保存会话
        tomcat.destroy();
    }
}
