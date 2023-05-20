package io.github.weasleyj.http.rate.limit;

/**
 * The counter rate limit algorithm strategy
 *
 * @author weasley
 * @version 1.0.0
 */
public class DefaultCounterRateLimitStrategy implements RateLimitStrategy {
    @Override
    public boolean tryLimit() {
        // TODO: 2023/5/20
        return false;
    }
}
