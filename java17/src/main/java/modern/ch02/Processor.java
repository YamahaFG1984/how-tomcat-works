package modern.ch02;

/** 处理一次请求的策略：静态资源处理器和两种 Servlet 处理器都实现它 */
@FunctionalInterface
public interface Processor {

    void process(Request request, Response response) throws Exception;
}
