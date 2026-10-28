package modern.ch03.connector.http;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import modern.ch03.ServletProcessor;
import modern.ch03.StaticResourceProcessor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 为一个 socket 创建 HttpRequest / HttpResponse，解析请求行和请求头，
 * 然后交给 ServletProcessor 或 StaticResourceProcessor。
 */
public class HttpProcessor {

    private static final StringManager sm = StringManager.getManager("modern.ch03.connector.http");

    private final HttpConnector connector;
    private HttpRequest request;

    public HttpProcessor(HttpConnector connector) {
        this.connector = connector;
    }

    public void process(Socket socket) {
        try (socket) {                                   // Java 9：对已有变量使用 try-with-resources
            var input = new SocketInputStream(socket.getInputStream(), 2048);
            OutputStream output = socket.getOutputStream();

            request = new HttpRequest(input);
            request.setRemoteAddr(socket.getInetAddress().getHostAddress());
            request.setServerPort(socket.getLocalPort());
            var response = new HttpResponse(output);
            response.setRequest(request);
            response.setHeader("Server", HttpResponse.serverInfo());

            try {
                if (!parseRequest(input)) {
                    return;                              // 连接上没有数据
                }
                parseHeaders(input);
            } catch (ServletException e) {
                // 请求格式错误：回一个 400，而不是像原书那样只打印异常、直接断开连接
                System.out.println(e.getMessage());
                response.sendError(HttpResponse.SC_BAD_REQUEST, e.getMessage());
                response.finishResponse();
                return;
            }

            if (request.getRequestURI().startsWith("/servlet/")) {
                new ServletProcessor().process(request, response);
            } else {
                new StaticResourceProcessor().process(request, response);
            }
            response.finishResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 解析请求行：方法、URI、查询串、URL 里的 ;jsessionid=、协议，并规范化 URI */
    private boolean parseRequest(SocketInputStream input) throws IOException, ServletException {
        HttpRequestLine requestLine = input.readRequestLine();
        if (requestLine == null) {
            return false;
        }
        if (requestLine.method().isEmpty()) {
            throw new ServletException(sm.getString("httpProcessor.parseRequest.method"));
        }
        String uri = requestLine.uri();
        if (uri.isEmpty()) {
            throw new ServletException(sm.getString("httpProcessor.parseRequest.uri"));
        }

        // ① 查询串：问号之后的部分
        int question = uri.indexOf('?');
        if (question >= 0) {
            request.setQueryString(uri.substring(question + 1));
            uri = uri.substring(0, question);
        }

        // ② 绝对 URI：http://host:port/path → /path
        if (!uri.startsWith("/")) {
            int pos = uri.indexOf("://");
            if (pos != -1) {
                pos = uri.indexOf('/', pos + 3);
                uri = pos == -1 ? "" : uri.substring(pos);
            }
        }

        // ③ 浏览器禁用 Cookie 时，会话 ID 会以 ;jsessionid=XXX 的形式拼在 URI 里
        String match = ";jsessionid=";
        int semicolon = uri.indexOf(match);
        if (semicolon >= 0) {
            String rest = uri.substring(semicolon + match.length());
            int semicolon2 = rest.indexOf(';');
            request.setRequestedSessionId(semicolon2 >= 0 ? rest.substring(0, semicolon2) : rest);
            request.setRequestedSessionURL(true);
            uri = uri.substring(0, semicolon) + (semicolon2 >= 0 ? rest.substring(semicolon2) : "");
        }

        // ④ 规范化
        String normalizedUri = RequestUtil.normalize(uri);
        request.setMethod(requestLine.method());
        request.setProtocol(requestLine.protocol());
        request.setRequestURI(normalizedUri != null ? normalizedUri : uri);
        if (normalizedUri == null) {
            throw new ServletException(sm.getString("httpProcessor.parseRequest.invalidUri", uri));
        }
        return true;
    }

    /** 逐个读取请求头；cookie、content-length、content-type 三个头需要额外处理 */
    private void parseHeaders(SocketInputStream input) throws IOException, ServletException {
        for (HttpHeader header; (header = input.readHeader()) != null; ) {
            String name = header.name();
            String value = header.value();
            request.addHeader(name, value);
            switch (name) {
                case "cookie" -> {
                    for (Cookie cookie : RequestUtil.parseCookieHeader(value)) {
                        if (cookie.getName().equalsIgnoreCase("jsessionid") && !request.isRequestedSessionIdFromCookie()) {
                            // Cookie 里的会话 ID 优先于 URL 里的，且只接受第一个
                            request.setRequestedSessionId(cookie.getValue());
                            request.setRequestedSessionCookie(true);
                            request.setRequestedSessionURL(false);
                        }
                        request.addCookie(cookie);
                    }
                }
                case "content-length" -> {
                    try {
                        request.setContentLength(Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        throw new ServletException(sm.getString("httpProcessor.parseHeaders.contentLength"));
                    }
                }
                case "content-type" -> request.setContentType(value);
                default -> { }
            }
        }
    }

}
