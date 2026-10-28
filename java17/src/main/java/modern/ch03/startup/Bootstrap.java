package modern.ch03.startup;

import modern.ch03.connector.http.HttpConnector;

/**
 * 第 3 章启动类。从这一章开始，「等待连接」交给连接器 HttpConnector，
 * 「解析请求、创建请求/响应对象」交给 HttpProcessor。
 *
 * 运行：./run.sh ch03.startup.Bootstrap
 * 访问：http://localhost:8080/servlet/ModernServlet?userName=tarzan&password=pwd
 */
public final class Bootstrap {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new HttpConnector(port).start();
    }
}
