package modern.ch05.core;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

/** 阀调用 invokeNext 把请求交给管道里的下一个阀 */
@FunctionalInterface
public interface ValveContext {

    void invokeNext(HttpRequest request, HttpResponse response) throws Exception;
}
