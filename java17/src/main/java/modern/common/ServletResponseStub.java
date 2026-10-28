package modern.common;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * ServletResponse 的「空实现」：接口里的每个方法默认都抛 UnsupportedOperationException。
 *
 * 原书为了让 Request/Response 能编译，手写了几十个返回 null 的空方法；
 * Java 8+ 可以用接口默认方法把这些样板代码集中到一处，
 * 实现类只需要覆盖自己真正支持的那几个方法。
 * （本文件由脚本根据 jakarta.servlet 6.1 接口自动生成。）
 */
public interface ServletResponseStub extends ServletResponse {

    private static UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException("本章的简化实现不支持 " + method + "()");
    }

    @Override default String getCharacterEncoding() { throw unsupported("getCharacterEncoding"); }
    @Override default String getContentType() { throw unsupported("getContentType"); }
    @Override default ServletOutputStream getOutputStream() throws IOException { throw unsupported("getOutputStream"); }
    @Override default PrintWriter getWriter() throws IOException { throw unsupported("getWriter"); }
    @Override default void setCharacterEncoding(String arg0) { throw unsupported("setCharacterEncoding"); }
    @Override default void setContentLength(int arg0) { throw unsupported("setContentLength"); }
    @Override default void setContentLengthLong(long arg0) { throw unsupported("setContentLengthLong"); }
    @Override default void setContentType(String arg0) { throw unsupported("setContentType"); }
    @Override default void setBufferSize(int arg0) { throw unsupported("setBufferSize"); }
    @Override default int getBufferSize() { throw unsupported("getBufferSize"); }
    @Override default void flushBuffer() throws IOException { throw unsupported("flushBuffer"); }
    @Override default void resetBuffer() { throw unsupported("resetBuffer"); }
    @Override default boolean isCommitted() { throw unsupported("isCommitted"); }
    @Override default void reset() { throw unsupported("reset"); }
    @Override default void setLocale(Locale arg0) { throw unsupported("setLocale"); }
    @Override default Locale getLocale() { throw unsupported("getLocale"); }
}
