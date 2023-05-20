package io.github.weasleyj.http.rate.limit.util;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Temporal Unit Utils
 *
 * @author weasley
 * @version 1.0.0
 */
public class TemporalUnitUtils {

    private TemporalUnitUtils() {
    }

    /**
     * {@link ChronoUnit} to {@link  TimeUnit}
     *
     * @param chronoUnit The chrono unit
     * @return TimeUnit
     */
    public static TimeUnit toTimeUnit(ChronoUnit chronoUnit) {
        if (chronoUnit == null) {
            return null;
        }
        switch (chronoUnit) {
            case DAYS:
                return TimeUnit.DAYS;
            case HOURS:
                return TimeUnit.HOURS;
            case MINUTES:
                return TimeUnit.MINUTES;
            case SECONDS:
                return TimeUnit.SECONDS;
            case MICROS:
                return TimeUnit.MICROSECONDS;
            case MILLIS:
                return TimeUnit.MILLISECONDS;
            case NANOS:
                return TimeUnit.NANOSECONDS;
            default:
                throw new UnsupportedOperationException("Not a real temporal unit");
        }
    }

    /**
     * {@link TimeUnit} to {@link ChronoUnit}
     *
     * @param timeUnit The time unit
     * @return ChronoUnit
     */
    public static ChronoUnit toChronoUnit(TimeUnit timeUnit) {
        if (timeUnit == null) {
            return null;
        }
        switch (timeUnit) {
            case DAYS:
                return ChronoUnit.DAYS;
            case HOURS:
                return ChronoUnit.HOURS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            default:
                throw new UnsupportedOperationException("Not a real time unit");
        }
    }
}
