package modern.design.ch29;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 核心层的输入：已经和传输协议无关的「命令」（相当于 Catalina 的 Request）。
 * 文本格式只在边界层出现："order create id=1 amount=30" → Command("order", "create", {id=1, amount=30})
 */
public record Command(String module, String action, Map<String, String> params) {

    public static Command parse(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) throw new IllegalArgumentException("格式：<模块> <动作> [k=v ...]，收到 " + line);
        var params = new LinkedHashMap<String, String>();
        for (int i = 2; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            params.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return new Command(parts[0], parts[1], java.util.Collections.unmodifiableMap(params));   // 保持参数顺序
    }

    public String param(String name) {
        return params.get(name);
    }
}
