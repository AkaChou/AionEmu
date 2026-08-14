package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.model.AccountTime;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：账号鉴权结果响应（含账号名、在线/休息时间、权限与货币信息）。
 * LS→GS: account authentication result response (account name, online/rest time, access and currency).
 *
 * @author -Nemesiss-
 */
public class SM_ACCOUNT_AUTH_RESPONSE extends GsServerPacket {

    /**
     * 账号 ID。
     * Account id.
     */
    private final int accountId;
    /**
     * 是否鉴权通过。
     * True if the account is authenticated.
     */
    private final boolean ok;
    /**
     * 账号名。
     * Account name.
     */
    private final String accountName;
    /**
     * 访问权限等级。
     * Access level.
     */
    private final byte accessLevel;
    /**
     * 会员等级。
     * Membership level.
     */
    private final byte membership;
    /**
     * 通行点数（Toll）。
     * Toll points.
     */
    private final long toll;
    /**
     * 露娜币（Luna）。
     * Luna currency.
     */
    private final long luna;
    /**
     * 回流标记。
     * Return-player flag.
     */
    private final byte isReturn;
    private final int vipLevel;
    private final long vipExp;
    /** Unix seconds VIP end time. */
    private final long vipExpireTime;

    /**
     * 构造账号鉴权响应包。
     * Constructs an account authentication response packet.
     *
     * @param accountId 账号 ID / account id
     * @param ok 是否鉴权通过 / whether authentication succeeded
     * @param accountName 账号名 / account name
     * @param accessLevel 访问权限等级 / access level
     * @param membership 会员等级 / membership level
     * @param toll 通行点数 / toll points
     * @param luna 露娜货币 / luna currency
     * @param isReturn 返回玩家标志 / return-player flag
     */
    public SM_ACCOUNT_AUTH_RESPONSE(int accountId, boolean ok, String accountName, byte accessLevel, byte membership,
            long toll, long luna, byte isReturn, int vipLevel, long vipExp, long vipExpireTime) {
        this.accountId = accountId;
        this.ok = ok;
        this.accountName = accountName;
        this.accessLevel = accessLevel;
        this.membership = membership;
        this.toll = toll;
        this.luna = luna;
        this.isReturn = isReturn;
        this.vipLevel = vipLevel;
        this.vipExp = vipExp;
        this.vipExpireTime = vipExpireTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(1);
        writeD(accountId);
        writeC(ok ? 1 : 0);

        if (ok) {
            writeS(accountName);

            AccountTime accountTime = con.getGameServerInfo().getAccountFromGameServer(accountId).getAccountTime();

            writeQ(accountTime.getAccumulatedOnlineTime());
            writeQ(accountTime.getAccumulatedRestTime());
            writeC(accessLevel);
            writeC(membership);
            writeQ(toll);
            writeQ(luna);
            writeC(isReturn);
            writeC(vipLevel);
            writeQ(vipExp);
            writeQ(vipExpireTime);
        }
    }
}
