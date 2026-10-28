package modern.design.ch29;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 把 Tomcat 的整体架构用在一个非 Web 的「订单服务」上：
 *   配置文本 ──Assembler──→ 组件树（Server ⊃ Engine ⊃ Module ⊃ Handler，外加 Endpoint）──start──→ 处理消息 ──stop
 * 观察三件事：组件树的形状、启动与停止的顺序、请求在两级阀链里的路径。
 *
 * 运行：./run.sh design.ch29.ArchitectureDemo
 */
public final class ArchitectureDemo {

    static final String CONFIG = """
            endpoint.name = queue-endpoint
            engine.valves = modern.design.ch29.app.TimingValve
            modules = order, stock
            module.order.valves = modern.design.ch29.app.AuditValve
            module.order.handlers = create:modern.design.ch29.app.CreateOrderHandler, \\
                                    query:modern.design.ch29.app.QueryOrderHandler, \\
                                    cancel:modern.design.ch29.app.QueryOrderHandler
            module.stock.handlers = query:modern.design.ch29.app.StockHandler
            """;

    public static void main(String[] args) throws Exception {
        var inbox = new LinkedBlockingQueue<String>();
        var outbox = new LinkedBlockingQueue<String>();

        System.out.println("① 配置 → 装配：");
        Server server = Assembler.assemble(CONFIG, inbox, outbox);
        Assembler.print(server, "  ");

        System.out.println("\n② 启动（只启动顶层，子组件级联启动）：");
        listenRecursively(server, "  ");
        server.start();

        System.out.println("\n③ 运行：");
        String[] messages = {
                "order create id=1 amount=30", "order query id=1", "stock query sku=A-7",
                "order cancel id=1", "order create id=2 amount=-5", "payment refund id=1"};
        for (String m : messages) {                                  // 一条一条发，日志不会交错
            inbox.put(m);
            System.out.println("  " + outbox.poll(2, TimeUnit.SECONDS));
        }

        System.out.println("\n④ 停止（顺序和启动相反：先关大门，再停核心）：");
        server.stop();
    }

    /** 给每个组件挂一个监听器，按层级缩进打印「进入 STARTED / STOPPED」的时刻 */
    private static void listenRecursively(modern.design.kit.Lifecycle c, String indent) {
        c.addLifecycleListener(e -> {
            switch (e.to()) {
                case STARTED, STOPPED -> System.out.println(indent + e.to() + "  " + e.source().getName());
                default -> { }
            }
        });
        if (c instanceof modern.design.kit.CompositeComponent<?> cc) {
            cc.children().forEach(child -> listenRecursively(child, indent + "    "));
        }
    }
}
