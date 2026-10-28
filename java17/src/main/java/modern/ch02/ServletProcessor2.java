package modern.ch02;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import modern.common.Facade;

/**
 * 第 2 个 Servlet 处理器：和 ServletProcessor1 唯一的区别是，交给 Servlet 的是外观对象。
 * Servlet 只能看到 ServletRequest / ServletResponse 接口里的方法，无法转型回 Request/Response。
 */
public class ServletProcessor2 implements Processor {

    @Override
    public void process(Request request, Response response) throws Exception {
        Servlet servlet = ServletProcessor1.loadServlet(request.uri());
        ServletRequest requestFacade = Facade.of(ServletRequest.class, request);
        ServletResponse responseFacade = Facade.of(ServletResponse.class, response);
        servlet.service(requestFacade, responseFacade);
        response.finish(200, "OK");
    }
}
