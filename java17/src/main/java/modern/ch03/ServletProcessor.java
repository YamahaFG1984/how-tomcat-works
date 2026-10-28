package modern.ch03;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.common.Facade;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/** 从 webroot 加载 Servlet，把请求/响应的外观交给它（和第 2 章 ServletProcessor2 一样） */
public class ServletProcessor {

    public static final Path WEB_ROOT = Path.of(System.getProperty("user.dir"), "webroot");

    public void process(HttpRequest request, HttpResponse response) throws Exception {
        String uri = request.getRequestURI();
        String servletName = uri.substring(uri.lastIndexOf('/') + 1);
        var loader = new URLClassLoader(new URL[] {WEB_ROOT.toUri().toURL()}, getClass().getClassLoader());
        Servlet servlet = (Servlet) loader.loadClass(servletName).getDeclaredConstructor().newInstance();
        servlet.service(Facade.of(HttpServletRequest.class, request),
                        Facade.of(HttpServletResponse.class, response));
    }
}
