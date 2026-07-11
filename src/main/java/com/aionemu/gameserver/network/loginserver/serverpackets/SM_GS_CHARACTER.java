package com.aionemu.gameserver.network.loginserver.serverpackets;

import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;

/**
 * 游戏服向登录服回报指定账号角色数量的服务端包。
 * Server packet that reports a given account's character count to the login server.
 *
 * @author cura
 */
public class SM_GS_CHARACTER extends LsServerPacket {

	private int accountId;
	private int characterCount;

	/**
	 * 构造角色数量回报包。
	 * Constructs a character-count report packet.
	 *
	 * 账号 ID / account id
	 * character count
	 */
	public SM_GS_CHARACTER(final int accountId, final int characterCount) {
		super(0x08);
		this.accountId = accountId;
		this.characterCount = characterCount;
	}

	/**
	 * 写入账号 ID 与角色数量。
	 * Writes account id and character count.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeD(accountId);
		writeC(characterCount);
	}
}
