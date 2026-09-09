package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;
import lombok.AllArgsConstructor;

/**
 * LS→GS：角色相关应答（携带目标账号 ID）。
 * LS→GS: character-related response (carries target account id).
 *
 * @author cura
 */
@AllArgsConstructor
public class SM_GS_CHARACTER_RESPONSE extends GsServerPacket {

    /**
     * 目标账号 ID。
     * Target account id.
     */
    private final int accountId;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(8);
        writeD(accountId);
    }
}
