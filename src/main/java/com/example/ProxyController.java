package main.java.com.example;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.HttpClient;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import reactor.core.publisher.Mono;

@Controller("/api/users")
public class ProxyController {

    @Inject
    @Client("http://localhost:8081")
    HttpClient userServiceClient;

    @Get
    @Post
    @Put
    @Delete
    public Publisher<HttpResponse<Object>> proxyRoot(@Nullable @Body String body, HttpRequest<?> request) {
        String targetPath = "/users";
        System.out.println("in users" );
        // (same logic as before, but without appending path)
        MutableHttpRequest<Object> proxyRequest = HttpRequest.create(request.getMethod(), targetPath)
            .body(body);

        for (String headerName : request.getHeaders().names()) {
            for (String value : request.getHeaders().getAll(headerName)) {
                proxyRequest.header(headerName, value);
            }
        }
        proxyRequest.header("Content-Type", "application/json");

        return Mono.from(userServiceClient.exchange(proxyRequest, Object.class))
            .onErrorResume(HttpClientResponseException.class, ex ->
                Mono.just(HttpResponse.status(ex.getStatus()).body(ex.getResponse().getBody(Object.class).orElse(null)))
            );
    }
    
    @Post("/{path:.*}")
    @Get("/{path:.*}")
    @Put("/{path:.*}")
    @Delete("/{path:.*}")
    public Publisher<HttpResponse<Object>> proxy(@Body String body, HttpRequest<?> request, @PathVariable String path) {
        String targetPath = "/users/" + path;

        // String rawBody = request.getBody(String.class).orElse(null);
        // System.out.println("Forwarding body: " + rawBody);
        MutableHttpRequest<Object> proxyRequest = HttpRequest.create(request.getMethod(), targetPath)
            .body(body);

        // Copy all headers
        for (String headerName : request.getHeaders().names()) {
            for (String value : request.getHeaders().getAll(headerName)) {
                proxyRequest.header(headerName, value);
            }
        }
        System.out.println("Headers: {}"+proxyRequest +"hello"+ Object.class);

        return Mono.from(userServiceClient.exchange(proxyRequest, Object.class))
            .onErrorResume(HttpClientResponseException.class, ex ->
                Mono.just(HttpResponse.status(ex.getStatus()).body(ex.getResponse().getBody(Object.class).orElse(null)))
            );
    }
}
