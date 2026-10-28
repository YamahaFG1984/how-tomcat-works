package modern.ch17.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.LifecycleException;
import modern.ch08.loader.WebappLoader;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardEngine;
import modern.ch13.core.StandardHost;
import modern.ch14.core.StandardServer;
import modern.ch14.core.StandardService;
import modern.ch15.mini.MiniDigester;
import modern.ch15.startup.ContextConfig;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 迷你版 org.apache.catalina.startup.Catalina：
 *   process(args) 解析命令行 → start：用 Digester 解析 server.xml 搭出整棵组件树，启动并 await；
 *                             → stop：只解析 <Server> 拿到端口和命令，向关闭端口发送命令。
 */
public class Catalina {

    private Path configFile = Path.of("conf", "server.xml");
    private StandardServer server;
    private ClassLoader parentClassLoader = Catalina.class.getClassLoader();

    /** Digester 的 SetNext 规则会调用它：栈底的 Catalina.setServer(server) */
    public void setServer(StandardServer server) {
        this.server = server;
    }

    /** Bootstrap 通过反射调用，传入 shared 类加载器 */
    public void setParentClassLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    public void process(String[] args) throws Exception {
        String home = System.getProperty("catalina.home", System.getProperty("user.dir"));
        System.setProperty("catalina.home", home);
        System.setProperty("catalina.base", System.getProperty("catalina.base", home));

        String command = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-config" -> configFile = Path.of(args[++i]);
                case "start", "stop" -> command = args[i];
                default -> {
                    usage();
                    return;
                }
            }
        }
        if (command == null) {
            usage();
        } else if (command.equals("start")) {
            start();
        } else {
            stop();
        }
    }

    private static void usage() {
        System.out.println("usage: java modern.ch17.startup.Catalina [ -config {pathname} ] { start | stop }");
    }

    /* ---------------- start ---------------- */

    protected void start() throws Exception {
        MiniDigester digester = createStartDigester();
        digester.push(this);                                 // Catalina 压在栈底，<Server> 结束时调用 setServer
        digester.parse(resolve(configFile));
        System.out.println("Catalina: 从 " + configFile + " 创建了 Server，父类加载器 = " + parentClassLoader.getName());

        Thread shutdownHook = new Thread(() -> {             // 第 16 章的 CatalinaShutdownHook
            try {
                server.stop();
            } catch (LifecycleException e) {
                System.out.println("Catalina.stop: " + e);
            }
        }, "CatalinaShutdownHook");

        server.initialize();
        server.start();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        server.await();

        Runtime.getRuntime().removeShutdownHook(shutdownHook);   // 先移除钩子，避免 stop 被调用两次
        server.stop();
    }

    /** 对应原书 Listing 17.6 的 createStartDigester（规则和 server.xml 的结构一一对应） */
    protected MiniDigester createStartDigester() {
        var digester = new MiniDigester();

        digester.addObjectCreate("Server", StandardServer::new);
        digester.addSetProperties("Server");                              // port、shutdown
        digester.addSetNext("Server", "setServer");

        digester.addObjectCreate("Server/Service", StandardService::new);
        digester.addSetProperties("Server/Service");                      // name
        digester.addSetNext("Server/Service", "addService");

        // 连接器的端口是构造器参数，所以用自定义规则创建（Tomcat 里也有一个 ConnectorCreateRule）
        digester.addRule("Server/Service/Connector", new MiniDigester.Rule() {
            @Override public void begin(MiniDigester d, Attributes a) {
                d.push(new HttpConnector(Integer.parseInt(a.getValue("port"))));
            }
            @Override public void end(MiniDigester d) { d.pop(); }
        });
        digester.addSetNext("Server/Service/Connector", "addConnector");

        digester.addObjectCreate("Server/Service/Engine", StandardEngine::new);
        digester.addSetProperties("Server/Service/Engine");               // defaultHost
        digester.addSetNext("Server/Service/Engine", "setContainer");

        digester.addRule("Server/Service/Engine/Host", new MiniDigester.Rule() {
            @Override public void begin(MiniDigester d, Attributes a) {
                var host = new StandardHost(a.getValue("name"));
                if (a.getValue("appBase") != null) {
                    host.setAppBase(a.getValue("appBase"));
                }
                d.push(host);
            }
            @Override public void end(MiniDigester d) { d.pop(); }
        });
        digester.addSetNext("Server/Service/Engine/Host", "addChild");

        digester.addObjectCreate("Server/Service/Engine/Host/Context", StandardContext::new);
        digester.addSetProperties("Server/Service/Engine/Host/Context");  // path、docBase
        digester.addSetNext("Server/Service/Engine/Host/Context", "addChild");
        // end 倒序执行：这条规则的 end 会在 addChild 之前运行，给 Context 装上配置监听器和载入器
        digester.addRule("Server/Service/Engine/Host/Context", new MiniDigester.Rule() {
            @Override public void end(MiniDigester d) {
                var context = (StandardContext) d.peek(0);
                context.addLifecycleListener(new ContextConfig());
                context.setLoader(new WebappLoader(context.getDocBase(), context::reload));
            }
        });
        return digester;
    }

    /* ---------------- stop ---------------- */

    /** 只关心 <Server> 元素：拿到端口和关闭命令，然后发过去（原书 Listing 17.5 / 17.7） */
    protected void stop() throws Exception {
        var digester = new MiniDigester();
        digester.addObjectCreate("Server", StandardServer::new);
        digester.addSetProperties("Server");
        digester.addSetNext("Server", "setServer");
        digester.push(this);
        digester.parse(resolve(configFile));

        try (var socket = new Socket(InetAddress.getLoopbackAddress(), server.getPort());
             OutputStream out = socket.getOutputStream()) {
            out.write(server.getShutdown().getBytes(StandardCharsets.US_ASCII));
            System.out.println("Catalina: 已向端口 " + server.getPort() + " 发送关闭命令");
        } catch (IOException e) {
            System.out.println("Catalina.stop: 服务器没有运行（" + e.getMessage() + "）");
        }
    }

    private static Path resolve(Path config) {
        return config.isAbsolute() ? config : Path.of(System.getProperty("catalina.base")).resolve(config);
    }

    public static void main(String[] args) throws Exception {
        new Catalina().process(args);
    }
}
