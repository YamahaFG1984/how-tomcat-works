package modern.ch02;

import java.io.IOException;

/** 第 2 章应用 1：Servlet 直接拿到容器内部的 Request / Response */
public final class HttpServer1 {

    public static void main(String[] args) throws IOException {
        new HttpServer(new ServletProcessor1()).await(HttpServer.port(args));
    }
}
