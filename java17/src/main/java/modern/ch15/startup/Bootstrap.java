package modern.ch15.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch05.core.Container;
import modern.ch08.loader.WebappLoader;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardHost;

/**
 * 第 15 章：不再手写 Wrapper，交给 ContextConfig 从 webapps/app1/WEB-INF/web.xml 读出来。
 * 运行：./run.sh ch15.startup.Bootstrap    访问 /app1/Primitive、/app1/Modern
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var context = new StandardContext();
        context.setPath("/app1");
        context.setDocBase("webapps/app1");
        context.addLifecycleListener(new ContextConfig());
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));

        var host = new StandardHost("localhost");
        host.setAppBase("webapps");
        host.addChild(context);

        connector.setContainer(host);
        connector.initialize();
        connector.start();
        host.start();

        System.out.println("ContextConfig 创建的 Wrapper：");
        for (Container child : context.findChildren()) {
            System.out.println("  " + child.getName());
        }
        System.in.read();
        host.stop();
        connector.stop();
    }
}
