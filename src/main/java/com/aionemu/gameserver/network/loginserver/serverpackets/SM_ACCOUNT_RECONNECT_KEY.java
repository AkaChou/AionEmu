package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服请求登录服为玩家快速重连签发密钥的服务端包。
 * Server packet sent when a player requests a fast reconnect; the login server replies with a reconnect key.
 *
 * @author -Nemesiss-
 */
public class SM_ACCOUNT_RECONNECT_KEY extends LsServerPacket {

	/**
	 * 请求重连登录服的账号 ID。
	 * Account id of the client that requested reconnection to the login server.
	 */
	private final int accountId;

	/**
	 * 构造账号重连密钥请求包。
	 * Constructs a new account reconnect-key request packet.
	 *
	 * account identifier
	 */
	public SM_ACCOUNT_RECONNECT_KEY(int accountId) {
		super(0x02);
		this.accountId = accountId;
	}

	/**
	 * 写入请求重连的账号 ID。
	 * Writes the account id requesting reconnection.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeD(accountId);
	}
}
