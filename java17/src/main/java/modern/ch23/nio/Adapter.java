package modern.ch23.nio;

/**
 * 连接器和容器之间的接口。Tomcat 里是 org.apache.coyote.Adapter，唯一的实现 CoyoteAdapter
 * 负责把 Coyote 的 Request/Response 转成 Catalina 的 Request/Response，再调用 Engine 的管道。
 * 第 4 章的 Container.invoke() 扮演的是同一个角色。
 */
@FunctionalInterface
public interface Adapter {

    RawResponse service(RawRequest request) throws Exception;

    record RawResponse(int status, String contentType, byte[] body) { }
}
