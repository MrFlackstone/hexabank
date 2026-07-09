package com.hexabank.transfer.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria de un token-bucket por principal ({@code sub} del JWT). Cada llamante tiene su
 * propio cupo, de modo que un cliente abusivo no afecta al resto.
 *
 * <p>En memoria y por instancia: suficiente para la demo (cada servicio corre en un único proceso).
 * En un despliegue multi-instancia se sustituiría por un backend distribuido (Redis/Hazelcast) sin
 * tocar el filtro, gracias a la abstracción de Bucket4j.</p>
 */
public class PrincipalRateLimiter {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public PrincipalRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    /** Intenta consumir un token del bucket del principal. {@code true} si quedaba cupo. */
    public boolean tryConsume(String principal) {
        return buckets.computeIfAbsent(principal, key -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(properties.getCapacity())
                        .refillGreedy(properties.getRefillTokens(), properties.getRefillPeriod()))
                .build();
    }
}
