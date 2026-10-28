package modern.ch10.core;

import modern.ch05.core.Valve;
import modern.ch06.core.LifecycleEvent;
import modern.ch06.core.LifecycleListener;
import modern.ch10.authenticator.Authenticator;
import modern.ch10.authenticator.BasicAuthenticator;
import modern.ch10.authenticator.NonLoginAuthenticator;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Context 的配置监听器：启动时，如果有安全约束，就根据 auth-method 安装对应的认证阀。
 * 原书用 Class.forName("...BasicAuthenticator").newInstance() 动态加载；
 * 这里用「认证方式 → 构造器引用」的 Map，同样是按名字选择实现，但编译期就能检查类是否存在。
 */
public class SimpleContextConfig implements LifecycleListener {

    private static final Map<String, Supplier<Valve>> AUTHENTICATORS = Map.of(
            "BASIC", BasicAuthenticator::new,
            "NONE", NonLoginAuthenticator::new);

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.type() == LifecycleEvent.Type.START) {
            authenticatorConfig((SecureContext) event.lifecycle());
        }
    }

    private synchronized void authenticatorConfig(SecureContext context) {
        if (context.findConstraints().isEmpty()) {
            return;                                                   // 没有约束，不需要认证器
        }
        if (context.getLoginConfig() == null) {
            context.setLoginConfig(new LoginConfig("NONE", null));
        }
        boolean installed = context.getPipeline().getBasic() instanceof Authenticator
                || context.getPipeline().getValves().stream().anyMatch(v -> v instanceof Authenticator);
        if (installed || context.getRealm() == null) {
            return;                                                   // 已经有了，或者没有域可以用来认证
        }
        Supplier<Valve> factory = AUTHENTICATORS.get(context.getLoginConfig().authMethod());
        if (factory == null) {
            throw new IllegalArgumentException("不支持的认证方式: " + context.getLoginConfig().authMethod());
        }
        context.getPipeline().addValve(factory.get());
        System.out.println("Added authenticator valve to Context");
    }
}
