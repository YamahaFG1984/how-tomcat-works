package modern.ch26.ws;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 用 JDK 11+ 自带的 java.net.http.WebSocket 做客户端：发几条消息、一个 ping，然后正常关闭。
 * 运行：./run.sh ch26.ws.WsClient ws://localhost:8080/ [消息...] [--hold=秒]
 * --hold：发完消息后保持连接若干秒再关闭，用来观察服务器的广播。
 */
public final class WsClient {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "ws://localhost:8080/";
        int hold = 0;
        var list = new java.util.ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--hold=")) hold = Integer.parseInt(args[i].substring(7));
            else list.add(args[i]);
        }
        String[] messages = list.isEmpty() ? new String[] {"你好", "time"} : list.toArray(String[]::new);
        var replies = new CountDownLatch(messages.length);

        WebSocket ws = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(url), new WebSocket.Listener() {
                    private final StringBuilder buf = new StringBuilder();
                    @Override public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
                        buf.append(data);
                        if (last) {                                   // 一条消息可能分成多帧到达
                            System.out.println("← " + buf);
                            buf.setLength(0);
                            replies.countDown();
                        }
                        w.request(1);                                 // 背压：处理完一条再要下一条
                        return null;
                    }
                    @Override public CompletionStage<?> onPong(WebSocket w, ByteBuffer message) {
                        System.out.println("← pong");
                        w.request(1);
                        return null;
                    }
                    @Override public CompletionStage<?> onClose(WebSocket w, int status, String reason) {
                        System.out.println("← close " + status + " " + reason);
                        return CompletableFuture.completedFuture(null);
                    }
                }).join();

        ws.sendPing(ByteBuffer.wrap(new byte[] {1, 2, 3})).join();
        for (String m : messages) {
            System.out.println("→ " + m);
            ws.sendText(m, true).join();
        }
        if (!replies.await(5, TimeUnit.SECONDS)) System.out.println("等待回复超时");
        Thread.sleep(hold * 1000L);
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
        Thread.sleep(300);
    }
}
