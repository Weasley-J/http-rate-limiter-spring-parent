package io.github.weasleyj.http.rate.limit.entity;

import io.github.weasleyj.http.rate.limit.annotation.RateLimit;
import io.github.weasleyj.http.rate.limit.config.HttpRateLimitProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;

/**
 * Redis key request params
 *
 * @author weasley
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RedisKeyRequest implements Serializable {
    /**
     * The headers of http request
     */
    @SuppressWarnings({"all"})
    private Map<String, Object> headers;
    /**
     * The annotation of <code>@RateLimit</code>
     */
    private RateLimit rateLimit;
    /**
     * The http servlet request
     */
    @SuppressWarnings({"all"})
    private HttpServletRequest httpServletRequest;
    /**
     * The http rate limit properties
     */
    private HttpRateLimitProperties httpRateLimitProperties;
}
