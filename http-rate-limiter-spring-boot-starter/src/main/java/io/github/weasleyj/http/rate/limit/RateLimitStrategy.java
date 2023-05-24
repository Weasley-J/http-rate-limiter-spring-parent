package io.github.weasleyj.http.rate.limit;

import io.github.weasleyj.http.rate.limit.annotation.RateLimit;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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
     * @param rateLimit The annotation of RateLimit
     * @param request   The HttpServletRequest object
     * @return The result is true if success， false: Don't need to limit
     * @throws InterruptedException throw
     */
    boolean tryLimit(RateLimit rateLimit, Map<String, Object> headers, HttpServletRequest request) throws InterruptedException;
}
