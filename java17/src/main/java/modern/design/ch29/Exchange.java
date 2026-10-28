package modern.design.ch29;

/** 在阀链里流动的上下文：一次请求 + 它的响应（相当于 Request/Response 那一对） */
public final class Exchange {

    private final Command command;
    private String response;

    public Exchange(Command command) {
        this.command = command;
    }

    public Command command() { return command; }
    public String response() { return response; }
    public void respond(String text) { this.response = text; }
}
