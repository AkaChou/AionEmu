package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.loginserver.GameServerTable;
import com.aionemu.loginserver.network.gameserver.GsClientPacket;

/**
 * GS→LS：游戏服心跳应答（pong）。
 * GS→LS: GameServer heartbeat pong.
 *
 * @author KID
 */
public class CM_GS_PONG extends GsClientPacket {

    /**
     * 游戏服 ID。
     * GameServer id.
     */
    private byte serverId;
    /**
     * 进程 ID。
     * Process id.
     */
    private int pid;

    /**
     * 读取 serverId 与 pid。
     * Reads serverId and pid.
     */
    @Override
    protected void readImpl() {
        serverId = (byte) readC();
        pid = readD();
    }

    /**
     * 通知 GameServerTable 收到 pong。
     * Notifies GameServerTable that pong was received.
     */
    @Override
    protected void runImpl() {
        GameServerTable.pong(serverId, pid);
    }
}
