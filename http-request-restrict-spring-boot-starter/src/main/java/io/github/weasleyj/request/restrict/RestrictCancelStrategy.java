package io.github.weasleyj.request.restrict;

/**
 * HTTP请求取消限制策略
 * <p>
 * 注意事项
 * <ul>
 *     <li>首次访问满足条件不会触发限流</li>
 *     <li>再次访问‘不满足条件’且‘超过最大允许点击次数’会触发限流，Redis中的限流键过期自动删除，再次访问，如果满足条件，不会进入限流模式</li>
 *     <li>Redis中的限流键未过期，此时满足条件也会被限流</li>
 * </ul>
 *
 * @author weasley
 * @version 1.0.0
 * @apiNote 如果你不是太能够理解这个激进的功能，推荐你使用普通功能即可
 */
@FunctionalInterface
public interface RestrictCancelStrategy {
    /**
     * http请求完成以后取消限
     *
     * @apiNote true: 取消限制
     */
    boolean cancelRestrict();
}
