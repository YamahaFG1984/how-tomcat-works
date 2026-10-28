package modern.ch10.core;

import modern.ch08.core.SimpleContext;
import modern.ch10.realm.Realm;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 在第 8 章 Context 的基础上，增加域、安全约束和登录配置 */
public class SecureContext extends SimpleContext {

    private final List<SecurityConstraint> constraints = new CopyOnWriteArrayList<>();
    private Realm realm;
    private LoginConfig loginConfig;

    public void addConstraint(SecurityConstraint constraint) { constraints.add(constraint); }
    public List<SecurityConstraint> findConstraints() { return List.copyOf(constraints); }
    public Realm getRealm() { return realm; }
    public void setRealm(Realm realm) { this.realm = realm; }
    public LoginConfig getLoginConfig() { return loginConfig; }
    public void setLoginConfig(LoginConfig loginConfig) { this.loginConfig = loginConfig; }
}
