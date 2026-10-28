package modern.ch10.authenticator;

import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Contained;
import modern.ch05.core.Container;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;
import modern.ch10.core.SecureContext;

/** auth-method 为 NONE 时使用：只检查约束，不做登录——受保护的资源一律 403 */
public class NonLoginAuthenticator implements Valve, Contained, Authenticator {

    private SecureContext context;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext next) throws Exception {
        boolean protectedResource = context.findConstraints().stream()
                .anyMatch(c -> c.matches(request.getRequestURI(), request.getMethod()));
        if (protectedResource) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, null);
        } else {
            next.invokeNext(request, response);
        }
    }

    @Override public Container getContainer() { return context; }
    @Override public void setContainer(Container container) { this.context = (SecureContext) container; }
}
