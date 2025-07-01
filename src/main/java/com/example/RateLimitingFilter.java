package com.example;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.*;
import io.micronaut.http.filter.*;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RateLimitingFilter implements HttpServerFilter {

    private static final int LIMIT = 5; // requests
    private static final long PERIOD = 60_000; // 1 minute in ms

    private final Map<String, UserRequestInfo> requestCounts = new ConcurrentHashMap<>();

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
        long now = Instant.now().toEpochMilli();

        UserRequestInfo info = requestCounts.computeIfAbsent(clientIp, k -> new UserRequestInfo(0, now));
        synchronized (info) {
            if (now - info.timestamp > PERIOD) {
                info.count = 1;
                info.timestamp = now;
            } else {
                if (info.count >= LIMIT) {
                    return Publishers.just(HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Rate limit exceeded. Try again later."));
                }
                info.count++;
            }
        }
        return chain.proceed(request);
    }

    private static class UserRequestInfo {
        int count;
        long timestamp;
        UserRequestInfo(int count, long timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }
}
