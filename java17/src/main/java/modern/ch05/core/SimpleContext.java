package modern.ch05.core;

import modern.ch03.connector.http.HttpRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context：代表一个 Web 应用，包含多个 Wrapper。
 * 原书 Tomcat 4 用一个单独的 Mapper 组件（SimpleContextMapper）根据 URL 找 Wrapper；
 * Tomcat 5 起去掉了 Mapper 接口，这里也直接把映射逻辑放进 map() 方法。
 */
public class SimpleContext extends ContainerBase {

    private final Map<String, String> servletMappings = new ConcurrentHashMap<>();

    public SimpleContext() {
        pipeline.setBasic(new SimpleContextValve());
    }

    /** URL 模式 → Wrapper 名字，例如 "/Primitive" → "Primitive" */
    public void addServletMapping(String pattern, String name) {
        servletMappings.put(pattern, name);
    }

    public String findServletMapping(String pattern) {
        return servletMappings.get(pattern);
    }

    /** 根据请求 URI（去掉上下文路径）找到负责处理它的 Wrapper；找不到返回 null */
    public SimpleWrapper map(HttpRequest request) {
        String relativeURI = request.getRequestURI().substring(request.getContextPath().length());
        String name = findServletMapping(relativeURI);
        return name == null ? null : (SimpleWrapper) findChild(name);
    }
}
