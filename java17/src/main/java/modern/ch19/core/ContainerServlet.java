package modern.ch19.core;

import modern.ch05.core.SimpleWrapper;

/**
 * 实现了这个接口的 Servlet，在加载时会拿到代表它的 Wrapper，
 * 顺着 Wrapper → Context → Host 就能操作容器内部（原书 org.apache.catalina.ContainerServlet）。
 */
public interface ContainerServlet {

    SimpleWrapper getWrapper();

    void setWrapper(SimpleWrapper wrapper);
}
