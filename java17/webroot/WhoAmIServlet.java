import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** 第 10 章新增：显示当前登录的用户和角色 */
public class WhoAmIServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        var out = response.getWriter();
        out.println("authType   = " + request.getAuthType());
        out.println("remoteUser = " + request.getRemoteUser());
        out.println("manager?   = " + request.isUserInRole("manager"));
        out.println("programmer?= " + request.isUserInRole("programmer"));
    }
}
