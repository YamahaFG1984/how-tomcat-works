package modern.ch06.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.SimpleContext;
import modern.ch06.core.SimpleContextLifecycleListener;
import modern.ch06.core.SimpleLoader;
import modern.ch06.core.SimpleWrapper;

/**
 * 第 6 章：只启动 / 停止顶层的 Context，载入器和两个 Wrapper 跟着被启动 / 停止。
 * 运行：./run.sh ch06.startup.Bootstrap   按回车停止，观察 Servlet 的 destroy 被调用
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);

        var wrapper1 = new SimpleWrapper();
        wrapper1.setName("Primitive");
        wrapper1.setServletClass("PrimitiveServlet");
        var wrapper2 = new SimpleWrapper();
        wrapper2.setName("Modern");
        wrapper2.setServletClass("ModernServlet");

        var context = new SimpleContext();
        context.addChild(wrapper1);
        context.addChild(wrapper2);
        context.setLoader(new SimpleLoader());
        context.addServletMapping("/Primitive", "Primitive");
        context.addServletMapping("/Modern", "Modern");
        context.addLifecycleListener(new SimpleContextLifecycleListener());
        // 监听器是函数式接口，也可以用 lambda
        context.addLifecycleListener(event -> System.out.println("  (lambda 监听器收到 " + event.type() + ")"));

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        context.start();

        System.in.read();
        context.stop();
        connector.stop();
    }
}
