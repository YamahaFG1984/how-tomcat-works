package modern.ch04.core;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch04.connector.Container;
import modern.common.Facade;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * 和默认连接器配合使用的最简容器：从 webroot 加载 Servlet 并调用 service。
 * 原书的 SimpleContainer 为了实现 Container 接口，写了三十多个空方法；
 * 这里 Container 是只有 invoke 的函数式接口，所以只剩真正干活的那一个方法。
 */
public class SimpleContainer implements Container {

    public static final Path WEB_ROOT = Path.of(System.getProperty("user.dir"), "webroot");

    @Override
    public void invoke(HttpRequest request, HttpResponse response) throws Exception {
        String uri = request.getRequestURI();
        String servletName = uri.substring(uri.lastIndexOf('/') + 1);
        var loader = new URLClassLoader(new URL[] {WEB_ROOT.toUri().toURL()}, getClass().getClassLoader());
        Class<?> servletClass;
        try {
            servletClass = loader.loadClass(servletName);
        } catch (ClassNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, null);
            return;
        }
        Servlet servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();
        servlet.service(Facade.of(HttpServletRequest.class, request),
                        Facade.of(HttpServletResponse.class, response));
    }
}
