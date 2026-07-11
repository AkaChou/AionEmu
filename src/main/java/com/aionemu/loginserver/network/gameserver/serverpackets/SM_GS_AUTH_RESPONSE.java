package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.network.gameserver.GsAuthResponse;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;

/**
 * LS→GS：游戏服注册认证结果响应（对应 CM_GS_AUTH）。
 * LS→GS: game-server registration auth result response (for CM_GS_AUTH).
 *
 * @author -Nemesiss-
 */
public class SM_GS_AUTH_RESPONSE extends GsServerPacket {

    /**
     * 游戏服认证应答结果。
     * Response for gameserver authentication.
     */
    private final GsAuthResponse response;

    /**
     * 构造游戏服认证结果响应包。
     * Constructs a game-server authentication response packet.
     *
     * @param response 认证应答结果 / authentication response
     */
    public SM_GS_AUTH_RESPONSE(GsAuthResponse response) {
        this.response = response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(0);
        writeC(response.getResponseId());
        if (response.getResponseId() == 0) {
            writeC(GameServerTable.getGameServers().size());
        }
    }
}
