package modern.ch10.realm;

import org.w3c.dom.Element;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 从 tomcat-users.xml 读取用户的域（原书 SimpleUserDatabaseRealm，内部用 MemoryUserDatabase 解析 XML）。
 * 这里直接用 JDK 自带的 DOM 解析器，并关闭 DTD 和外部实体，防止 XXE 攻击。
 */
public class UserDatabaseRealm implements Realm {

    private record User(String password, Set<String> roles) {
    }

    private final Map<String, User> users = new HashMap<>();

    public void createDatabase(Path path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(true);
        var doc = factory.newDocumentBuilder().parse(path.toFile());
        var nodes = doc.getElementsByTagNameNS("*", "user");
        for (int i = 0; i < nodes.getLength(); i++) {
            var e = (Element) nodes.item(i);
            Set<String> roles = Arrays.stream(e.getAttribute("roles").split(","))
                    .map(String::trim).filter(r -> !r.isEmpty()).collect(Collectors.toUnmodifiableSet());
            users.put(e.getAttribute("username"), new User(e.getAttribute("password"), roles));
        }
        System.out.println("UserDatabaseRealm: 从 " + path + " 读取了 " + users.size() + " 个用户");
    }

    @Override
    public Principal authenticate(String username, String credentials) {
        User user = username == null ? null : users.get(username);
        if (user == null || credentials == null) {
            return null;
        }
        boolean ok = user.password().startsWith("pbkdf2$")
                ? PasswordHasher.matches(credentials, user.password())
                : MessageDigest.isEqual(user.password().getBytes(UTF_8), credentials.getBytes(UTF_8));
        return ok ? new GenericPrincipal(username, user.roles()) : null;
    }

    @Override
    public boolean hasRole(Principal principal, String role) {
        return principal instanceof GenericPrincipal gp && gp.hasRole(role);
    }
}
