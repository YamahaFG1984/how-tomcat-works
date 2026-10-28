package modern.ch23.nio;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 连接器和容器之间的接口。Tomcat 里是 org.apache.coyote.Adapter，唯一的实现 CoyoteAdapter
 * 负责把 Coyote 的 Request/Response 转成 Catalina 的 Request/Response，再调用 Engine 的管道。
 * 第 4 章的 Container.invoke() 扮演的是同一个角色。
 */
@FunctionalInterface
public interface Adapter {

    RawResponse service(RawRequest request) throws Exception;

    /**
     * 连接器实际调用的方法。同步实现直接返回已完成的 Future；
     * 返回未完成的 Future 就是「异步」：Worker 线程立刻归还，Future 完成时再派线程回来写响应（第 24 章）。
     */
    default CompletableFuture<RawResponse> serviceAsync(RawRequest request) {
        try {
            return CompletableFuture.completedFuture(service(request));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /** 用一个返回 Future 的函数创建异步 Adapter */
    static Adapter async(Function<RawRequest, CompletableFuture<RawResponse>> handler) {
        return new Adapter() {
            @Override public RawResponse service(RawRequest request) {
                return handler.apply(request).join();
            }
            @Override public CompletableFuture<RawResponse> serviceAsync(RawRequest request) {
                return handler.apply(request);
            }
        };
    }

    record RawResponse(int status, String contentType, byte[] body) { }
}
