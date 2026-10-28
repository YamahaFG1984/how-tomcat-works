package modern.ch04.startup;

import modern.ch04.connector.HttpConnector;
import modern.ch04.core.SimpleContainer;

/**
 * 第 4 章启动类：创建连接器和容器，把容器交给连接器，initialize + start，按回车停止。
 *
 * 运行：./run.sh ch04.startup.Bootstrap [端口] [minProcessors] [maxProcessors]
 */
public final class Bootstrap {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var connector = new HttpConnector(port);
        if (args.length > 2) {
            connector.setMinProcessors(Integer.parseInt(args[1]));
            connector.setMaxProcessors(Integer.parseInt(args[2]));
        }
        connector.setContainer(new SimpleContainer());
        connector.initialize();
        connector.start();

        System.out.println("按回车键停止……");
        System.in.read();
        connector.stop();
    }
}
