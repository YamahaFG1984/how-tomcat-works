package modern.ch26.tomcat;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Jakarta WebSocket（JSR 356）的注解式端点：每个连接一个 ChatEndpoint 实例，
 * 所有在线会话放在一个静态集合里，收到消息就广播给所有人。
 */
@ServerEndpoint("/chat")
public class ChatEndpoint {

    private static final Set<Session> SESSIONS = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.add(session);
        broadcast("系统：" + session.getId() + " 加入，当前 " + SESSIONS.size() + " 人");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("[chat] " + session.getId() + " 线程 " + Thread.currentThread().getName() + "：" + message);
        broadcast(session.getId() + "：" + message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        SESSIONS.remove(session);
        broadcast("系统：" + session.getId() + " 离开（" + reason.getCloseCode() + "）");
    }

    private static void broadcast(String text) {
        for (Session s : SESSIONS) {
            try {
                synchronized (s) {                             // 同一个会话不允许并发发送
                    s.getBasicRemote().sendText(text);
                }
            } catch (IOException e) {
                SESSIONS.remove(s);
            }
        }
    }
}
