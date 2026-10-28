package modern.ch05.core;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

/**
 * 阀：管道里的一个处理步骤。原书的 Valve 接口还有 getInfo()，这里省略，
 * 于是 Valve 只剩一个抽象方法，成了函数式接口——可以直接用 lambda 写阀。
 */
@FunctionalInterface
public interface Valve {

    void invoke(HttpRequest request, HttpResponse response, ValveContext context) throws Exception;
}
