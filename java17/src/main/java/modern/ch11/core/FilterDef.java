package modern.ch11.core;

import java.util.Map;

/** web.xml 里的 <filter> 元素：过滤器名、类名、初始化参数 */
public record FilterDef(String filterName, String filterClass, Map<String, String> initParameters) {

    public FilterDef {
        initParameters = Map.copyOf(initParameters);
    }
}
