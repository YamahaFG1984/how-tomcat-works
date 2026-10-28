package modern.ch10.realm;

import java.security.Principal;
import java.util.Set;

/**
 * 已认证的用户。原书的 GenericPrincipal 把密码也存在里面，并用排好序的数组 + 二分查找判断角色。
 * 这里用 record + 不可变 Set：不保存密码（认证完就不需要了），查角色是 O(1)。
 */
public record GenericPrincipal(String name, Set<String> roles) implements Principal {

    public GenericPrincipal {
        roles = Set.copyOf(roles);
    }

    @Override
    public String getName() {
        return name;
    }

    /** Servlet 2.4 起，角色名 "*" 表示「任何已认证的用户」 */
    public boolean hasRole(String role) {
        return "*".equals(role) || (role != null && roles.contains(role));
    }
}
