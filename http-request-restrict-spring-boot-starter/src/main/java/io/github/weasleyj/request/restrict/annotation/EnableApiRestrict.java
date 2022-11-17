package io.github.weasleyj.request.restrict.annotation;

import io.github.weasleyj.request.restrict.config.RequestRestrictProperties;
import io.github.weasleyj.request.restrict.config.RequestRestrictRedissonConfig;
import io.github.weasleyj.request.restrict.config.RequestRestrictWebMvcConfig;
import io.github.weasleyj.request.restrict.interceptor.DefaultRequestRestrictInterceptor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Api Restrict
 *
 * @author weasley
 * @version 1.0.0
 */
@Inherited
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RequestRestrictWebMvcConfig.class, RequestRestrictProperties.class,
        DefaultRequestRestrictInterceptor.class, RequestRestrictRedissonConfig.class,
})
public @interface EnableApiRestrict {
}
