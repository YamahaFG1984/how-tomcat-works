package modern.ch05.core;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 四种容器的公共部分（对应 org.apache.catalina.core.ContainerBase） */
public abstract class ContainerBase implements Container {

    protected final Pipeline pipeline = new SimplePipeline(this);
    protected final Map<String, Container> children = new ConcurrentHashMap<>();
    protected String name;
    protected Container parent;
    protected Loader loader;

    /** 容器自己不处理请求，全部交给管道 */
    @Override
    public void invoke(HttpRequest request, HttpResponse response) throws Exception {
        pipeline.invoke(request, response);
    }

    /** 自己没有载入器就用父容器的——第 2 个应用里两个 Wrapper 共用 Context 的载入器 */
    @Override
    public Loader getLoader() {
        if (loader != null) {
            return loader;
        }
        return parent != null ? parent.getLoader() : null;
    }

    @Override
    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    @Override
    public void addChild(Container child) {
        child.setParent(this);
        children.put(child.getName(), child);
    }

    @Override
    public void removeChild(Container child) {
        children.remove(child.getName());
        child.setParent(null);
    }

    @Override public Container findChild(String name) { return children.get(name); }
    @Override public List<Container> findChildren() { return List.copyOf(children.values()); }
    @Override public Pipeline getPipeline() { return pipeline; }
    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public Container getParent() { return parent; }
    @Override public void setParent(Container parent) { this.parent = parent; }
}
