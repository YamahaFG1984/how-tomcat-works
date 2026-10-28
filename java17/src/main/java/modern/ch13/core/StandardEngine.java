package modern.ch13.core;

import modern.ch05.core.Container;

/**
 * 引擎（原书 org.apache.catalina.core.StandardEngine）：顶层容器，子容器只能是 Host，自己不能有父容器。
 * map(serverName) 按 Host 请求头选择虚拟主机：先找同名 Host，再找别名，最后用默认主机。
 */
public class StandardEngine extends LifecycleContainerBase {

    private String defaultHost;

    public StandardEngine() {
        pipeline.setBasic(new StandardEngineValve());
    }

    public String getDefaultHost() { return defaultHost; }
    public void setDefaultHost(String defaultHost) { this.defaultHost = defaultHost; }

    @Override
    public void addChild(Container child) {
        if (!(child instanceof StandardHost)) {
            throw new IllegalArgumentException("Engine 的子容器必须是 Host");
        }
        super.addChild(child);
    }

    @Override
    public void setParent(Container container) {
        throw new IllegalArgumentException("Engine 是顶层容器，不能有父容器");
    }

    public StandardHost map(String serverName) {
        if (serverName != null) {
            String name = serverName.toLowerCase();
            Container host = findChild(name);
            if (host != null) {
                return (StandardHost) host;
            }
            for (Container child : findChildren()) {
                if (((StandardHost) child).findAliases().contains(name)) {
                    return (StandardHost) child;
                }
            }
        }
        return defaultHost == null ? null : (StandardHost) findChild(defaultHost);
    }
}
