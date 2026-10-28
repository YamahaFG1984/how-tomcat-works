package modern.ch02;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 第 1 个 Servlet 处理器：每次请求都新建一个类加载器，从 webroot 加载 Servlet 类，
 * 实例化后调用 service()。注意它把容器内部的 Request/Response 直接（向上转型后）交给了 Servlet。
 */
public class ServletProcessor1 implements Processor {

    @Override
    public void process(Request request, Response response) throws Exception {
        Servlet servlet = loadServlet(request.uri());
        servlet.service((ServletRequest) request, (ServletResponse) response);   // ← 安全隐患
        response.finish(200, "OK");
    }

    /** "/servlet/PrimitiveServlet" → 从 webroot 目录加载 PrimitiveServlet 类并实例化 */
    static Servlet loadServlet(String uri) throws Exception {
        String servletName = uri.substring(uri.lastIndexOf('/') + 1);
        // 以 "/" 结尾的 file: URL 表示一个目录仓库（repository）
        URL repository = Constants.WEB_ROOT.toUri().toURL();
        // 父加载器是应用类加载器，Servlet API（jakarta.servlet.*）从那里找到
        var loader = new URLClassLoader(new URL[] {repository}, ServletProcessor1.class.getClassLoader());
        Class<?> servletClass = loader.loadClass(servletName);
        // Class.newInstance() 自 Java 9 起已废弃，改用构造器
        return (Servlet) servletClass.getDeclaredConstructor().newInstance();
    }
}
