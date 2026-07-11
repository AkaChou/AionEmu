package com.aionemu.loginserver.model.base;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * MAC 封禁条目。
 * Banned MAC entry.
 *
 * @author KID
 */
@Getter
public class BannedMacEntry {

    /**
     * MAC 地址。
     * MAC address.
     */
    private String mac;

    /**
     * 封禁备注。
     * Ban details.
     */
    @Setter
    private String details;

    /**
     * 封禁到期时间。
     * Ban end time.
     */
    private Timestamp timeEnd;

    /**
     * 按地址与到期毫秒构造。
     * Constructs entry from address and end time in millis.
     *
     * MAC address
     * @param newTime 到期时间（毫秒） / End time in millis
     */
    public BannedMacEntry(String address, long newTime) {
        this.mac = address;
        this.updateTime(newTime);
    }

    /**
     * 按地址、到期时间与备注构造。
     * Constructs entry from address, end timestamp and details.
     *
     * MAC address
     * @param time 到期时间 / End timestamp
     * Details
     */
    public BannedMacEntry(String address, Timestamp time, String details) {
        this.mac = address;
        this.timeEnd = time;
        this.details = details;
    }

    /**
     * 更新到期时间。
     * Updates ban end time.
     *
     * @param newTime 到期时间（毫秒） / End time in millis
     */
    public final void updateTime(long newTime) {
        this.timeEnd = new Timestamp(newTime);
    }

    /**
     * 返回到期时间（兼容旧 API）。
     * Returns end time (legacy API alias).
     *
     * End timestamp
     */
    public final Timestamp getTime() {
        return timeEnd;
    }

    /**
     * 判断封禁是否仍生效。
     * Checks whether the ban is still active.
     *
     * @return 若 still active 则为 true / True if still active
     */
    public final boolean isActive() {
        return timeEnd != null || timeEnd.getTime() > System.currentTimeMillis();
    }

    /**
     * 判断在指定时间点是否仍生效。
     * Checks whether the ban is still active until the given time.
     *
     * @param time 比较时间（毫秒） / Time in millis
     * @return 若 still active 则为 true / True if still active
     */
    public final boolean isActiveTill(long time) {
        return timeEnd != null || timeEnd.getTime() > time;
    }
}
