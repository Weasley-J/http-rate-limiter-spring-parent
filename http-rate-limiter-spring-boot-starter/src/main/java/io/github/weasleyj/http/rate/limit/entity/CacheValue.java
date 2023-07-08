package io.github.weasleyj.http.rate.limit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static io.github.weasleyj.http.rate.limit.util.TemporalUnitUtils.convertToDateTime;

/**
 * Caffeine Value to set expire time for single key
 *
 * @author weasley
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class CacheValue<K, V> implements Serializable {
    /**
     * The key
     */
    private K key;
    /**
     * The value of key
     */
    private V value;
    /**
     * the length of time from now when the entry should be automatically removed
     */
    private long duration;
    /**
     * the unit that duration is expressed in
     */
    private TimeUnit timeUnit;
    /**
     * The time when a key putted
     */
    private LocalDateTime putTime = LocalDateTime.now();
    /**
     * The time when a key expired
     */
    private LocalDateTime expireAt;

    public CacheValue(long duration, TimeUnit timeUnit) {
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    public CacheValue(K key, V value, long duration, TimeUnit timeUnit) {
        this.key = key;
        this.value = value;
        this.duration = duration;
        this.timeUnit = timeUnit;
    }

    public LocalDateTime getExpireAt() {
        return convertToDateTime(this.duration, this.timeUnit, this.putTime);
    }

    /**
     * Whether the key expires
     */
    public boolean isExpired() {
        return null == getExpireAt() || getExpireAt().isBefore(LocalDateTime.now());
    }
}
