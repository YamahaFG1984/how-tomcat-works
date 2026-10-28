package modern.ch12;

import modern.ch12.core.MappingResult.Rule;
import modern.ch12.core.StandardContextMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 第 12 章：四条映射规则及其优先级 */
class StandardContextMapperTest {

    private final StandardContextMapper mapper = new StandardContextMapper(Map.of(
            "/Primitive", "Primitive", "/modern/*", "Modern", "/modern/admin/*", "Admin",
            "*.hello", "Hello", "/", "Default"));

    @Test
    void exactMatchWins() {
        var r = mapper.map("/Primitive").orElseThrow();
        assertEquals("Primitive", r.wrapperName());
        assertEquals(Rule.EXACT, r.rule());
    }

    @Test
    void longestPrefixWinsAndSplitsPathInfo() {
        var r = mapper.map("/modern/admin/x").orElseThrow();
        assertEquals("Admin", r.wrapperName());
        assertEquals("/modern/admin", r.servletPath());
        assertEquals("/x", r.pathInfo());
        assertNull(mapper.map("/modern").orElseThrow().pathInfo());
    }

    @Test
    void prefixBeatsExtension() {
        assertEquals("Modern", mapper.map("/modern/x.hello").orElseThrow().wrapperName());
        assertEquals(Rule.EXTENSION, mapper.map("/a/b/index.hello").orElseThrow().rule());
    }

    @Test
    void fallsBackToDefaultServlet() {
        assertEquals(Rule.DEFAULT, mapper.map("/nothing/here").orElseThrow().rule());
    }
}
