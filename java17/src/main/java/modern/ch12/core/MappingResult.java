package modern.ch12.core;

/** 一次映射的结果：找到了哪个 Wrapper、按哪条规则、servletPath 和 pathInfo 各是什么 */
public record MappingResult(String wrapperName, Rule rule, String servletPath, String pathInfo) {

    /** 规范规定的四条规则，按顺序尝试 */
    public enum Rule { EXACT, PREFIX, EXTENSION, DEFAULT }
}
