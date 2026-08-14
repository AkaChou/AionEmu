package com.aionemu.chatserver.model;

/**
 * 服务器重启频率枚举。
 * Server restart frequency enumeration.
 *
 * @author nrg
 */
public enum RestartFrequency {

    /**
     * 从不自动重启 / Never restart automatically
     */
    NEVER(0),
    /**
     * 每日重启 / Daily restart
     */
    DAILY(1),
    /**
     * 每周重启 / Weekly restart
     */
    WEEKLY(2),
    /**
     * 每月重启 / Monthly restart
     */
    MONTHLY(3);

    /**
     * 频率 ID。
     * Frequency identifier.
     */
    private int id;

    /**
     * 构造重启频率枚举。
     * Constructs a restart frequency enum value.
     *
     * @param id 频率 ID / frequency id
     */
    private RestartFrequency(int id) {
        this.id = id;
    }

    /**
     * 获取频率 ID。
     * Returns the frequency id.
     *
     * @return 频率 ID / frequency id
     */
    public int getID() {
        return id;
    }
}
