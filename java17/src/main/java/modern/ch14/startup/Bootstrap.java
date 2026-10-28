package modern.ch14.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.SimpleWrapper;
import modern.ch08.loader.WebappLoader;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardEngine;
import modern.ch13.core.StandardHost;
import modern.ch14.core.StandardServer;
import modern.ch14.core.StandardService;

/**
 * 第 14 章：Server → Service → (Engine + 两个连接器)。启动后阻塞在 await()，
 * 用 Stopper 向关闭端口发送 SHUTDOWN 才会优雅地停止所有组件。
 *
 * 运行：./run.sh ch14.startup.Bootstrap [http端口] [第二个http端口] [关闭端口]   默认 8080 8081 8005
 * 停止：./run.sh ch14.startup.Stopper [关闭端口]
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        int port1 = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int port2 = args.length > 1 ? Integer.parseInt(args[1]) : 8081;
        int shutdownPort = args.length > 2 ? Integer.parseInt(args[2]) : 8005;

        var context = new StandardContext();
        context.setPath("/app1");
        context.setDocBase("webapps/app1");
        for (String name : new String[] {"Primitive", "Modern"}) {
            var wrapper = new SimpleWrapper();
            wrapper.setName(name);
            wrapper.setServletClass(name + "Servlet");
            context.addChild(wrapper);
            context.addServletMapping("/" + name, name);
        }
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));

        var host = new StandardHost("localhost");
        host.addChild(context);
        var engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("localhost");

        var service = new StandardService();
        service.setName("Stand-alone Service");
        service.addConnector(new HttpConnector(port1));
        service.addConnector(new HttpConnector(port2));     // 原书说「一个 Service 可以有多个连接器」，这里真的加两个
        service.setContainer(engine);                       // 两个连接器共享同一个 Engine

        var server = new StandardServer();
        server.setPort(shutdownPort);
        server.addService(service);

        server.initialize();
        server.start();
        server.await();          // 阻塞，直到收到 SHUTDOWN
        server.stop();
        System.out.println("服务器已优雅关闭");
    }
}
