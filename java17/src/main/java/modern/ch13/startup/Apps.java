package modern.ch13.startup;

import modern.ch06.core.SimpleWrapper;
import modern.ch08.loader.WebappLoader;
import modern.ch09.session.StandardManager;
import modern.ch12.core.StandardContext;

import java.nio.file.Path;

/** 两个启动类共用的工具：按原书的方式创建一个包含 Primitive 和 Modern 的 Context */
final class Apps {

    private Apps() {
    }

    static StandardContext context(String path, String docBase, String... servlets) {
        var context = new StandardContext();
        context.setPath(path);
        context.setDocBase(docBase);
        for (String name : servlets) {
            var wrapper = new SimpleWrapper();
            wrapper.setName(name);
            wrapper.setServletClass(name + "Servlet");
            context.addChild(wrapper);
            context.addServletMapping("/" + name, name);
        }
        context.setLoader(new WebappLoader(context.getDocBase(), context::reload));
        var manager = new StandardManager(Path.of("work", path.isEmpty() ? "ROOT" : path.substring(1), "SESSIONS.ser"));
        manager.setCheckInterval(0);
        context.setManager(manager);
        return context;
    }
}
