package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：账号重连密钥响应（对应 CM_ACCOUNT_RECONNECT_KEY）。
 * LS→GS: account reconnect key response (for CM_ACCOUNT_RECONNECT_KEY).
 *
 * @author -Nemesiss-
 */
public class SM_ACCOUNT_RECONNECT_KEY extends GsServerPacket {

    /**
     * 将要重连的账号 ID。
     * Account id of the account that will reconnect.
     */
    private final int accountId;
    /**
     * 用于鉴权的重连密钥。
     * Reconnect key used for authentication.
     */
    private final int reconnectKey;

    /**
     * 构造账号重连密钥响应包。
     * Constructs an account reconnect key response packet.
     *
     * @param accountId 账号 ID / account id
     * @param reconnectKey 重连密钥 / reconnect key
     */
    public SM_ACCOUNT_RECONNECT_KEY(int accountId, int reconnectKey) {
        this.accountId = accountId;
        this.reconnectKey = reconnectKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(3);
        writeD(accountId);
        writeD(reconnectKey);
    }
}
