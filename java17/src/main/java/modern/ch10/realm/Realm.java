package modern.ch10.realm;

import java.security.Principal;

/**
 * 域（Realm）：判断用户名和密码是否有效、用户是否拥有某个角色。
 * 原书的接口有四个 authenticate 重载（密码、字节数组、DIGEST、客户端证书），这里只保留最常用的一个。
 */
public interface Realm {

    /** 认证成功返回代表该用户的 Principal，失败返回 null */
    Principal authenticate(String username, String credentials);

    boolean hasRole(Principal principal, String role);
}
