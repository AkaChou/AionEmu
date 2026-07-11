package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服通知登录服某账号已断开连接的服务端包。
 * Server packet used by the game server to inform the login server that an account is no longer online.
 *
 * @author -Nemesiss-
 */
public class SM_ACCOUNT_DISCONNECTED extends LsServerPacket {

	/**
	 * 已从游戏服下线的账号 ID。
	 * Account id that is no longer on the game server.
	 */
	private final int accountId;

	/**
	 * 构造账号断开通知包。
	 * Constructs a new account-disconnected packet.
	 *
	 * 账号 ID / account id
	 */
	public SM_ACCOUNT_DISCONNECTED(int accountId) {
		super(0x03);
		this.accountId = accountId;
	}

	/**
	 * 写入已断开连接的账号 ID。
	 * Writes the disconnected account id.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeD(accountId);
	}
}
