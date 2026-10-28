package modern.design.kit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 设计工具包：阀链的顺序、短路、调用期间修改阀列表 */
class PipelineTest {

    @Test
    void valvesWrapBasicInOrder() throws Exception {
        List<String> log = new ArrayList<>();
        var p = new Pipeline<List<String>>(l -> l.add("basic"));
        p.addValve((l, next) -> { l.add("a>"); next.invoke(l); l.add("<a"); })
         .addValve((l, next) -> { l.add("b>"); next.invoke(l); l.add("<b"); });
        p.invoke(log);
        assertEquals(List.of("a>", "b>", "basic", "<b", "<a"), log);
    }

    @Test
    void valveCanShortCircuit() throws Exception {
        List<String> log = new ArrayList<>();
        var p = new Pipeline<List<String>>(l -> l.add("basic"));
        p.addValve((l, next) -> l.add("denied"));
        p.invoke(log);
        assertEquals(List.of("denied"), log);
    }

    @Test
    void addingValveDuringInvocationAffectsOnlyLaterCalls() throws Exception {
        List<String> log = new ArrayList<>();
        var p = new Pipeline<List<String>>(l -> l.add("basic"));
        p.addValve((l, next) -> {
            p.addValve((l2, n2) -> { l2.add("late"); n2.invoke(l2); });
            next.invoke(l);
        });
        p.invoke(log);
        assertEquals(List.of("basic"), log);                   // 本次调用用的是快照
    }
}
