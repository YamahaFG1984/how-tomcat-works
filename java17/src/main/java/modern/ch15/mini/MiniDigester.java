package modern.ch15.mini;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;

/**
 * 约 150 行的迷你 Digester，用来看清 Digester 的原理：
 *   SAX 逐个报告「开始标签 / 文本 / 结束标签」→ 维护当前路径（模式）→ 找到该模式上注册的规则 →
 *   开始标签调用 begin，文本调用 body，结束标签按注册的相反顺序调用 end。
 * 规则之间通过一个对象栈协作：ObjectCreate 压栈、SetNext 把栈顶交给次栈顶、结束时出栈。
 */
public class MiniDigester extends DefaultHandler {

    /** 规则：三个回调都有默认的空实现，按需覆盖 */
    public interface Rule {
        default void begin(MiniDigester digester, Attributes attributes) throws Exception { }
        default void body(MiniDigester digester, String text) throws Exception { }
        default void end(MiniDigester digester) throws Exception { }
    }

    private final Map<String, List<Rule>> rules = new HashMap<>();
    private final Deque<Object> stack = new ArrayDeque<>();          // 对象栈
    private final Deque<Object[]> params = new ArrayDeque<>();       // CallMethod 的参数栈
    private final Deque<StringBuilder> bodies = new ArrayDeque<>();  // 每层元素的文本
    private final List<String> path = new ArrayList<>();             // 当前元素路径
    private Object root;

    /* ---------------- 注册规则 ---------------- */

    public void addRule(String pattern, Rule rule) {
        rules.computeIfAbsent(pattern, k -> new ArrayList<>()).add(rule);
    }

    /** 遇到 pattern 时创建对象并压栈，元素结束时出栈 */
    public void addObjectCreate(String pattern, Supplier<?> factory) {
        addRule(pattern, new Rule() {
            @Override public void begin(MiniDigester d, Attributes a) { d.push(factory.get()); }
            @Override public void end(MiniDigester d) { d.pop(); }
        });
    }

    /** 把元素的每个属性 name="value" 变成对栈顶对象 setName(value) 的调用 */
    public void addSetProperties(String pattern) {
        addRule(pattern, new Rule() {
            @Override
            public void begin(MiniDigester d, Attributes attributes) throws Exception {
                Object top = d.peek(0);
                for (int i = 0; i < attributes.getLength(); i++) {
                    String name = attributes.getLocalName(i).isEmpty() ? attributes.getQName(i) : attributes.getLocalName(i);
                    String setter = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    invoke(top, setter, attributes.getValue(i));
                }
            }
        });
    }

    /** 元素结束时，调用「次栈顶」对象的 methodName(栈顶对象)，建立父子关系 */
    public void addSetNext(String pattern, String methodName) {
        addRule(pattern, new Rule() {
            @Override public void end(MiniDigester d) throws Exception { invoke(d.peek(1), methodName, d.peek(0)); }
        });
    }

    /**
     * 调用栈顶对象的方法。paramCount = 0：用元素本身的文本作参数；
     * paramCount &gt; 0：参数由子元素上的 CallParam 规则收集。
     */
    public void addCallMethod(String pattern, String methodName, int paramCount) {
        addRule(pattern, new Rule() {
            private final Deque<String> texts = new ArrayDeque<>();

            @Override public void begin(MiniDigester d, Attributes a) {
                if (paramCount > 0) {
                    d.params.push(new Object[paramCount]);
                }
            }

            @Override public void body(MiniDigester d, String text) {
                texts.push(text.trim());
            }

            @Override public void end(MiniDigester d) throws Exception {
                String text = texts.pop();
                Object[] args = paramCount > 0 ? d.params.pop() : (text.isEmpty() ? new Object[0] : new Object[] {text});
                invoke(d.peek(0), methodName, args);
            }
        });
    }

    /** 把这个元素的文本放进最近一个 CallMethod 参数数组的第 index 位 */
    public void addCallParam(String pattern, int index) {
        addRule(pattern, new Rule() {
            @Override public void body(MiniDigester d, String text) { d.params.peek()[index] = text.trim(); }
        });
    }

    /* ---------------- 栈 ---------------- */

    public void push(Object object) {
        if (stack.isEmpty()) {
            root = object;               // 第一个压栈的对象就是 parse() 的返回值
        }
        stack.push(object);
    }

    public Object pop() {
        return stack.pop();
    }

    /** peek(0) 是栈顶，peek(1) 是次栈顶 */
    public Object peek(int n) {
        return stack.stream().skip(n).findFirst().orElseThrow();
    }

    /* ---------------- 解析 ---------------- */

    @SuppressWarnings("unchecked")
    public <T> T parse(Path file) throws Exception {
        var factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);   // 防 XXE
        factory.newSAXParser().parse(file.toFile(), this);
        return (T) root;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        path.add(localName);
        bodies.push(new StringBuilder());
        forEachRule(rule -> rule.begin(this, attributes), false);
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        bodies.peek().append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String text = bodies.pop().toString();
        forEachRule(rule -> rule.body(this, text), false);
        forEachRule(rule -> rule.end(this), true);            // end 按注册的相反顺序调用
        path.remove(path.size() - 1);
    }

    private interface RuleAction {
        void apply(Rule rule) throws Exception;
    }

    private void forEachRule(RuleAction action, boolean reverse) {
        List<Rule> matched = new ArrayList<>(rules.getOrDefault(String.join("/", path), List.of()));
        if (reverse) {
            Collections.reverse(matched);
        }
        for (Rule rule : matched) {
            try {
                action.apply(rule);
            } catch (Exception e) {
                throw new IllegalStateException("处理 <" + String.join("/", path) + "> 时出错: " + e, e);
            }
        }
    }

    /** 按名字和参数个数找方法；String 参数会自动转换成 int / boolean */
    private static void invoke(Object target, String name, Object... args) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length && compatible(m.getParameterTypes(), args)) {
                Object[] converted = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    converted[i] = convert(args[i], m.getParameterTypes()[i]);
                }
                m.invoke(target, converted);
                return;
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name + Arrays.toString(args));
    }

    private static boolean compatible(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            boolean ok = args[i] == null || types[i].isInstance(args[i])
                    || (args[i] instanceof String && (types[i] == int.class || types[i] == boolean.class));
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static Object convert(Object value, Class<?> type) {
        if (value instanceof String s) {
            if (type == int.class) {
                return Integer.parseInt(s);
            }
            if (type == boolean.class) {
                return Boolean.parseBoolean(s);
            }
        }
        return value;
    }
}
