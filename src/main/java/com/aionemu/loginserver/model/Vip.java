package com.aionemu.loginserver.model;

import lombok.Getter;

/**
 * 账号 VIP 状态模型。
 * Independent account VIP state.
 */
@Getter
public final class Vip {

    private final int accountId;
    /**
     * 客户端 VIP 阶段（1-6）。
     * Client VIP stage (1-6).
     */
    private final int level;
    private final long experience;
    /**
     * VIP 到期时间（Unix 秒）；小于等于当前时间即已过期。
     * Unix seconds when VIP ends; &lt;= now means expired.
     */
    private final long expireTime;

    public Vip(int accountId, int level, long experience, long expireTime) {
        this.accountId = accountId;
        this.level = level;
        this.experience = experience;
        this.expireTime = expireTime;
    }

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
