package modern.ch12.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.SimpleWrapper;
import modern.ch08.loader.WebappLoader;
import modern.ch09.core.SessionWrapperValve;
import modern.ch09.session.StandardManager;
import modern.ch12.core.ContainerBackgroundProcessor;
import modern.ch12.core.StandardContext;

import java.nio.file.Path;

/**
 * 第 12 章（原书本章没有示例程序）：把映射规则、重载暂停、共享后台线程组合到一起。
 * 运行：./run.sh ch12.startup.Bootstrap
 * 试试：/exact、/modern/a/b、/x.hello、/anything、/myApp/Session
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var context = new StandardContext();
        context.setPath("/myApp");
        context.setDocBase("myApp");
        addServlet(context, "Mapping", "MappingServlet", "/exact", "/modern/*", "*.hello", "/");
        SimpleWrapper session = addServlet(context, "Session", "SessionServlet", "/myApp/Session");
        session.getPipeline().setBasic(new SessionWrapperValve());

        // 载入器和管理器都不开自己的线程（reloadable=false、checkInterval=0），交给后台线程统一处理
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));
        context.setReloadable(true);
        var manager = new StandardManager(Path.of("work", "SESSIONS.ser"));
        manager.setCheckInterval(0);
        manager.setMaxInactiveInterval(args.length > 1 ? Integer.parseInt(args[1]) : 1800);
        context.setManager(manager);

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        context.start();
        var background = ContainerBackgroundProcessor.start(context, 1);   // backgroundProcessorDelay = 1 秒

        System.in.read();
        background.shutdownNow();
        context.stop();
        connector.stop();
    }

    private static SimpleWrapper addServlet(StandardContext context, String name, String servletClass, String... patterns) {
        var wrapper = new SimpleWrapper();
        wrapper.setName(name);
        wrapper.setServletClass(servletClass);
        context.addChild(wrapper);
        for (String pattern : patterns) {
            context.addServletMapping(pattern, name);
        }
        return wrapper;
    }
}
