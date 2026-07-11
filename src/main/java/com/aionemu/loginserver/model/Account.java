package com.aionemu.loginserver.model;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

/**
 * 账号模型。
 * Account model.
 *
 * @author SoulKeeper
 */
@Getter
@Setter
public class Account {

    /**
     * 账号 ID；未入库时为 null。
     * Account id; null if not yet stored in DB.
     */
    private Integer id;

    /**
     * 账号名。
     * Account name.
     */
    private String name;

    /**
     * 密码哈希。
     * Password hash.
     */
    private String passwordHash;

    /**
     * 权限等级：0 普通用户，&gt;0 GM。
     * Access level: 0 regular user, &gt;0 GM.
     */
    private byte accessLevel;

    /**
     * 会员等级（普通、高级等）。
     * Membership (regular, premium, etc.).
     */
    private byte membership;

    /**
     * 是否已激活。
     * Whether account is activated.
     */
    private byte activated;

    /**
     * 上次访问的服务器；-1 表示无。
     * Last visited server; -1 if none.
     */
    private byte lastServer;

    /**
     * 上次登录 IP；-1 表示无。
     * Last login IP; -1 if none.
     */
    private String lastIp;

    /**
     * 上次登录 MAC；无记录时为 xx-xx-xx-xx-xx-xx。
     * Last login MAC; xx-xx-xx-xx-xx-xx if none.
     */
    private String lastMac = "xx-xx-xx-xx-xx-xx";

    /**
     * 强制绑定的唯一允许 IP。
     * Forced IP allowed for this account.
     */
    private String ipForce;

    /**
     * 账号时间数据。
     * Account time data.
     */
    private AccountTime accountTime;

    /**
     * 回归标记。
     * Return flag.
     */
    private byte isReturn;

    /**
     * 回归活动结束时间。
     * Return event end time.
     */
    private Timestamp returnEnd;

    /**
     * 返回回归标记。
     * Returns return flag.
     *
     * Return flag
     */
    public byte getReturn() {
        return isReturn;
    }

    /**
     * 设置回归标记。
     * Sets return flag.
     *
     * Return flag
     */
    public void setReturn(byte isReturn) {
        this.isReturn = isReturn;
    }

    /**
     * 按账号名与密码哈希判断相等。
     * Equality based on name and password hash.
     *
     * @param o 另一对象 / Other object
     * @return 名称与密码哈希均相同则为 true / True if name and password hash match
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Account)) {
            return false;
        }

        Account account = (Account) o;

        // noinspection SimplifiableIfStatement
        if (name != null ? !name.equals(account.name) : account.name != null) {
            return false;
        }

        return !(passwordHash != null ? !passwordHash.equals(account.passwordHash) : account.passwordHash != null);

    }

    /**
     * 基于名称与密码哈希的哈希码。
     * Hash code based on name and password hash.
     *
     * Hash code
     */
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;

        result = 31 * result + (passwordHash != null ? passwordHash.hashCode() : 0);

        return result;
    }
}
