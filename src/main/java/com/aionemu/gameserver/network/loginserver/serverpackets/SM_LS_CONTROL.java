package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服下发账号控制指令的服务端包。
 * Server packet that sends account-control commands from the game server to the login server.
 *
 * @author Aionchs-Wylovech
 */
public class SM_LS_CONTROL extends LsServerPacket {

	private final String accountName;

	private final String adminName;

	private final String playerName;

	private final int param;

	private final int type;

	/**
	 * 构造登录服账号控制包。
	 * Constructs a login-server account control packet.
	 *
	 * account name
	 * player name
	 * admin name
	 * @param param 控制参数 / control parameter
	 * @param type 控制类型 / control type
	 */
	public SM_LS_CONTROL(String accountName, String playerName, String adminName, int param, int type) {
		super(0x05);
		this.accountName = accountName;
		this.param = param;
		this.playerName = playerName;
		this.adminName = adminName;
		this.type = type;
	}

	/**
	 * 写入控制类型、管理员、账号、角色与参数。
	 * Writes control type, admin name, account name, player name and parameter.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeC(type);
		writeS(adminName);
		writeS(accountName);
		writeS(playerName);
		writeC(param);
	}
}
