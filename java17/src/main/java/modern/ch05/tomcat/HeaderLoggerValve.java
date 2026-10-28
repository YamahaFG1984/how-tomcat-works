package modern.ch05.tomcat;

import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import java.io.IOException;
import java.util.Collections;

/**
 * Tomcat 11 里的阀：继承 ValveBase，只有 invoke(request, response) 两个参数。
 * 没有 ValveContext 了——每个阀持有下一个阀的引用，用 getNext().invoke(...) 往下传（责任链）。
 */
public class HeaderLoggerValve extends ValveBase {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        getNext().invoke(request, response);
        System.out.println("Header Logger Valve (Tomcat 11)");
        for (String name : Collections.list(request.getHeaderNames())) {
            System.out.println(name + ":" + request.getHeader(name));
        }
        System.out.println("-----------------------------------");
    }
}
