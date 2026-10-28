package modern.ch11.core;

/** web.xml 里的 <filter-mapping> 元素：哪个过滤器作用于哪个 URL 模式（"/*" 或精确路径） */
public record FilterMap(String filterName, String urlPattern) {

    public boolean matches(String uri) {
        return urlPattern.equals("/*")
                || (urlPattern.endsWith("/*") && uri.startsWith(urlPattern.substring(0, urlPattern.length() - 1)))
                || urlPattern.equals(uri);
    }
}
