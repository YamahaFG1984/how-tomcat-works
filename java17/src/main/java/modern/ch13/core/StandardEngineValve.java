package modern.ch13.core;

import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Contained;
import modern.ch05.core.Container;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;

/** Engine 的基础阀（原书 Listing 13.7）：HTTP/1.1 请求必须带 Host 头；按主机名选择 Host 并调用它 */
public class StandardEngineValve implements Valve, Contained {

    private StandardEngine engine;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext valveContext) throws Exception {
        if ("HTTP/1.1".equals(request.getProtocol()) && request.getServerName() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "HTTP/1.1 request without Host header");
            return;
        }
        StandardHost host = engine.map(request.getServerName());
        if (host == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No host matches " + request.getServerName());
            return;
        }
        host.invoke(request, response);
    }

    @Override public Container getContainer() { return engine; }
    @Override public void setContainer(Container container) { this.engine = (StandardEngine) container; }
}
