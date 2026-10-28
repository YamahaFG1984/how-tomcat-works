package modern.ch04.connector;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import modern.ch03.connector.http.HttpHeader;
import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpRequestLine;
import modern.ch03.connector.http.RequestUtil;
import modern.ch03.connector.http.SocketInputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * 处理一个连接上的所有请求（HTTP/1.1 持久连接）。
 *
 * 原书的 HttpProcessor 是一个常驻线程：run() 里循环 await() 等 socket、process()、recycle 回池。
 * 这里它只是一个提交给线程池的任务——「常驻线程」和「回收进池」都由 ThreadPoolExecutor 负责。
 */
public class HttpProcessor implements Runnable {

    private static final String SERVER_INFO = "Pyrmont Default Connector (Java 17)";

    private final HttpConnector connector;
    private final Socket socket;

    /** 当前请求的状态，由 parseHeaders 设置 */
    private boolean keepAlive;
    private boolean sendAck;
    private boolean http11;

    HttpProcessor(HttpConnector connector, Socket socket) {
        this.connector = connector;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void process() throws IOException {
        try (socket) {
            socket.setSoTimeout(connector.getConnectionTimeout());
            var input = new SocketInputStream(socket.getInputStream(), connector.getBufferSize());
            OutputStream output = new BufferedOutputStream(socket.getOutputStream());

            keepAlive = true;
            while (!connector.isStopped() && keepAlive) {
                var request = new HttpRequest(input);
                var response = new ChunkedHttpResponse(output);
                response.setRequest(request);
                response.setHeader("Server", SERVER_INFO);
                sendAck = false;

                try {
                    parseConnection(request);
                    if (!parseRequest(input, request)) {
                        break;                                  // 客户端关闭了连接
                    }
                    if (!request.getProtocol().startsWith("HTTP/0")) {
                        parseHeaders(input, request);
                    }
                } catch (SocketTimeoutException e) {
                    break;                                      // keep-alive 连接空闲超时
                } catch (ServletException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                    response.setHeader("Connection", "close");
                    response.finishResponse();
                    output.flush();
                    break;
                }

                if (http11) {
                    ackRequest(output);                         // Expect: 100-continue
                    response.setAllowChunking(connector.isChunkingAllowed());
                }
                if (!keepAlive) {
                    response.setHeader("Connection", "close");
                }

                try {
                    connector.getContainer().invoke(request, response);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!response.isCommitted()) {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
                    }
                }

                response.finishResponse();
                request.finishRequest();                        // 丢掉没读完的请求体
                output.flush();

                if ("close".equals(response.getHeader("Connection"))) {
                    keepAlive = false;                          // Servlet 主动要求关闭
                }
            }
        }
    }

    /** 记录客户端地址和服务器端口（原书还会在这里处理代理端口） */
    private void parseConnection(HttpRequest request) {
        request.setRemoteAddr(socket.getInetAddress().getHostAddress());
        request.setServerPort(socket.getLocalPort());
    }

    /** 和第 3 章基本相同，另外根据协议版本决定是否默认保持连接 */
    private boolean parseRequest(SocketInputStream input, HttpRequest request) throws IOException, ServletException {
        HttpRequestLine line = input.readRequestLine();
        if (line == null) {
            return false;
        }
        String uri = line.uri();
        int question = uri.indexOf('?');
        if (question >= 0) {
            request.setQueryString(uri.substring(question + 1));
            uri = uri.substring(0, question);
        }
        String normalized = RequestUtil.normalize(uri);
        if (normalized == null) {
            throw new ServletException("Invalid URI: " + uri);
        }
        request.setMethod(line.method());
        request.setRequestURI(normalized);
        request.setProtocol(line.protocol());

        http11 = "HTTP/1.1".equals(line.protocol());
        keepAlive = http11;                 // HTTP/1.1 默认持久连接；HTTP/1.0 默认用完即关
        return true;
    }

    private void parseHeaders(SocketInputStream input, HttpRequest request) throws IOException, ServletException {
        for (HttpHeader header; (header = input.readHeader()) != null; ) {
            String value = header.value();
            request.addHeader(header.name(), value);
            switch (header.name()) {
                case "cookie" -> {
                    for (Cookie cookie : RequestUtil.parseCookieHeader(value)) {
                        if (cookie.getName().equalsIgnoreCase("jsessionid") && !request.isRequestedSessionIdFromCookie()) {
                            request.setRequestedSessionId(cookie.getValue());
                            request.setRequestedSessionCookie(true);
                        }
                        request.addCookie(cookie);
                    }
                }
                case "content-length" -> {
                    try {
                        request.setContentLength(Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        throw new ServletException("Invalid 'Content-Length' header");
                    }
                }
                case "content-type" -> request.setContentType(value);
                case "connection" -> {
                    if (value.equalsIgnoreCase("close")) {
                        keepAlive = false;
                    } else if (value.equalsIgnoreCase("keep-alive")) {
                        keepAlive = true;       // HTTP/1.0 客户端显式要求保持连接
                    }
                }
                case "expect" -> {
                    if (value.equalsIgnoreCase("100-continue")) {
                        sendAck = true;
                    } else {
                        throw new ServletException("Unknown Expectation: " + value);
                    }
                }
                default -> { }
            }
        }
    }

    /** 客户端发了 Expect: 100-continue，告诉它「请继续发送请求体」 */
    private void ackRequest(OutputStream output) throws IOException {
        if (sendAck) {
            output.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            output.flush();
        }
    }
}
