package com.hexabank.transfer.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del rate limiting por principal en {@code POST /transfers} (Fase 11).
 *
 * <p>Valores por defecto generosos (50 tokens, recarga de 50/seg): protegen frente a abuso o bucles
 * de reintentos sin estorbar el uso normal. {@code enabled=false} desactiva el filtro por completo,
 * lo que usa la prueba de carga k6 para aislar el efecto de los virtual threads.</p>
 */
@ConfigurationProperties(prefix = "hexabank.ratelimit")
public class RateLimitProperties {

    /** Si {@code false}, el filtro deja pasar todo (usado por el benchmark/carga). */
    private boolean enabled = true;

    /** Capacidad del bucket: ráfaga máxima admitida por principal. */
    private long capacity = 50;

    /** Tokens que se reponen cada {@link #refillPeriod} (recarga greedy, continua). */
    private long refillTokens = 50;

    /** Periodo de reposición de {@link #refillTokens}. */
    private Duration refillPeriod = Duration.ofSeconds(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public Duration getRefillPeriod() {
        return refillPeriod;
    }

    public void setRefillPeriod(Duration refillPeriod) {
        this.refillPeriod = refillPeriod;
    }
}
