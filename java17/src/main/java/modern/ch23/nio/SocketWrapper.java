package modern.ch23.nio;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 每个连接一个，挂在 SelectionKey 上（Tomcat：NioEndpoint.NioSocketWrapper）。
 * 它保存「这个连接读到一半的数据」，所以线程可以随时离开、下次由另一个线程接着处理。
 * 阻塞模型里这些状态都在线程栈上，线程就走不开。
 */
final class SocketWrapper {

    final SocketChannel channel;
    final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    volatile SelectionKey key;
    volatile long lastAccess = System.currentTimeMillis();
    int requests;

    SocketWrapper(SocketChannel channel) {
        this.channel = channel;
        readBuffer.flip();                                         // 初始为空的「读模式」
    }
}
