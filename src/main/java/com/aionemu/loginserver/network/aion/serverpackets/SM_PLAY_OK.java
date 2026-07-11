package com.aionemu.loginserver.network.aion.serverpackets;

import com.aionemu.loginserver.network.aion.AionServerPacket;
import com.aionemu.loginserver.network.aion.LoginConnection;
import com.aionemu.loginserver.network.aion.SessionKey;

/**
 * 登录服→客户端：允许进入指定游戏服，下发 playOk 与服务器 ID。
 * LoginServer → client: allow enter selected game server with playOk tokens and server id.
 *
 * @author -Nemesiss-
 */
public class SM_PLAY_OK extends AionServerPacket {

    /**
     * 会话密钥 playOk1，游戏服侧校验用。
     * playOk1 session-key part, verified on the game-server side.
     */
    private final int playOk1;
    /**
     * 会话密钥 playOk2，游戏服侧校验用。
     * playOk2 session-key part, verified on the game-server side.
     */
    private final int playOk2;
    /**
     * 目标游戏服 ID。
     * Target game-server id.
     */
    private int serverId;

    /**
     * 构造 SM_PLAY_OK 包。
     * Constructs a new SM_PLAY_OK packet.
     *
     * @param key 会话密钥 / session key
     * game server id
     */
    public SM_PLAY_OK(SessionKey key, byte serverId) {
        super(0x07);
        this.playOk1 = key.playOk1;
        this.playOk2 = key.playOk2;
        this.serverId = serverId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(LoginConnection con) {
        writeD(playOk1);
        writeD(playOk2);
        writeC(serverId);
    	writeB(new byte[14]);
    }
}
