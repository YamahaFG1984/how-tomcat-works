package modern.ch25.demo;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch25.miniboot.AppInitializer;

import java.io.IOException;
import java.time.LocalDateTime;

/** 不用注解，用代码注册：适合需要根据配置决定注册什么的场景（Spring 的 DispatcherServlet 就是这样注册的） */
public class ApiInitializer implements AppInitializer {

    @Override
    public void onStartup(ServletContext sc) {
        sc.addServlet("time", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json");
                resp.getWriter().printf("{\"now\":\"%s\"}%n", LocalDateTime.now().withNano(0));
            }
        }).addMapping("/api/time");
    }
}
