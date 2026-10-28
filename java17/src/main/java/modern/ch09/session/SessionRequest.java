package modern.ch09.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 给请求加上 getSession() 能力的包装器（装饰器模式，HttpServletRequestWrapper 是 Servlet API 自带的）。
 * 对应原书 HttpRequestBase.doGetSession()：先找请求里带来的会话 ID，找不到且 create 为 true 再新建，
 * 新建时通过 Set-Cookie 把 JSESSIONID 发给浏览器。
 */
public class SessionRequest extends HttpServletRequestWrapper {

    private final StandardManager manager;
    private final HttpServletResponse response;
    private final String contextPath;
    private StandardSession session;

    public SessionRequest(HttpServletRequest request, HttpServletResponse response,
                          StandardManager manager, String contextPath) {
        super(request);
        this.manager = manager;
        this.response = response;
        this.contextPath = contextPath;
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (session != null && session.isValid()) {                    // 本次请求里已经拿过
            return session.getSession();
        }
        session = manager.findSession(getRequestedSessionId());        // 浏览器带来的 ID 还有效吗？
        if (session != null) {
            return session.getSession();
        }
        if (!create) {
            return null;
        }
        session = manager.createSession();
        var cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setPath(contextPath);                                   // 浏览器只在 /myApp 下回传这个 Cookie
        cookie.setHttpOnly(true);                                      // JavaScript 读不到，防 XSS 偷会话
        response.addCookie(cookie);
        return session.getSession();
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return manager.findSession(getRequestedSessionId()) != null;
    }
}
