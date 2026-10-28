package modern.ch25.demo;

import modern.ch25.miniboot.MiniBoot;

/**
 * 「Spring Boot 风格」的入口：只有一行。组件都在同一个包里，由 MiniBoot 扫描发现。
 * 运行：./run.sh ch25.demo.DemoApplication
 */
public final class DemoApplication {

    public static void main(String[] args) throws Exception {
        MiniBoot.run(DemoApplication.class, args);
    }
}
