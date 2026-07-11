package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服发起账号会话校验的服务端包。
 * Server packet used by the game server to ask if a session key is valid on the login server.
 *
 * @author -Nemesiss-
 */
public class SM_ACCOUNT_AUTH extends LsServerPacket {

	/**
	 * 账号 ID（会话密钥片段）。
	 * Account id (part of the session key).
	 */
	private final int accountId;
	/**
	 * loginOk（会话密钥片段）。
	 * loginOk (part of the session key).
	 */
	private final int loginOk;
	/**
	 * playOk1（会话密钥片段）。
	 * playOk1 (part of the session key).
	 */
	private final int playOk1;
	/**
	 * playOk2（会话密钥片段）。
	 * playOk2 (part of the session key).
	 */
	private final int playOk2;

	/**
	 * 构造账号会话校验包。
	 * Constructs a new account session-auth packet.
	 *
	 * account identifier
	 * loginOk part
	 * playOk1 part
	 * playOk2 part
	 */
	public SM_ACCOUNT_AUTH(int accountId, int loginOk, int playOk1, int playOk2) {
		super(0x01);
		this.accountId = accountId;
		this.loginOk = loginOk;
		this.playOk1 = playOk1;
		this.playOk2 = playOk2;
	}

	/**
	 * 写入账号 ID 与会话密钥片段。
	 * Writes account id and session-key parts.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeD(accountId);
		writeD(loginOk);
		writeD(playOk1);
		writeD(playOk2);
	}
}
