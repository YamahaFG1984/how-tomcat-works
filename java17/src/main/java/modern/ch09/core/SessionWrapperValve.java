package modern.ch09.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Contained;
import modern.ch05.core.Container;
import modern.ch05.core.SimpleWrapper;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;
import modern.ch09.session.SessionRequest;
import modern.common.Facade;

/**
 * Wrapper 的基础阀。和第 5 章相比多做了一件事：让请求能找到 Context 的管理器。
 * 原书是 request.setContext(context)，再由请求对象内部调用 context.getManager()；
 * 这里用 SessionRequest 包装请求，把管理器直接交给它。
 */
public class SessionWrapperValve implements Valve, Contained {

    private Container container;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext valveContext) throws Exception {
        var wrapper = (SimpleWrapper) container;
        var context = (SimpleContext) wrapper.getParent();
        HttpServletResponse res = Facade.of(HttpServletResponse.class, response);
        HttpServletRequest req = new SessionRequest(Facade.of(HttpServletRequest.class, request), res,
                context.getManager(), context.getPath());
        wrapper.allocate().service(req, res);
    }

    @Override public Container getContainer() { return container; }
    @Override public void setContainer(Container container) { this.container = container; }
}
