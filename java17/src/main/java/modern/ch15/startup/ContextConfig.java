package modern.ch15.startup;

import modern.ch06.core.LifecycleEvent;
import modern.ch06.core.LifecycleListener;
import modern.ch06.core.SimpleWrapper;
import modern.ch12.core.StandardContext;
import modern.ch15.mini.MiniDigester;
import org.xml.sax.Attributes;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 迷你版 ContextConfig：Context 启动时读取 WEB-INF/web.xml，为每个 <servlet> 创建 Wrapper，
 * 为每个 <servlet-mapping> 调用 addServletMapping。这样启动类里就不用再手写 Wrapper 了。
 *
 * 规则和原书 WebRuleSet 中对应的部分一一对应（WrapperCreateRule、addSetNext("addChild")……）。
 */
public class ContextConfig implements LifecycleListener {

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        // 原书在 START 事件中配置；迷你容器的 START 事件在子容器启动之后才发出，
        // 所以这里提前到 BEFORE_START，让新建的 Wrapper 也能被一起启动
        if (event.type() == LifecycleEvent.Type.BEFORE_START) {
            applicationConfig((StandardContext) event.lifecycle());
        }
    }

    private void applicationConfig(StandardContext context) {
        Path webXml = context.getDocBase().resolve("WEB-INF/web.xml");
        if (!Files.exists(webXml)) {
            System.out.println("ContextConfig: 找不到 " + webXml + "，跳过");
            return;
        }
        var digester = createWebDigester();
        digester.push(context);                         // 栈底是 Context，规则都在它上面操作
        try {
            digester.parse(webXml);
        } catch (Exception e) {
            throw new IllegalStateException("解析 web.xml 失败", e);
        }
    }

    static MiniDigester createWebDigester() {
        var digester = new MiniDigester();
        // <servlet>：创建 Wrapper 压栈；结束时 context.addChild(wrapper)
        digester.addRule("web-app/servlet", new MiniDigester.Rule() {
            @Override public void begin(MiniDigester d, Attributes a) { d.push(new SimpleWrapper()); }
            @Override public void end(MiniDigester d) { d.pop(); }
        });
        digester.addSetNext("web-app/servlet", "addChild");
        digester.addCallMethod("web-app/servlet/servlet-name", "setName", 0);
        digester.addCallMethod("web-app/servlet/servlet-class", "setServletClass", 0);
        // <servlet-mapping>：addServletMapping(url-pattern, servlet-name)
        digester.addCallMethod("web-app/servlet-mapping", "addServletMapping", 2);
        digester.addCallParam("web-app/servlet-mapping/url-pattern", 0);
        digester.addCallParam("web-app/servlet-mapping/servlet-name", 1);
        return digester;
    }
}
