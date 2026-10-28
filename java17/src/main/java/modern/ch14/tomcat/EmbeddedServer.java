package modern.ch14.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tomcat 11 的 Server / Service：设置关闭端口和命令，给 Service 再加一个连接器，
 * 然后 server.await() 等待关闭命令（可以用第 14 章的 Stopper 发送）。
 *
 * 运行：./run.sh ch14.tomcat.EmbeddedServer [http端口] [第二个端口] [关闭端口]
 * 停止：./run.sh ch14.startup.Stopper 8005
 */
public final class EmbeddedServer {

    public static void main(String[] args) throws Exception {
        int port1 = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int port2 = args.length > 1 ? Integer.parseInt(args[1]) : 8081;
        int shutdownPort = args.length > 2 ? Integer.parseInt(args[2]) : 8005;

        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch14").toString());
        tomcat.setPort(port1);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        var second = new Connector();                      // 默认 HTTP/1.1 NIO
        second.setPort(port2);
        second.setProperty("address", "127.0.0.1");
        tomcat.getService().addConnector(second);

        Context app1 = tomcat.addContext("/app1", Path.of("webapps/app1").toAbsolutePath().toString());
        Tomcat.addServlet(app1, "Primitive", "PrimitiveServlet");
        app1.addServletMappingDecoded("/Primitive", "Primitive");

        var server = tomcat.getServer();                   // StandardServer
        server.setPort(shutdownPort);
        server.setShutdown("SHUTDOWN");

        tomcat.start();
        System.out.println("Server=" + server.getClass().getSimpleName()
                + ", Service=" + tomcat.getService().getName()
                + ", 连接器数量=" + tomcat.getService().findConnectors().length);
        server.await();
        tomcat.stop();
        tomcat.destroy();
        System.out.println("Tomcat 11 已优雅关闭");
    }
}
