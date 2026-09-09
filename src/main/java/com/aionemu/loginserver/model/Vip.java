package com.aionemu.loginserver.model;

/**
 * 账号 VIP 状态模型。
 * Independent account VIP state.
 *
 * @param level      客户端 VIP 阶段（1-6）。
 *                   Client VIP stage (1-6).
 * @param expireTime VIP 到期时间（Unix 秒）；小于等于当前时间即已过期。
 *                   Unix seconds when VIP ends; &lt;= now means expired.
 */
public record Vip(int accountId, int level, long experience, long expireTime) {

    /**
     * 等级大于 0 且未过到期时间时处于激活状态。
     * Active when level &gt; 0 and not past expire.
     * expireTime 为 0 表示永久（无到期时间）。
     * expire_time == 0 means permanent (no expiry set).
     */
    public boolean isActive(long nowUnixSeconds) {
        if (level <= 0) {
            return false;
        }
        return expireTime == 0 || expireTime > nowUnixSeconds;
    }
}
