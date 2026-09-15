package com.aegis.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    public record RateLimitResult(boolean allowed, long remaining, long retryAfter, boolean fallbackActive) {}

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("scripts/sliding_window.lua"));
        this.script.setResultType(List.class);
    }

    public RateLimitResult evaluate(String clientKey, int limit, long windowMillis) {
        try {
            long now = System.currentTimeMillis();
            List<Long> result = redisTemplate.execute(
                script,
                Collections.singletonList("rl:" + clientKey),
                String.valueOf(now),
                String.valueOf(windowMillis),
                String.valueOf(limit)
            );

            if (result != null && !result.isEmpty()) {
                boolean allowed = result.get(0) == 1L;
                long remaining = result.get(1);
                long retryAfter = result.get(2);
                return new RateLimitResult(allowed, remaining, retryAfter, false);
            }
        } catch (Exception ex) {
            // Fail-open strategy: Keep downstream APIs available if Redis degrades
            return new RateLimitResult(true, limit, 0, true);
        }
        return new RateLimitResult(false, 0, 1, false);
    }
}