package modern.ch08.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.SimpleWrapper;
import modern.ch08.core.SimpleContext;
import modern.ch08.loader.WebappLoader;

/**
 * 第 8 章：Context 使用 WebappLoader，从 myApp/WEB-INF/classes 加载 Servlet，并支持自动重载。
 *
 * 运行：./run.sh ch08.startup.Bootstrap   访问 /Hello、/Primitive、/Modern
 * 重载：修改 webroot/HelloServlet.java 里的 MESSAGE，执行 ./compile-servlets.sh，几秒后再访问 /Hello
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var context = new SimpleContext();
        context.setDocBase("myApp");
        for (String name : new String[] {"Primitive", "Modern", "Hello"}) {
            var wrapper = new SimpleWrapper();
            wrapper.setName(name);
            wrapper.setServletClass(name + "Servlet");
            context.addChild(wrapper);
            context.addServletMapping("/" + name, name);
        }

        var loader = new WebappLoader(context.getDocBase(), context::reload);
        loader.setReloadable(true);
        loader.setCheckInterval(3);              // 原书默认 15 秒，演示时改短一点
        context.setLoader(loader);

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        context.start();

        System.out.println("docBase: " + context.getDocBase());
        loader.findRepositories().forEach(r -> System.out.println("  repository: " + r));

        System.in.read();
        context.stop();
        connector.stop();
    }
}
