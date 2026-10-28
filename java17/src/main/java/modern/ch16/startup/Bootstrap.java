package modern.ch16.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.LifecycleException;
import modern.ch06.core.SimpleWrapper;
import modern.ch08.loader.WebappLoader;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardEngine;
import modern.ch13.core.StandardHost;
import modern.ch14.core.StandardServer;
import modern.ch14.core.StandardService;

/**
 * 第 14 章的服务器加上 Catalina 风格的关闭钩子：
 *  - 用 Stopper 正常关闭：await() 返回 → stop() → 移除钩子（避免退出时再 stop 一次）；
 *  - 用 Ctrl+C 或 kill（SIGTERM）关闭：await() 不会返回，但关闭钩子会调用 server.stop()。
 *
 * 运行：./run.sh ch16.startup.Bootstrap [http端口] [关闭端口]
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        int httpPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int shutdownPort = args.length > 1 ? Integer.parseInt(args[1]) : 8005;

        StandardServer server = createServer(httpPort, shutdownPort);
        Thread hook = new Thread(() -> {                  // 对应原书 Catalina.CatalinaShutdownHook
            System.out.println("CatalinaShutdownHook: JVM 正在退出，停止服务器");
            try {
                server.stop();
            } catch (LifecycleException e) {
                System.out.println("Catalina.stop: " + e);
            }
        }, "CatalinaShutdownHook");

        server.initialize();
        server.start();
        Runtime.getRuntime().addShutdownHook(hook);
        System.out.println("pid=" + ProcessHandle.current().pid() + "，可以用 kill 或 Ctrl+C 试试");

        server.await();                                   // 收到 SHUTDOWN 才返回
        Runtime.getRuntime().removeShutdownHook(hook);    // 正常关闭路径：钩子不再需要
        server.stop();
        System.out.println("通过关闭端口正常关闭");
    }

    static StandardServer createServer(int httpPort, int shutdownPort) {
        var context = new StandardContext();
        context.setPath("/app1");
        context.setDocBase("webapps/app1");
        var wrapper = new SimpleWrapper();
        wrapper.setName("Primitive");
        wrapper.setServletClass("PrimitiveServlet");
        context.addChild(wrapper);
        context.addServletMapping("/Primitive", "Primitive");
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));

        var host = new StandardHost("localhost");
        host.addChild(context);
        var engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("localhost");

        var service = new StandardService();
        service.setName("Catalina");
        service.addConnector(new HttpConnector(httpPort));
        service.setContainer(engine);

        var server = new StandardServer();
        server.setPort(shutdownPort);
        server.addService(service);
        return server;
    }
}
