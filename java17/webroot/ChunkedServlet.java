import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 第 4 章新增：边算边发的 Servlet。每输出一行就 flushBuffer()，
 * 连接器无法预先知道总长度，只能使用 Transfer-Encoding: chunked。
 */
public class ChunkedServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        for (int i = 1; i <= 3; i++) {
            out.println("第 " + i + " 块，线程 " + Thread.currentThread().getName());
            response.flushBuffer();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        out.print("done");
    }
}
