package modern.ch10.startup;

import modern.ch10.realm.UserDatabaseRealm;

import java.nio.file.Path;

/**
 * 第 10 章应用 2：用户来自 conf/tomcat-users.xml（admin / password 拥有 manager 角色）。
 * 运行：./run.sh ch10.startup.Bootstrap2
 */
public final class Bootstrap2 {

    public static void main(String[] args) throws Exception {
        var realm = new UserDatabaseRealm();
        realm.createDatabase(Path.of("conf", "tomcat-users.xml"));
        Bootstrap1.run(args, realm, "Simple User Database Realm");
    }
}
