package io.github.weasleyj.request.restrict.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Redis Version
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class RedisVersion implements Serializable {
    /**
     * Redis version，i.e: 7.0.5
     */
    private String version;
    /**
     * Redis integer version，i.e: 7
     */
    private int intVersion;
}
