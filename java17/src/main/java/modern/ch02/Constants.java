package modern.ch02;

import java.nio.file.Path;

public final class Constants {

    /** 静态资源和 Servlet 类文件都放在工作目录下的 webroot 里 */
    public static final Path WEB_ROOT = Path.of(System.getProperty("user.dir"), "webroot");

    private Constants() {
    }
}
