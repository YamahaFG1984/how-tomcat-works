package modern.ch10.core;

import java.util.Set;

/**
 * 安全约束：哪些 URL、哪些 HTTP 方法，只允许哪些角色访问。
 * 原书分成 SecurityCollection（URL + 方法）和 SecurityConstraint（集合 + 角色）两个类，这里合成一个 record。
 *
 * @param patterns URL 模式："/*" 匹配全部，"/admin/*" 匹配前缀，其他为精确匹配
 * @param methods  空集表示所有方法
 * @param roles    允许的角色；"*" 表示任何已登录用户
 */
public record SecurityConstraint(Set<String> patterns, Set<String> methods, Set<String> roles) {

    public boolean matches(String uri, String method) {
        boolean methodOk = methods.isEmpty() || methods.contains(method);
        return methodOk && patterns.stream().anyMatch(p -> matchPattern(p, uri));
    }

    private static boolean matchPattern(String pattern, String uri) {
        if (pattern.equals("/*")) {
            return true;
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return uri.equals(prefix) || uri.startsWith(prefix + "/");
        }
        return pattern.equals(uri);
    }
}
