package modern.ch05.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch05.core.SimpleContext;
import modern.ch05.core.SimpleLoader;
import modern.ch05.core.SimpleWrapper;
import modern.ch05.valves.ClientIPLoggerValve;
import modern.ch05.valves.HeaderLoggerValve;

/**
 * 第 5 章应用 2：一个 Context 包含两个 Wrapper，按 URL 分派。
 * 运行：./run.sh ch05.startup.Bootstrap2    访问 /Primitive 或 /Modern
 */
public final class Bootstrap2 {

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
        context.getPipeline().addValve(new HeaderLoggerValve());
        context.getPipeline().addValve(new ClientIPLoggerValve());
        // 阀是函数式接口，也可以直接写成 lambda：记录每个请求花了多少毫秒
        context.getPipeline().addValve((request, response, valveContext) -> {
            long start = System.nanoTime();
            valveContext.invokeNext(request, response);
            System.out.printf("%s 耗时 %.1f ms%n", request.getRequestURI(), (System.nanoTime() - start) / 1e6);
        });
        context.setLoader(new SimpleLoader());          // 两个 Wrapper 都通过 getLoader() 找到它
        context.addServletMapping("/Primitive", "Primitive");
        context.addServletMapping("/Modern", "Modern");

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        System.in.read();
        connector.stop();
    }
}
