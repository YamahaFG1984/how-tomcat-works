package modern.design.ch29;

import modern.design.kit.Lifecycle;
import modern.design.kit.Pipeline;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

/**
 * 装配层（相当于 Digester + ContextConfig）：读配置，用反射创建对象，把它们接成一棵树。
 * 业务组件里没有任何 new 具体实现的代码——「谁和谁连在一起」只在这里决定。
 *
 *   modules = order,stock
 *   module.order.handlers = create:modern.design.ch29.app.CreateOrderHandler, …
 *   module.order.valves   = modern.design.ch29.app.AuditValve
 *   engine.valves         = modern.design.ch29.app.TimingValve
 */
public final class Assembler {

    private Assembler() { }

    public static Server assemble(String config, BlockingQueue<String> inbox, BlockingQueue<String> outbox)
            throws IOException, ReflectiveOperationException {
        var p = new Properties();
        p.load(new StringReader(config));

        var engine = new Engine("engine");
        for (String valve : list(p, "engine.valves")) engine.pipeline().addValve(newInstance(valve));

        for (String moduleName : list(p, "modules")) {
            var module = new Module(moduleName);
            for (String valve : list(p, "module." + moduleName + ".valves")) module.pipeline().addValve(newInstance(valve));
            for (String entry : list(p, "module." + moduleName + ".handlers")) {
                String[] kv = entry.split(":", 2);                     // 动作名:类名
                module.addChild((Handler) Class.forName(kv[1]).getConstructor(String.class).newInstance(kv[0]));
            }
            engine.addChild(module);
        }

        var endpoint = new QueueEndpoint(p.getProperty("endpoint.name", "endpoint"), inbox, outbox);
        endpoint.setEngine(engine);

        var server = new Server("server");
        server.addChild(engine);                                       // 顺序有意义：核心在前，大门在后
        server.addChild(endpoint);
        return server;
    }

    @SuppressWarnings("unchecked")
    private static Pipeline.Valve<Exchange> newInstance(String className) throws ReflectiveOperationException {
        return (Pipeline.Valve<Exchange>) Class.forName(className).getConstructor().newInstance();
    }

    private static String[] list(Properties p, String key) {
        String v = p.getProperty(key, "").trim();
        return v.isEmpty() ? new String[0] : Arrays.stream(v.split(",")).map(String::trim).toArray(String[]::new);
    }

    /** 给演示用：递归打印组件树 */
    public static void print(Lifecycle c, String indent) {
        System.out.println(indent + c);
        if (c instanceof modern.design.kit.CompositeComponent<?> cc) {
            for (Lifecycle child : cc.children()) print(child, indent + "  ");
        }
    }
}
