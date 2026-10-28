package modern.ch25.miniboot;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * 应用自己的初始化回调，相当于 Spring 的 WebApplicationInitializer / ServletContextInitializer：
 * MiniBoot 找到它的所有实现类，在容器启动时逐个调用，用代码注册 Servlet、过滤器。
 */
public interface AppInitializer {

    void onStartup(ServletContext sc) throws ServletException;
}
