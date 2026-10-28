package modern.ch25.miniboot;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.List;
import java.util.Set;

/**
 * 交给 Tomcat 的唯一一个 ServletContainerInitializer，相当于 Spring Boot 的 TomcatStarter。
 * 容器启动 Context 时回调 onStartup，我们在这里把扫描到的东西用 ServletContext 的编程式 API 注册进去：
 *   @WebListener → addListener    @WebFilter → addFilter    @WebServlet → addServlet
 *   AppInitializer 的实现类 → 实例化后调用 onStartup（相当于 @HandlesTypes 的效果）
 */
final class MiniBootStarter implements ServletContainerInitializer {

    private final List<Class<?>> classes;

    MiniBootStarter(List<Class<?>> classes) {
        this.classes = classes.stream().sorted(Comparator.comparing(Class::getName)).toList();
    }

    @Override
    public void onStartup(Set<Class<?>> ignored, ServletContext sc) throws ServletException {
        for (Class<?> c : classes) {
            if (c.isAnnotationPresent(WebListener.class)) {
                sc.addListener(c.asSubclass(EventListener.class));
                log("@WebListener", c, "");
            }
        }
        for (Class<?> c : classes) {
            WebFilter f = c.getAnnotation(WebFilter.class);
            if (f == null) continue;
            String name = f.filterName().isEmpty() ? c.getName() : f.filterName();
            var reg = sc.addFilter(name, c.asSubclass(Filter.class));
            reg.setAsyncSupported(f.asyncSupported());
            String[] patterns = f.urlPatterns().length > 0 ? f.urlPatterns() : f.value();
            var types = f.dispatcherTypes().length == 0 ? EnumSet.of(DispatcherType.REQUEST)
                    : EnumSet.copyOf(List.of(f.dispatcherTypes()));
            reg.addMappingForUrlPatterns(types, true, patterns);
            for (WebInitParam p : f.initParams()) reg.setInitParameter(p.name(), p.value());
            log("@WebFilter", c, String.join(",", patterns));
        }
        for (Class<?> c : classes) {
            WebServlet s = c.getAnnotation(WebServlet.class);
            if (s == null) continue;
            String name = s.name().isEmpty() ? c.getName() : s.name();
            var reg = sc.addServlet(name, c.asSubclass(Servlet.class));
            String[] patterns = s.urlPatterns().length > 0 ? s.urlPatterns() : s.value();
            reg.addMapping(patterns);
            reg.setLoadOnStartup(s.loadOnStartup());
            reg.setAsyncSupported(s.asyncSupported());
            for (WebInitParam p : s.initParams()) reg.setInitParameter(p.name(), p.value());
            log("@WebServlet", c, String.join(",", patterns));
        }
        for (Class<?> c : classes) {
            if (AppInitializer.class.isAssignableFrom(c) && !c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                try {
                    log("AppInitializer", c, "");
                    ((AppInitializer) c.getDeclaredConstructor().newInstance()).onStartup(sc);
                } catch (ReflectiveOperationException e) {
                    throw new ServletException("无法实例化 " + c.getName(), e);
                }
            }
        }
    }

    private static void log(String kind, Class<?> c, String extra) {
        System.out.printf("[MiniBoot] %-15s %s %s%n", kind, c.getSimpleName(), extra);
    }
}
