package io.github.weasleyj.http.rate.limit;

/**
 * Cache Type
 *
 * @author weasley
 * @version 1.0.0
 */
public enum CacheType {
    /**
     * Cache in redis
     *
     * @apiNote Distributed systems be supported
     */
    REDIS,
    /**
     * Cache in memory
     *
     * @apiNote Not support current limiting in distributed systems
     */
    MEMORY;
}
