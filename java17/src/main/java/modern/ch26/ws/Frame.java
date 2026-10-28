package modern.ch26.ws;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 一个 WebSocket 帧：
 *
 *   0                   1                   2                   3
 *   |F|R|R|R| opcode|M| 长度(7) |   扩展长度(16 或 64，可选)   |
 *   |   掩码键（4 字节，仅客户端→服务器）   |   负载 ……
 *
 * 客户端发来的帧必须加掩码（防止中间代理把它误认为 HTTP 而被缓存投毒），服务器发出的帧不加。
 */
record Frame(boolean fin, int opcode, boolean masked, byte[] payload) {

    static final int TEXT = 0x1, CLOSE = 0x8, PING = 0x9, PONG = 0xA;

    static Frame read(DataInputStream in) throws IOException {
        int b0 = in.readUnsignedByte();
        int b1 = in.readUnsignedByte();
        boolean fin = (b0 & 0x80) != 0;
        int opcode = b0 & 0x0F;
        boolean masked = (b1 & 0x80) != 0;
        long len = b1 & 0x7F;
        if (len == 126) len = in.readUnsignedShort();
        else if (len == 127) len = in.readLong();
        if (len > 1 << 20) throw new IOException("帧太大：" + len);
        byte[] mask = new byte[4];
        if (masked) in.readFully(mask);
        byte[] payload = new byte[(int) len];
        in.readFully(payload);
        if (masked) {
            for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
        }
        return new Frame(fin, opcode, masked, payload);
    }

    /** 服务器发出的帧：FIN=1，不加掩码 */
    static void write(OutputStream out, int opcode, byte[] payload) throws IOException {
        out.write(0x80 | opcode);
        if (payload.length < 126) {
            out.write(payload.length);
        } else if (payload.length < 65536) {
            out.write(126);
            out.write(payload.length >>> 8);
            out.write(payload.length & 0xFF);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) out.write((int) ((long) payload.length >>> (8 * i)) & 0xFF);
        }
        out.write(payload);
        out.flush();
    }
}
