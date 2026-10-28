package modern.ch05.tomcat;

import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import java.io.IOException;

public class ClientIPLoggerValve extends ValveBase {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        getNext().invoke(request, response);
        System.out.println("Client IP Logger Valve (Tomcat 11)");
        System.out.println(request.getRemoteAddr());
        System.out.println("-----------------------------------");
    }
}
