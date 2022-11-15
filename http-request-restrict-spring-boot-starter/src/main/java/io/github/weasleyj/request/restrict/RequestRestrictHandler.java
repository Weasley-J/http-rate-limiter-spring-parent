package io.github.weasleyj.request.restrict;

/**
 * Request Restrict Handler
 *
 * @author weasley
 * @version 1.0.0
 */
public class RequestRestrictHandler {
    /**
     * the strategy for cancelling api restrict
     */
    private static final ThreadLocal<RestrictCancelStrategy> CANCEL_STRATEGY = new ThreadLocal<>();

    private RequestRestrictHandler() {
    }

    /**
     * @return should cancel restrict
     */
    public static boolean shouldCancelRestrict() {
        if (null != CANCEL_STRATEGY.get()) {
            return CANCEL_STRATEGY.get().cancelRestrict();
        }
        return false;
    }

    /**
     * Set for strategy
     */
    public static void set(RestrictCancelStrategy cancelStrategy) {
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
