package modern.ch07.logger;

public class SystemErrLogger extends LoggerBase {

    @Override
    public void log(String message) {
        System.err.println(message);
    }
}
