package modern.ch05.core;

import java.util.List;

/**
 * 容器。继承第 4 章连接器需要的 invoke(request, response)，再加上层级关系、管道和载入器。
 * 原书 Tomcat 4 的 Container 接口有四十多个方法，这里只保留本章用到的。
 */
public interface Container extends modern.ch04.connector.Container {

    String getName();

    void setName(String name);

    Container getParent();

    void setParent(Container parent);

    void addChild(Container child);

    void removeChild(Container child);

    Container findChild(String name);

    List<Container> findChildren();

    Pipeline getPipeline();

    Loader getLoader();

    void setLoader(Loader loader);
}
