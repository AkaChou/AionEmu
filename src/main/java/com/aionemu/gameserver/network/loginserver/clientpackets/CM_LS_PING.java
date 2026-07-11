package com.aionemu.gameserver.network.loginserver.clientpackets;

import java.lang.management.ManagementFactory;

import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_LS_PONG;

/**
 * 登录服心跳探测包，游戏服以当前进程 PID 回复 SM_LS_PONG。
 * LoginServer ping packet; Gameserver replies with SM_LS_PONG carrying the current process PID.
 *
 * @author KID
 */
public class CM_LS_PING extends LsClientPacket {
	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_LS_PING(int opCode) {
		super(opCode);
	}

	/**
	 * 无载荷，仅作为触发。
	 * No payload; used as a trigger only.
	 */
	@Override
	protected void readImpl() {
		// 触发 / trigger
	}

	/**
	 * 解析本进程 PID 并通过登录服发送 SM_LS_PONG。
	 * Resolves this process PID and sends SM_LS_PONG via the login server.
	 */
	@Override
	protected void runImpl() {
		int pid = -1;
		try {
			pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		} catch (Exception ex) {
		}

		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_LS_PONG(pid));
	}
}
