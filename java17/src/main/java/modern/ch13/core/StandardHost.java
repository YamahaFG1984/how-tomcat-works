package modern.ch13.core;

import modern.ch05.core.Container;
import modern.ch12.core.StandardContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟主机（原书 org.apache.catalina.core.StandardHost）：
 *  - 子容器是 Context，以上下文路径（"/app1"、""）为名字；
 *  - map(uri) 用「最长上下文路径前缀」找 Context，找不到用默认 Context（路径为 ""）；
 *  - 启动时在管道里加一个错误报告阀（对应 ErrorReportValve）。
 */
public class StandardHost extends LifecycleContainerBase {

    private final Set<String> aliases = ConcurrentHashMap.newKeySet();
    private String appBase = "webapps";

    public StandardHost(String name) {
        setName(name);
        pipeline.setBasic(new StandardHostValve());
        pipeline.addValve(new ErrorReportValve());
    }

    public String getAppBase() { return appBase; }
    public void setAppBase(String appBase) { this.appBase = appBase; }
    public void addAlias(String alias) { aliases.add(alias.toLowerCase()); }
    public Set<String> findAliases() { return Set.copyOf(aliases); }

    /** Context 的名字就是它的上下文路径 */
    @Override
    public void addChild(Container child) {
        if (!(child instanceof StandardContext context)) {
            throw new IllegalArgumentException("Host 的子容器必须是 Context");
        }
        context.setName(context.getPath());
        super.addChild(context);
    }

    /** 原书 Listing 13.3：从完整 URI 开始，每次去掉最后一段，找最长的匹配上下文路径 */
    public StandardContext map(String uri) {
        if (uri == null) {
            return null;
        }
        String mapuri = uri;
        while (true) {
            Container context = findChild(mapuri);
            if (context != null) {
                return (StandardContext) context;
            }
            int slash = mapuri.lastIndexOf('/');
            if (slash < 0) {
                break;
            }
            mapuri = mapuri.substring(0, slash);
        }
        return (StandardContext) findChild("");          // 默认 Context（ROOT 应用）
    }
}
