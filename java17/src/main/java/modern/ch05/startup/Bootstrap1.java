package modern.ch05.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch05.core.SimpleLoader;
import modern.ch05.core.SimpleWrapper;
import modern.ch05.valves.ClientIPLoggerValve;
import modern.ch05.valves.HeaderLoggerValve;

/**
 * 第 5 章应用 1：整个容器只有一个 Wrapper。任何 URL 都会交给 ModernServlet。
 * 运行：./run.sh ch05.startup.Bootstrap1    访问 http://localhost:8080/
 */
public final class Bootstrap1 {

    public static void main(String[] args) throws Exception {
        var connector = new HttpConnector(args.length > 0 ? Integer.parseInt(args[0]) : 8080);
        var wrapper = new SimpleWrapper();
        wrapper.setServletClass("ModernServlet");
        wrapper.setLoader(new SimpleLoader());
        wrapper.getPipeline().addValve(new HeaderLoggerValve());
        wrapper.getPipeline().addValve(new ClientIPLoggerValve());

        connector.setContainer(wrapper);
        connector.initialize();
        connector.start();
        System.in.read();
        connector.stop();
    }
}
