package modern.ch03.connector;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * 请求体输入流：最多只允许读 Content-Length 个字节，防止 Servlet 读过头、
 * 把同一连接上的下一个请求（第 4 章的 keep-alive）也读进来。
 */
public class RequestStream extends ServletInputStream {

    private final InputStream in;
    private long remaining;

    public RequestStream(InputStream in, long contentLength) {
        this.in = in;
        this.remaining = Math.max(contentLength, 0);
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int b = in.read();
        if (b >= 0) {
            remaining--;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) {
            return -1;
        }
        int n = in.read(b, off, (int) Math.min(len, remaining));
        if (n > 0) {
            remaining -= n;
        }
        return n;
    }

    /** 把没读完的请求体读掉丢弃，保证连接上的下一个请求从正确位置开始 */
    public void drain() throws IOException {
        byte[] discard = new byte[4096];
        while (read(discard, 0, discard.length) > 0) {
            // 丢弃
        }
    }

    @Override
    public boolean isFinished() {
        return remaining <= 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
        throw new UnsupportedOperationException("本章不支持异步 IO");
    }
}
