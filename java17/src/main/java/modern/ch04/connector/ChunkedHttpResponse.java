package modern.ch04.connector;

import modern.ch03.connector.http.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 在第 3 章 HttpResponse 的基础上支持 HTTP/1.1 分块传输（chunked encoding）。
 *
 * 默认仍然把整个正文缓冲起来、最后用 Content-Length 发出；
 * 一旦 Servlet 调用 flushBuffer()（想边算边发），就先发出带
 * "Transfer-Encoding: chunked" 的响应头，之后每次 flush 发一块：
 *
 *   十六进制长度\r\n
 *   数据\r\n
 *   ...
 *   0\r\n\r\n        ← 长度为 0 的块表示结束
 */
public class ChunkedHttpResponse extends HttpResponse {

    private boolean allowChunking;
    private boolean chunking;

    public ChunkedHttpResponse(OutputStream output) {
        super(output);
    }

    /** 只有 HTTP/1.1 客户端才认识分块编码 */
    public void setAllowChunking(boolean allowChunking) {
        this.allowChunking = allowChunking;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (!allowChunking) {
            return;                         // HTTP/1.0：只能全部缓冲，最后用 Content-Length
        }
        flushWriter();
        if (!isCommitted()) {
            writeHead(true);
            chunking = true;
        }
        writeChunk();
        output().flush();
    }

    @Override
    public void finishResponse() throws IOException {
        if (!chunking) {
            super.finishResponse();
            return;
        }
        flushWriter();
        writeChunk();
        output().write("0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
        output().flush();
    }

    private void writeChunk() throws IOException {
        int size = body().size();
        if (size == 0) {
            return;                          // 空块会被当成结束标记，不能发
        }
        output().write((Integer.toHexString(size) + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        body().writeTo(output());
        output().write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
        body().reset();
    }
}
