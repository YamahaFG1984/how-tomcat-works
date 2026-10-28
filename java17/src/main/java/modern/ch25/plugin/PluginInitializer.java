package modern.ch25.plugin;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.HandlesTypes;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * 放在 WEB-INF/lib/plugin.jar 里，通过 META-INF/services/jakarta.servlet.ServletContainerInitializer 声明。
 * @HandlesTypes 告诉容器：扫描应用时把 PluginModule 的所有实现类收集起来，作为 onStartup 的第一个参数传给我。
 * Spring MVC 的 SpringServletContainerInitializer 正是这样找到所有 WebApplicationInitializer 的。
 */
@HandlesTypes(PluginModule.class)
public class PluginInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext sc) {
        System.out.println("[PluginInitializer] 容器传入的 @HandlesTypes 类：" + classes);
        if (classes == null) return;                         // 一个实现类都没有时是 null
        for (Class<?> c : classes) {
            if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) continue;
            try {
                ((PluginModule) c.getDeclaredConstructor().newInstance()).register(sc);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
