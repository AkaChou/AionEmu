package com.aionemu.loginserver.model;

import lombok.Getter;

/**
 * Independent account VIP state.
 */
@Getter
public final class Vip {

    private final int accountId;
    /** Client VIP stage (1-6). */
    private final int level;
    private final long experience;
    /** Unix seconds when VIP ends; &lt;= now means expired. */
    private final long expireTime;

    public Vip(int accountId, int level, long experience, long expireTime) {
        this.accountId = accountId;
        this.level = level;
        this.experience = experience;
        this.expireTime = expireTime;
    }

    /**
     * Active when level &gt; 0 and not past expire.
     * expire_time == 0 means permanent (no expiry set).
     */
    public boolean isActive(long nowUnixSeconds) {
        if (level <= 0) {
            return false;
        }
        return expireTime == 0 || expireTime > nowUnixSeconds;
    }
}
