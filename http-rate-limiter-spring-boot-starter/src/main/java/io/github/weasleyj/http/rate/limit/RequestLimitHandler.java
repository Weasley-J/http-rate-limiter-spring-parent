package io.github.weasleyj.http.rate.limit;

/**
 * Request Restrict Handler
 *
 * @author weasley
 * @version 1.0.0
 */
public class RequestLimitHandler {
    /**
     * the strategy for cancelling rate limit
     */
    private static final ThreadLocal<CancelLimitStrategy> CANCEL_STRATEGY = new ThreadLocal<>();

    private RequestLimitHandler() {
    }

    /**
     * @return should cancel restrict
     */
    public static boolean shouldCancelLimit() {
        if (null != CANCEL_STRATEGY.get()) {
            return CANCEL_STRATEGY.get().cancel();
        }
        return false;
    }

    /**
     * Set for strategy
     */
    public static void set(CancelLimitStrategy cancelStrategy) {
        CANCEL_STRATEGY.set(cancelStrategy);
    }

    /**
     * remove
     */
    public static void remove() {
        if (null != CANCEL_STRATEGY.get()) {
            CANCEL_STRATEGY.remove();
        }
    }
}
