package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;
import lombok.AllArgsConstructor;

/**
 * LS→GS：请求游戏服踢下线指定账号。
 * LS→GS: request the game server to kick the given account.
 * @author -Nemesiss-
 */
@AllArgsConstructor
public class SM_REQUEST_KICK_ACCOUNT extends GsServerPacket {

    /**
     * 需在游戏服侧踢下线的账号 ID。
     * Account that must be kicked on the game server.
     */
    private final int accountId;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(2);
        writeD(accountId);
    }
}
