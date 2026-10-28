package modern.ch03;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

import java.io.IOException;

public class StaticResourceProcessor {

    public void process(HttpRequest request, HttpResponse response) throws IOException {
        response.sendStaticResource(ServletProcessor.WEB_ROOT);
    }
}
