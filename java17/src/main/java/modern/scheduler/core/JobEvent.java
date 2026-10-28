package modern.scheduler.core;

/** 发给监听器的事件（相当于 LifecycleEvent / ServletRequestEvent） */
public record JobEvent(Type type, String job, int attempt, long durationMillis, Throwable error) {

    public enum Type { STARTED, SUCCEEDED, FAILED, RETRYING, SKIPPED_OVERLAP, REJECTED }
}
