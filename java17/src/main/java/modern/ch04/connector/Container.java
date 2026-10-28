package modern.ch04.connector;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

/**
 * 连接器眼中的容器：只需要一个 invoke 方法（原书 org.apache.catalina.Container 有三十多个方法，
 * 但连接器只调用 invoke）。第 5 章会展开完整的 Container 接口。
 */
@FunctionalInterface
public interface Container {

    void invoke(HttpRequest request, HttpResponse response) throws Exception;
}
