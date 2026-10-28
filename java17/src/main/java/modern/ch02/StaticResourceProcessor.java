package modern.ch02;

import java.io.IOException;

public class StaticResourceProcessor implements Processor {

    @Override
    public void process(Request request, Response response) throws IOException {
        response.sendStaticResource(request.uri());
    }
}
