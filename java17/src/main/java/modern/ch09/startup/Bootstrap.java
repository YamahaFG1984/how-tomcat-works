package modern.ch09.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.SimpleWrapper;
import modern.ch08.loader.WebappLoader;
import modern.ch09.core.SessionWrapperValve;
import modern.ch09.core.SimpleContext;
import modern.ch09.session.StandardManager;

import java.nio.file.Path;

/**
 * 第 9 章：Context 带一个 StandardManager，运行 SessionServlet。
 * 运行：./run.sh ch09.startup.Bootstrap    访问 http://localhost:8080/myApp/Session?value=hello
 * 按回车停止时会话会保存到 work/SESSIONS.ser，再次启动后自动恢复。
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var wrapper = new SimpleWrapper();
        wrapper.setName("Session");
        wrapper.setServletClass("SessionServlet");
        wrapper.getPipeline().setBasic(new SessionWrapperValve());

        var context = new SimpleContext();
        context.setPath("/myApp");
        context.setDocBase("myApp");
        context.addChild(wrapper);
        // 上下文路径是 /myApp，会话 Cookie 的 Path 也是 /myApp，所以 URL 必须以 /myApp 开头
        context.addServletMapping("/myApp/Session", "Session");
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));

        var manager = new StandardManager(Path.of("work", "SESSIONS.ser"));
        manager.setMaxInactiveInterval(args.length > 1 ? Integer.parseInt(args[1]) : 30 * 60);
        manager.setCheckInterval(5);
        context.setManager(manager);

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        context.start();

        System.in.read();
        context.stop();
        connector.stop();
    }
}
