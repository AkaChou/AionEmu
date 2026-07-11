package com.aionemu.loginserver.model;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * 账号时间数据：上次登录、会话时长、当日累计在线/休息时间等。
 * Account time data: last login, session duration, accumulated online/rest time today.
 *
 * @author EvilSpirit
 */
@Getter
@Setter
public class AccountTime {

    /**
     * 上次登录时间。
     * Last login time.
     */
    private Timestamp lastLoginTime;

    /**
     * 账号过期时间。
     * Account expiration time.
     */
    private Timestamp expirationTime;

    /**
     * 处罚结束时间。
     * Penalty end time.
     */
    private Timestamp penaltyEnd;

    /**
     * 当前会话时长（毫秒）。
     * Current session duration in milliseconds.
     */
    private long sessionDuration;

    /**
     * 累计在线时间（毫秒）。
     * Accumulated online time in milliseconds.
     */
    private long accumulatedOnlineTime;

    /**
     * 累计休息时间（毫秒）。
     * Accumulated rest time in milliseconds.
     */
    private long accumulatedRestTime;

    /**
     * 默认构造：将 lastLoginTime 设为当前时间。
     * Default constructor: sets lastLoginTime to now.
     */
    public AccountTime() {
        this.lastLoginTime = new Timestamp(System.currentTimeMillis());
    }

}
