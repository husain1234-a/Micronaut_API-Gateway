package com.example;

import io.micronaut.http.*;
import io.micronaut.http.filter.*;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RateLimitingFilter implements HttpServerFilter {

    private final int maxRequests;
    private final long windowMs;
    private final Map<String, UserRequestInfo> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(
        @Value("${ratelimit.maxRequests:100}") int maxRequests,
        @Value("${ratelimit.windowMs:60000}") long windowMs
    ) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
        long now = Instant.now().toEpochMilli();

        UserRequestInfo info = requestCounts.computeIfAbsent(clientIp, k -> new UserRequestInfo(now, 0));

        synchronized (info) {
            if (now - info.windowStart > windowMs) {
                info.windowStart = now;
                info.requestCount = 1;
            } else {
                info.requestCount++;
            }

            if (info.requestCount > maxRequests) {
                return Publishers.just(HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Rate limit exceeded. Try again later."));
            }
        }

        return chain.proceed(request);
    }

    private static class UserRequestInfo {
        long windowStart;
        int requestCount;

        UserRequestInfo(long windowStart, int requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}
