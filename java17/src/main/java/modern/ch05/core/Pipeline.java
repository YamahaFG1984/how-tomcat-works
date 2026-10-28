package modern.ch05.core;

import modern.ch03.connector.http.HttpRequest;
import modern.ch03.connector.http.HttpResponse;

import java.util.List;

public interface Pipeline {

    Valve getBasic();

    void setBasic(Valve valve);

    void addValve(Valve valve);

    void removeValve(Valve valve);

    List<Valve> getValves();

    void invoke(HttpRequest request, HttpResponse response) throws Exception;
}
