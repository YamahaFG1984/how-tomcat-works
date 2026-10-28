package modern.ch05.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.common.Facade;

/** Wrapper 的基础阀：分配 Servlet 实例并调用 service。它是管道的终点，不调用 invokeNext。 */
public class SimpleWrapperValve implements Valve, Contained {

    private Container container;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext context) throws Exception {
        var wrapper = (SimpleWrapper) container;
        wrapper.allocate().service(Facade.of(HttpServletRequest.class, request),
                                   Facade.of(HttpServletResponse.class, response));
    }

    @Override public Container getContainer() { return container; }
    @Override public void setContainer(Container container) { this.container = container; }
}
