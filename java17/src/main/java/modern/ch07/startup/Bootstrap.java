package modern.ch07.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch06.core.SimpleContextLifecycleListener;
import modern.ch06.core.SimpleLoader;
import modern.ch06.core.SimpleWrapper;
import modern.ch07.core.SimpleContext;
import modern.ch07.logger.FileLogger;

/**
 * 第 7 章：在第 6 章的应用上给 Context 配一个 FileLogger。
 * 运行：./run.sh ch07.startup.Bootstrap   按回车停止后查看 logs/FileLog_日期.txt
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

        // ------ add logger --------
        System.setProperty("catalina.base", System.getProperty("user.dir"));
        var logger = new FileLogger();
        logger.setPrefix("FileLog_");
        logger.setSuffix(".txt");
        logger.setTimestamp(true);
        logger.setDirectory("logs");       // 原书写到 webroot，这里换成单独的 logs 目录
        context.setLogger(logger);
        // 阀也可以用 Context 的 Logger 记访问日志
        context.getPipeline().addValve((request, response, next) -> {
            next.invokeNext(request, response);
            context.log(request.getMethod() + " " + request.getRequestURI() + " -> " + response.getStatus());
        });

        connector.setContainer(context);
        connector.initialize();
        connector.start();
        context.start();
        System.out.println("日志文件：" + logger.currentFile());

        System.in.read();
        context.stop();
        connector.stop();
    }
}
