package modern.ch10.realm;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

/** 用户写死在代码里的域（原书 ex10.pyrmont.realm.SimpleRealm），密码以 PBKDF2 哈希保存 */
public class SimpleRealm implements Realm {

    private record User(String passwordHash, Set<String> roles) {
    }

    private final Map<String, User> users = Map.of(
            "ken", new User(PasswordHasher.hash("blackcomb"), Set.of("manager", "programmer")),
            "cindy", new User(PasswordHasher.hash("bamboo"), Set.of("programmer")));

    @Override
    public Principal authenticate(String username, String credentials) {
        System.out.println("SimpleRealm.authenticate()");
        User user = username == null ? null : users.get(username);
        if (user == null || credentials == null || !PasswordHasher.matches(credentials, user.passwordHash())) {
            return null;
        }
        return new GenericPrincipal(username, user.roles());
    }

    @Override
    public boolean hasRole(Principal principal, String role) {
        return principal instanceof GenericPrincipal gp && gp.hasRole(role);
    }
}
