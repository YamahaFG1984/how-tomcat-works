package modern.ch02;

import java.io.IOException;

/** 第 2 章应用 2：Servlet 拿到的是外观对象 */
public final class HttpServer2 {

    public static void main(String[] args) throws IOException {
        new HttpServer(new ServletProcessor2()).await(HttpServer.port(args));
    }
}
