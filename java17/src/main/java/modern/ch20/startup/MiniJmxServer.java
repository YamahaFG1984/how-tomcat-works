package modern.ch20.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch05.core.Container;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardEngine;
import modern.ch13.core.StandardHost;
import modern.ch18.startup.HostConfig;
import modern.ch18.startup.StandardHostDeployer;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * 让我们自己的迷你容器也能被 JMX 管理：启动第 18 章的服务器后，为每个 Context 注册一个 MXBean，
 * ObjectName 形如 Mini:type=Context,path=/app1。然后就可以用 JConsole 或者下面的代码停止、启动应用。
 *
 * 运行：./run.sh ch20.startup.MiniJmxServer [端口]
 */
public final class MiniJmxServer {

    public static void main(String[] args) throws Exception {
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path appBase = modern.ch18.startup.Bootstrap.prepareAppBase();

        var host = new StandardHost("localhost");
        host.addLifecycleListener(new HostConfig(new StandardHostDeployer(host), appBase));
        var engine = new StandardEngine();
        engine.addChild(host);
        engine.setDefaultHost("localhost");
        var connector = new HttpConnector(port);
        connector.setContainer(engine);
        connector.initialize();
        connector.start();
        engine.start();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (Container child : host.findChildren()) {
            var context = (StandardContext) child;
            var name = new ObjectName("Mini:type=Context,path=" + ObjectName.quote(context.getPath().isEmpty() ? "/" : context.getPath()));
            mbs.registerMBean(new ContextAdmin(context), name);
            System.out.println("注册 MBean " + name);
        }

        // 像管理程序那样，只通过 MBeanServer 操作，完全不碰 StandardContext 对象
        var app1 = new ObjectName("Mini:type=Context,path=" + ObjectName.quote("/app1"));
        System.out.println("app1 running = " + mbs.getAttribute(app1, "Running"));
        mbs.invoke(app1, "stop", null, null);
        System.out.println("通过 JMX 调用 stop 之后，app1 running = " + mbs.getAttribute(app1, "Running"));
        mbs.invoke(app1, "start", null, null);
        System.out.println("再调用 start，app1 running = " + mbs.getAttribute(app1, "Running"));
        System.out.println("pid=" + ProcessHandle.current().pid() + "，可以用 jconsole 连接后在 MBeans 页签找到 Mini 域；按回车退出");

        System.in.read();
        engine.stop();
        connector.stop();
    }
}
