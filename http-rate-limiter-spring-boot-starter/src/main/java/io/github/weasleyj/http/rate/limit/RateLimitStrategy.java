package io.github.weasleyj.http.rate.limit;

/**
 * The Strategy of Rate Limit
 *
 * @author weasley
 * @version 1.0.0
 */
@FunctionalInterface
public interface RateLimitStrategy {
    /**
     * Try to limit your http rates or guarantee idempotent for your handler method of Controller
     *
     * @return The result is true if success
     */
    boolean tryLimit();
}
