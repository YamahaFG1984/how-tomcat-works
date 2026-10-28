import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 第 8 章用来观察「自动重载」：修改 MESSAGE，运行 ./compile-servlets.sh，
 * 等几秒后再访问，就能看到新内容——不用重启服务器。
 * 它还会尝试加载容器自己的类，验证 Web 应用类加载器的隔离效果。
 */
public class HelloServlet extends HttpServlet {

    private static final String MESSAGE = "Hello, version 1";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        var out = response.getWriter();
        out.println(MESSAGE);
        out.println("loaded by: " + getClass().getClassLoader());
        try {
            Class.forName("modern.ch08.startup.Bootstrap");
            out.println("能看到容器的内部类（隔离失败）");
        } catch (ClassNotFoundException e) {
            out.println("看不到容器的内部类 modern.ch08.startup.Bootstrap（隔离生效）");
        }
    }
}
