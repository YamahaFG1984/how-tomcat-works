package modern.ch18.startup;

import modern.ch05.core.Container;
import modern.ch06.core.LifecycleException;
import modern.ch08.loader.WebappLoader;
import modern.ch12.core.StandardContext;
import modern.ch13.core.StandardHost;
import modern.ch15.startup.ContextConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * 部署器（原书 org.apache.catalina.Deployer 接口 + StandardHostDeployer 实现）：
 * 「安装」一个应用 = 创建 StandardContext、设置路径和文档根目录、挂上 ContextConfig、加入 Host；
 * Host 已经在运行时，还要立即启动这个 Context。
 */
public class StandardHostDeployer {

    private final StandardHost host;

    public StandardHostDeployer(StandardHost host) {
        this.host = host;
    }

    public synchronized StandardContext install(String contextPath, Path docBase) throws LifecycleException {
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
            throw new IllegalArgumentException("上下文路径必须是 \"\" 或以 / 开头: " + contextPath);
        }
        if (findDeployedApp(contextPath) != null) {
            throw new IllegalStateException("上下文路径已被占用: " + contextPath);
        }
        var context = new StandardContext();
        context.setPath(contextPath);
        context.setDocBase(docBase.toString());
        context.addLifecycleListener(new ContextConfig());
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));
        host.addChild(context);
        System.out.println("StandardHostDeployer: 安装 " + display(contextPath) + " ← " + docBase);
        if (host.isStarted()) {
            context.start();
        }
        return context;
    }

    public synchronized void remove(String contextPath) throws LifecycleException {
        StandardContext context = require(contextPath);
        context.stop();
        host.removeChild(context);
        System.out.println("StandardHostDeployer: 移除 " + display(contextPath));
    }

    public void start(String contextPath) throws LifecycleException {
        require(contextPath).start();
    }

    public void stop(String contextPath) throws LifecycleException {
        require(contextPath).stop();
    }

    public StandardContext findDeployedApp(String contextPath) {
        return (StandardContext) host.findChild(contextPath);
    }

    public List<String> findDeployedApps() {
        return host.findChildren().stream().map(Container::getName).sorted().toList();
    }

    private StandardContext require(String contextPath) {
        StandardContext context = findDeployedApp(contextPath);
        if (context == null) {
            throw new IllegalArgumentException("没有部署在 " + display(contextPath) + " 的应用");
        }
        return context;
    }

    static String display(String contextPath) {
        return contextPath.isEmpty() ? "/（ROOT）" : contextPath;
    }
}
