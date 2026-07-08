package com.haiz.servercore.website;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {

    private final Map<String, List<Long>> requests = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        List<Long> timestamps = requests.computeIfAbsent(ip, k -> new java.util.ArrayList<>());
        synchronized (timestamps) {
            timestamps.removeIf(t -> t < windowStart);
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.add(now);
            return true;
        }
    }
}
