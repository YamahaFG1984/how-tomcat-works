package modern.ch03.connector;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.OutputStream;

/** Servlet 通过 getOutputStream() 写的字节，先进响应自己的缓冲区 */
public class ResponseStream extends ServletOutputStream {

    private final OutputStream buffer;

    public ResponseStream(OutputStream buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(int b) throws java.io.IOException {
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws java.io.IOException {
        buffer.write(b, off, len);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener listener) {
        throw new UnsupportedOperationException("本章不支持异步 IO");
    }
}
