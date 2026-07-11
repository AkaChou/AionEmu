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

    /**
     * 构造账号鉴权响应包。
     * Constructs an account authentication response packet.
     *
     * 账号 ID / account id
     * @param ok 是否鉴权通过 / whether authentication succeeded
     * account name
     * @param accessLevel 访问权限等级 / access level
     * membership level
     * @param toll 通行点数 / toll points
     * luna currency
     * return-player flag
     */
    public SM_ACCOUNT_AUTH_RESPONSE(int accountId, boolean ok, String accountName, byte accessLevel, byte membership, long toll, long luna, byte isReturn) {
        this.accountId = accountId;
        this.ok = ok;
        this.accountName = accountName;
        this.accessLevel = accessLevel;
        this.membership = membership;
        this.toll = toll;
        this.luna = luna;
        this.isReturn = isReturn;
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
        }
    }
}
