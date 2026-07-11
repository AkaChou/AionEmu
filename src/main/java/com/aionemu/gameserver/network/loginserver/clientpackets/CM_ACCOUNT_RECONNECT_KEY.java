package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;

/**
 * 登录服对 SM_ACCOUNT_RECONNECT_KEY 的响应包，返回账号与重连密钥。
 * LoginServer response for SM_ACCOUNT_RECONNECT_KEY with account id and reconnection key.
 *
 * @author -Nemesiss-
 */
public class CM_ACCOUNT_RECONNECT_KEY extends LsClientPacket {

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_ACCOUNT_RECONNECT_KEY(int opCode) {
		super(opCode);
	}

	/**
	 * 将要重连的账号 ID。
	 * Account id of the account that will reconnect.
	 */
	private int accountId;
	/**
	 * 用于鉴权的重连密钥。
	 * Reconnection key used for authentication.
	 */
	private int reconnectKey;

	/**
	 * 读取账号 ID 与重连密钥。
	 * Reads account id and reconnection key.
	 */
	@Override
	public void readImpl() {
		accountId = readD();
		reconnectKey = readD();
	}

	/**
	 * 将重连密钥转交 LoginServer 门面处理。
	 * Forwards the reconnection key to the LoginServer facade.
	 */
	@Override
	public void runImpl() {
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().authReconnectionResponse(accountId, reconnectKey);
	}
}
