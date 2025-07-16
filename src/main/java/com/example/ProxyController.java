package main.java.com.example;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.HttpClient;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;

@Controller("/api/users")
public class ProxyController {

    @Inject
    @Client("http://localhost:8081")
    HttpClient userServiceClient;

    @Post("/{path:.*}")
    @Get("/{path:.*}")
    @Put("/{path:.*}")
    @Delete("/{path:.*}")
    public Publisher<HttpResponse<Object>> proxy(HttpRequest<?> request, @PathVariable String path) {
        String targetPath = "/users/" + path;
        HttpRequest<?> proxyRequest = HttpRequest.create(request.getMethod(), targetPath)
                .body(request.getBody().orElse(null));
        return userServiceClient.exchange(proxyRequest, Object.class);
    }
}
