package modern.ch13.core;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;

/**
 * 对应 org.apache.catalina.valves.ErrorReportValve：后面的阀或 Servlet 抛出异常时，
 * 生成一个 500 错误页面，而不是让异常一路抛到连接器。
 */
public class ErrorReportValve implements Valve {

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext next) throws Exception {
        try {
            next.invokeNext(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, null);
            response.getWriter().print("<p>" + e.getClass().getName() + ": " + e.getMessage() + "</p>");
        }
    }
}
