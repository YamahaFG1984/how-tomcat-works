import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 第 2 章新增的「捣蛋」Servlet：它知道容器内部类的名字，试图把 ServletRequest
 * 向下转型回容器自己的 Request，从而调用 Servlet 本不该碰到的方法。
 * 用 HttpServer1 运行时能得逞，用 HttpServer2（外观模式）运行时会失败。
 */
public class PeekServlet extends GenericServlet {

    @Override
    public void service(ServletRequest request, ServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("request 的真实类型: " + request.getClass().getName());
        if (request instanceof modern.ch02.Request internal) {
            out.println("得手了！可以直接调用容器内部方法，例如 uri() = " + internal.uri());
        } else {
            out.println("转型失败：拿到的只是一个外观（facade），只能用 ServletRequest 接口里的方法。");
        }
    }
}
