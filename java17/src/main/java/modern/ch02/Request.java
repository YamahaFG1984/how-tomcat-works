package modern.ch02;

import modern.common.ServletRequestStub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 容器内部的请求对象，实现了 ServletRequest（未实现的方法由 ServletRequestStub 统一抛异常）。
 * uri() 和 parse() 是容器自己用的，不应该被 Servlet 调用——这正是第 2 个应用要解决的问题。
 */
public class Request implements ServletRequestStub {

    private final String uri;
    private final String protocol;
    private final Map<String, Object> attributes = new HashMap<>();

    private Request(String uri, String protocol) {
        this.uri = uri;
        this.protocol = protocol;
    }

    /** 解析请求行；连接上没有数据时返回 null */
    public static Request parse(InputStream input) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.ISO_8859_1));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return null;
        }
        System.out.println(requestLine);
        while (true) {                                  // 跳过请求头
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                break;
            }
        }
        String[] parts = requestLine.split(" ");
        return new Request(parts.length > 1 ? parts[1] : "/", parts.length > 2 ? parts[2] : "HTTP/1.0");
    }

    /** 容器内部方法：取得请求 URI */
    public String uri() {
        return uri;
    }

    /* ---------- ServletRequest 中本章真正实现的几个方法 ---------- */

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }
}
