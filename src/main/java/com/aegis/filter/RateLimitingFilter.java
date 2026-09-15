package com.aegis.filter;

import com.aegis.controller.TelemetryController;
import com.aegis.service.RateLimiterService;
import com.aegis.service.RateLimiterService.RateLimitResult;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitingFilter implements Filter {

    private final RateLimiterService rateLimiterService;
    private final TelemetryController telemetry;

    public RateLimitingFilter(RateLimiterService rateLimiterService, TelemetryController telemetry) {
        this.rateLimiterService = rateLimiterService;
        this.telemetry = telemetry;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();
        // Allow static assets and telemetry stream without rate limits
        if (uri.equals("/") || uri.startsWith("/index.html") || uri.startsWith("/telemetry")) {
            chain.doFilter(req, res);
            return;
        }

        // Tier Determination
        String apiKey = request.getHeader("X-API-KEY");
        String clientId;
        int limit;
        long window = 10000L; // 10-second sliding window

        if (apiKey != null && apiKey.startsWith("pro_")) {
            clientId = "pro:" + apiKey;
            limit = 25; // 25 requests per 10s
        } else {
            clientId = "ip:" + request.getRemoteAddr();
            limit = 5;  // 5 requests per 10s
        }

        RateLimitResult result = rateLimiterService.evaluate(clientId, limit, window);

        // Inject standard HTTP headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        if (result.fallbackActive()) {
            response.setHeader("X-RateLimit-Fallback", "true");
        }

        // Broadcast to telemetry dashboard
        int statusCode = result.allowed() ? 200 : 429;
        telemetry.broadcast(statusCode, clientId, result.remaining(), result.retryAfter(), result.fallbackActive());

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.retryAfter()));
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Too Many Requests\",\"status\":429,\"retryAfter\":%d}", result.retryAfter()
            ));
            return;
        }

        chain.doFilter(req, res);
    }
}