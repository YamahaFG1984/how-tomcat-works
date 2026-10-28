package modern.ch12.core;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static modern.ch12.core.MappingResult.Rule.DEFAULT;
import static modern.ch12.core.MappingResult.Rule.EXACT;
import static modern.ch12.core.MappingResult.Rule.EXTENSION;
import static modern.ch12.core.MappingResult.Rule.PREFIX;

/**
 * Servlet 规范的 URL 映射规则（原书 Tomcat 4 的 StandardContextMapper.map）：
 *   ① 精确匹配   "/Primitive"
 *   ② 最长前缀   "/modern/*"，从完整路径开始，每次去掉最后一段再试
 *   ③ 扩展名     "*.hello"
 *   ④ 默认       "/"
 * findServletMapping 由 Context 提供：模式 → Wrapper 名字。
 */
public final class StandardContextMapper {

    private final Function<String, String> findServletMapping;

    public StandardContextMapper(Function<String, String> findServletMapping) {
        this.findServletMapping = findServletMapping;
    }

    public StandardContextMapper(Map<String, String> servletMappings) {
        this(servletMappings::get);
    }

    /** relativeURI 是去掉上下文路径后的部分，例如 "/modern/users/42" */
    public Optional<MappingResult> map(String relativeURI) {
        // 规则 1：精确匹配
        String name = relativeURI.equals("/") ? null : findServletMapping.apply(relativeURI);
        if (name != null) {
            return Optional.of(new MappingResult(name, EXACT, relativeURI, null));
        }

        // 规则 2：前缀匹配，最长的前缀优先
        String servletPath = relativeURI;
        while (true) {
            name = findServletMapping.apply(servletPath + "/*");
            if (name != null) {
                String pathInfo = relativeURI.substring(servletPath.length());
                return Optional.of(new MappingResult(name, PREFIX, servletPath, pathInfo.isEmpty() ? null : pathInfo));
            }
            int slash = servletPath.lastIndexOf('/');
            if (slash < 0) {
                break;
            }
            servletPath = servletPath.substring(0, slash);
        }

        // 规则 3：扩展名匹配（只看最后一段里的扩展名）
        String last = relativeURI.substring(relativeURI.lastIndexOf('/') + 1);
        int period = last.lastIndexOf('.');
        if (period >= 0) {
            name = findServletMapping.apply("*" + last.substring(period));
            if (name != null) {
                return Optional.of(new MappingResult(name, EXTENSION, relativeURI, null));
            }
        }

        // 规则 4：默认 Servlet
        name = findServletMapping.apply("/");
        return Optional.ofNullable(name).map(n -> new MappingResult(n, DEFAULT, relativeURI, null));
    }
}
