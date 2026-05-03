package com.voicememo.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // One bucket per IP address, stored in memory
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ipAddress) {
        return buckets.computeIfAbsent(ipAddress, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        // Allow 60 requests per minute per IP
        Bandwidth limit = Bandwidth.classic(
                60, Refill.greedy(60, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    // Stricter bucket for upload endpoint (10 uploads per minute)
    public Bucket resolveUploadBucket(String ipAddress) {
        return buckets.computeIfAbsent("upload_" + ipAddress,
                ip -> Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                10, Refill.greedy(10, Duration.ofMinutes(1))))
                        .build());
    }
}