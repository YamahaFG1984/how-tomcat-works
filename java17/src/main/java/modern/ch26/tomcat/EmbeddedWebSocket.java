package modern.ch26.tomcat;

import jakarta.servlet.ServletContext;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import org.apache.catalina.Context;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;

import java.nio.file.Files;

/**
 * Tomcat 11 的 WebSocket：WsSci（又是一个 ServletContainerInitializer，见第 25 章）在启动时
 * 创建 ServerContainer 并装上 WsFilter；WsFilter 发现握手请求后调用 request.upgrade()，
 * 之后这个连接就不再走 Servlet 管道，而是交给 WebSocket 的帧处理器。
 *
 * 运行：./run.sh ch26.tomcat.EmbeddedWebSocket
 *       ./run.sh ch26.ws.WsClient ws://localhost:8080/chat 大家好
 */
public final class EmbeddedWebSocket {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        var tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat-ch26ws").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context ctx = tomcat.addContext("", Files.createTempDirectory("ch26-docbase").toString());
        // 握手请求由过滤器 WsFilter 处理，但请求得先映射到某个 Servlet 才会进入过滤器链，
        // 否则 StandardContextValve 直接返回 404。普通应用里有 DefaultServlet 兜底，这里手动加上。
        Tomcat.addServlet(ctx, "default", new DefaultServlet());
        ctx.addServletMappingDecoded("/", "default");
        ctx.addServletContainerInitializer(new WsSci(), null);        // 先让 WsSci 创建 ServerContainer
        ctx.addServletContainerInitializer((classes, sc) -> deploy(sc), null);

        tomcat.start();
        System.out.println("WebSocket 聊天室：ws://localhost:" + port + "/chat");
        tomcat.getServer().await();
    }

    private static void deploy(ServletContext sc) {
        var container = (ServerContainer) sc.getAttribute(ServerContainer.class.getName());
        try {
            container.addEndpoint(ChatEndpoint.class);                 // 相当于扫描到 @ServerEndpoint
        } catch (DeploymentException e) {
            throw new IllegalStateException(e);
        }
    }
}
