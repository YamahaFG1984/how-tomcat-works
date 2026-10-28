package modern.ch13.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch13.core.StandardEngine;
import modern.ch13.core.StandardHost;

/**
 * 第 13 章应用 2：以 Engine 为顶层容器。原书只有一个 localhost；
 * 这里再加一个虚拟主机 www.example.com（别名 example.com），演示按 Host 请求头分派。
 *
 * 运行：./run.sh ch13.startup.Bootstrap2
 *   curl http://localhost:8080/app1/Primitive
 *   curl -H 'Host: www.example.com' http://localhost:8080/Hello
 */
public final class Bootstrap2 {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var localhost = new StandardHost("localhost");
        localhost.addChild(Apps.context("/app1", "webapps/app1", "Primitive", "Modern"));

        var example = new StandardHost("www.example.com");
        example.addAlias("example.com");
        example.addChild(Apps.context("", "myApp", "Hello", "Mapping"));     // 这个主机的 ROOT 应用

        var engine = new StandardEngine();
        engine.addChild(localhost);
        engine.addChild(example);
        engine.setDefaultHost("localhost");

        connector.setContainer(engine);
        connector.initialize();
        connector.start();
        engine.start();

        System.in.read();
        engine.stop();
        connector.stop();
    }
}
