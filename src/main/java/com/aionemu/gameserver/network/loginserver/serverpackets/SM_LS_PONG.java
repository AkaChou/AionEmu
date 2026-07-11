package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服对登录服心跳 Ping 的应答 Pong 服务端包。
 * Server packet that replies with a Pong to a login-server Ping heartbeat.
 *
 * @author KID
 */
public class SM_LS_PONG extends LsServerPacket {
	private int pid;

	/**
	 * 构造登录服 Pong 应答包。
	 * Constructs a login-server Pong reply packet.
	 *
	 * @param pid 进程/心跳标识 / process or ping id
	 */
	public SM_LS_PONG(int pid) {
		super(12);
		this.pid = pid;
	}

	/**
	 * 写入游戏服 ID 与心跳标识。
	 * Writes game-server id and ping id.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeC(NetworkConfig.GAMESERVER_ID);
		writeD(pid);
	}
}
