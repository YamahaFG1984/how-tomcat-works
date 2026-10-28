package modern.ch05.valves;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;

import java.util.Collections;

/** 先放行，再打印所有请求头 */
public class HeaderLoggerValve implements Valve {

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext context) throws Exception {
        context.invokeNext(request, response);
        System.out.println("Header Logger Valve");
        for (String name : Collections.list(request.getHeaderNames())) {
            System.out.println(name + ":" + request.getHeader(name));
        }
        System.out.println("-----------------------------------");
    }
}
