package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;

/**
 * 登录服请求踢出指定账号在线角色。
 * Login server request to kick an account's online character.
 *
 * @author -Nemesiss-
 */
public class CM_REQUEST_KICK_ACCOUNT extends LsClientPacket {

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_REQUEST_KICK_ACCOUNT(int opCode) {
		super(opCode);
	}

	/**
	 * 登录服要求踢下线的账号 ID。
	 * Account id that the login server requests to kick.
	 */
	private int accountId;

	/**
	 * 读取待踢账号 ID。
	 * Reads the account id to kick.
	 */
	@Override
	public void readImpl() {
		accountId = readD();
	}

	/**
	 * 通过 LoginServer 门面踢下该账号。
	 * Kicks the account via the LoginServer facade.
	 */
	@Override
	public void runImpl() {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().kickAccount(accountId);
	}
}
