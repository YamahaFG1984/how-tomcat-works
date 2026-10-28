package modern.ch10.authenticator;

import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;
import modern.ch05.core.Contained;
import modern.ch05.core.Container;
import modern.ch05.core.Valve;
import modern.ch05.core.ValveContext;
import modern.ch10.core.SecureContext;
import modern.ch10.core.SecurityConstraint;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;

/**
 * HTTP Basic 认证阀（原书 org.apache.catalina.authenticator.BasicAuthenticator，继承 AuthenticatorBase）。
 *
 *   1. 请求不受任何约束保护 → 直接放行；
 *   2. 没带 Authorization 头、或用户名密码不对 → 401 + WWW-Authenticate，浏览器弹出登录框；
 *   3. 认证成功但角色不够 → 403；
 *   4. 认证成功且有权限 → 记下 Principal，放行到 Servlet。
 */
public class BasicAuthenticator implements Valve, Contained, Authenticator {

    private SecureContext context;

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveContext next) throws Exception {
        Optional<SecurityConstraint> constraint = context.findConstraints().stream()
                .filter(c -> c.matches(request.getRequestURI(), request.getMethod()))
                .findFirst();
        if (constraint.isEmpty()) {
            next.invokeNext(request, response);
            return;
        }

        Principal principal = authenticate(request.getHeader("Authorization"));
        if (principal == null) {
            String realmName = context.getLoginConfig() != null && context.getLoginConfig().realmName() != null
                    ? context.getLoginConfig().realmName()
                    : "localhost:" + request.getServerPort();      // 没配域名就用「主机:端口」，和原书一致
            response.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\", charset=\"UTF-8\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, null);
            return;                                                 // 不调用 invokeNext：Servlet 不会被执行
        }
        boolean allowed = constraint.get().roles().stream().anyMatch(role -> context.getRealm().hasRole(principal, role));
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, null);
            return;
        }
        request.setUserPrincipal(principal, role -> context.getRealm().hasRole(principal, role));
        next.invokeNext(request, response);
    }

    /** 解析 "Basic dXNlcjpwYXNz"（Base64 编码的 "user:pass"），交给域去验证 */
    private Principal authenticate(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;                                            // 不是合法的 Base64
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) {
            return null;
        }
        return context.getRealm().authenticate(decoded.substring(0, colon), decoded.substring(colon + 1));
    }

    @Override public Container getContainer() { return context; }
    @Override public void setContainer(Container container) { this.context = (SecureContext) container; }
}
