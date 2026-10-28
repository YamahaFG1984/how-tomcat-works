package modern.scheduler;

import modern.scheduler.core.Interceptors;
import modern.scheduler.core.Job;
import modern.scheduler.core.JobEvent;
import modern.scheduler.core.Schedule;
import modern.scheduler.core.Scheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 第 34 章：调度规则解析、重试、超时、不重叠、停机 */
class SchedulerTest {

    private Scheduler scheduler;
    private final List<JobEvent> events = new CopyOnWriteArrayList<>();

    private Scheduler newScheduler() {
        scheduler = new Scheduler("test", 2, 4, Duration.ofSeconds(2));
        scheduler.pipeline().addValve(Interceptors.retry(3, Duration.ofMillis(10), scheduler))
                .addValve(Interceptors.timeout(Duration.ofMillis(200), scheduler.watchdog()));
        scheduler.addListener(events::add);
        return scheduler;
    }

    @AfterEach
    void stop() {
        if (scheduler != null) scheduler.stop();
    }

    @Test
    void parsesSchedules() {
        assertEquals(Duration.ofMillis(500), Schedule.parse("every 500ms").period());
        assertEquals(Duration.ofMinutes(2), Schedule.parse("every 2m").period());
        assertTrue(Schedule.parse("once").once());
        assertThrows(IllegalArgumentException.class, () -> Schedule.parse("daily"));
    }

    @Test
    void retryRecoversFromTransientFailures() throws Exception {
        var calls = new AtomicInteger();
        var s = newScheduler();
        s.addChild(new Job("flaky", "test", ctx -> {
            if (calls.incrementAndGet() < 3) throw new IllegalStateException("503");
        }, Schedule.parse("once"), Map.of()));
        s.start();
        Thread.sleep(500);
        Job job = s.findChild("flaky").orElseThrow();
        assertEquals(1, job.getRunCount());
        assertEquals(0, job.getFailureCount());
        assertEquals(2, events.stream().filter(e -> e.type() == JobEvent.Type.RETRYING).count());
    }

    @Test
    void timeoutInterruptsEachAttempt() throws Exception {
        var s = newScheduler();
        s.addChild(new Job("stuck", "test", ctx -> Thread.sleep(5000), Schedule.parse("once"), Map.of()));
        s.start();
        Thread.sleep(1200);                                     // 3 次 × 200ms + 退避
        Job job = s.findChild("stuck").orElseThrow();
        assertEquals(1, job.getFailureCount());
    }

    @Test
    void overlappingRunsAreSkipped() throws Exception {
        var s = newScheduler();
        s.addChild(new Job("slow", "test", ctx -> Thread.sleep(150), Schedule.parse("every 50ms"), Map.of()));
        s.start();
        Thread.sleep(600);
        Job job = s.findChild("slow").orElseThrow();
        assertTrue(job.getSkippedCount() > 0, "应当有被跳过的触发");
        assertTrue(job.getRunCount() <= 5);
    }

    @Test
    void stopWaitsForInFlightTask() throws Exception {
        var finished = new AtomicInteger();
        var s = newScheduler();
        s.addChild(new Job("work", "test", ctx -> {
            Thread.sleep(150);
            finished.incrementAndGet();
        }, Schedule.parse("once"), Map.of()));
        s.start();
        Thread.sleep(50);
        s.stop();
        assertEquals(1, finished.get());
    }
}
