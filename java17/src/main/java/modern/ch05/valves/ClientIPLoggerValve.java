package modern.ch05.valves;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;

/** 先放行（invokeNext），等后面的阀和 Servlet 都执行完，再打印客户端 IP */
public class ClientIPLoggerValve implements Valve {

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext context) throws Exception {
        context.invokeNext(request, response);
        System.out.println("Client IP Logger Valve");
        System.out.println(request.getRemoteAddr());
        System.out.println("-----------------------------------");
    }
}
