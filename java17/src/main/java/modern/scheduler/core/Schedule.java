package modern.scheduler.core;

import java.time.Duration;
import java.util.Locale;

/** 调度规则："every 2s"、"every 500ms"、"once"。用 record 表示，启动后不变（第 32 章规则 ②） */
public record Schedule(Duration period, boolean once) {

    public static Schedule parse(String text) {
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.equals("once")) return new Schedule(Duration.ZERO, true);
        if (!t.startsWith("every ")) throw new IllegalArgumentException("调度规则应为 once 或 every <时长>：" + text);
        return new Schedule(parseDuration(t.substring(6).trim()), false);
    }

    public static Duration parseDuration(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.endsWith("ms")) return Duration.ofMillis(Long.parseLong(t.substring(0, t.length() - 2)));
        long n = Long.parseLong(t.substring(0, t.length() - 1));
        return switch (t.charAt(t.length() - 1)) {
            case 's' -> Duration.ofSeconds(n);
            case 'm' -> Duration.ofMinutes(n);
            case 'h' -> Duration.ofHours(n);
            default -> throw new IllegalArgumentException("不认识的时长：" + s);
        };
    }
}
