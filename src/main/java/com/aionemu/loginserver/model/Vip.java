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

    public Vip(int accountId, int level, long experience) {
        this.accountId = accountId;
        this.level = level;
        this.experience = experience;
    }
}
