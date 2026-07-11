package com.aionemu.loginserver.model;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * 封禁 IP 模型。
 * Banned IP model.
 *
 * @author SoulKeeper
 */
@Getter
@Setter
public class BannedIP {

    /**
     * 封禁记录 ID。
     * Ban record id.
     */
    private Integer id;

    /**
     * IP 或掩码。
     * IP or mask.
     */
    private String mask;

    /**
     * 封禁到期时间；null 表示永久。
     * Ban expiration time; null means permanent.
     */
    private Timestamp timeEnd;

    /**
     * 判断封禁是否仍生效。
     * Checks whether the ban is still active.
     *
     * @return 若 ban is still active 则为 true / True if ban is still active
     */
    public boolean isActive() {
        return timeEnd == null || timeEnd.getTime() > System.currentTimeMillis();
    }

    /**
     * 基于 {@link #mask} 判断相等。
     * Equality based on {@link #mask}.
     *
     * @param o 另一对象 / Other object
     * @return 若 masks are equal 则为 true / True if masks are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BannedIP)) {
            return false;
        }

        BannedIP bannedIP = (BannedIP) o;

        return !(mask != null ? !mask.equals(bannedIP.mask) : bannedIP.mask != null);
    }

    /**
     * 基于 mask 的哈希码。
     * Hash code based on mask.
     *
     * Hash code
     */
    @Override
    public int hashCode() {
        return mask != null ? mask.hashCode() : 0;
    }
}
