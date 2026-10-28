package modern.ch05.core;

import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

import java.util.Locale;

/** Context 的基础阀：禁止直接访问 WEB-INF / META-INF，然后找到 Wrapper 并调用它 */
public class SimpleContextValve implements Valve, Contained {

    private Container container;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext context) throws Exception {
        var ctx = (SimpleContext) container;
        String relativeURI = request.getRequestURI().substring(request.getContextPath().length())
                .toUpperCase(Locale.ROOT);
        if (relativeURI.startsWith("/WEB-INF") || relativeURI.startsWith("/META-INF")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, null);
            return;
        }
        SimpleWrapper wrapper = ctx.map(request);
        if (wrapper == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, null);
            return;
        }
        wrapper.invoke(request, response);     // 进入 Wrapper 自己的管道
    }

    @Override public Container getContainer() { return container; }
    @Override public void setContainer(Container container) { this.container = container; }
}
