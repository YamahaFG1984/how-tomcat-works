package modern.ch13.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch13.core.StandardHost;

/**
 * 第 13 章应用 1：以 Host 为顶层容器，部署 webapps/app1，上下文路径 /app1。
 * 运行：./run.sh ch13.startup.Bootstrap1     访问 http://localhost:8080/app1/Primitive
 */
public final class Bootstrap1 {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var host = new StandardHost("localhost");
        host.setAppBase("webapps");
        host.addChild(Apps.context("/app1", "webapps/app1", "Primitive", "Modern"));

        connector.setContainer(host);
        connector.initialize();
        connector.start();
        host.start();

        System.in.read();
        host.stop();
        connector.stop();
    }
}
