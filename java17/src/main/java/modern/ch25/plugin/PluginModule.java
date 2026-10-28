package modern.ch25.plugin;

import jakarta.servlet.ServletContext;

/** 插件 jar 定义的扩展点：应用里实现这个接口的类会被 PluginInitializer 自动发现并调用 */
public interface PluginModule {

    void register(ServletContext sc);
}
