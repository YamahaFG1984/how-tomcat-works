import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

/** 原书 SessionServlet.java 的 jakarta 版（第 9 章起使用）：把表单提交的值存进 Session */
public class SessionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("SessionServlet -- service");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        String value = request.getParameter("value");
        HttpSession session = request.getSession(true);
        out.println("<html><head><title>SessionServlet</title></head><body>");
        out.println("<br>the previous value is " + session.getAttribute("value"));
        out.println("<br>the current value is " + value);
        session.setAttribute("value", value);
        out.println("<br><hr><form>New Value: <input name=value><input type=submit></form>");
        out.println("</body></html>");
    }
}
