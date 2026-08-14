package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：角色相关应答（携带目标账号 ID）。
 * LS→GS: character-related response (carries target account id).
 *
 * @author cura
 */
public class SM_GS_CHARACTER_RESPONSE extends GsServerPacket {

    /**
     * 目标账号 ID。
     * Target account id.
     */
    private final int accountId;

    /**
     * 构造角色应答包。
     * Constructs a character response packet.
     *
     * @param accountId 账号 ID / account id
     */
    public SM_GS_CHARACTER_RESPONSE(int accountId) {
        this.accountId = accountId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(8);
        writeD(accountId);
    }
}
