package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：心跳探测包。
 * LS→GS: keep-alive ping packet.
 *
 * @author KID
 */
public class SM_PING extends GsServerPacket {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(11);
    }
}
