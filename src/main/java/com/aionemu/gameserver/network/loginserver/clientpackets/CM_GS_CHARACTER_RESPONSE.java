package com.aionemu.gameserver.network.loginserver.clientpackets;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.network.loginserver.LsClientPacket;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_GS_CHARACTER;

/**
 * 登录服查询账号角色数量的请求包，游戏服以 SM_GS_CHARACTER 回复。
 * LoginServer request for the character count of an account; Gameserver replies with SM_GS_CHARACTER.
 *
 * @author cura
 */
public class CM_GS_CHARACTER_RESPONSE extends LsClientPacket {

	/**
	 * 构造函数。
	 * Constructor.
	 *
	 * @param opCode 操作码 opcode
	 */
	public CM_GS_CHARACTER_RESPONSE(int opCode) {
		super(opCode);
	}

	private int accountId;

	/**
	 * 读取待查询的账号 ID。
	 * Reads the account id to query.
	 */
	@Override
	public void readImpl() {
		accountId = readD();
	}

	/**
	 * 查询账号角色数并回复登录服。
	 * Queries character count for the account and replies to the login server.
	 */
	@Override
	public void runImpl() {
		int characterCount = DAOManager.getDAO(PlayerDAO.class).getCharacterCountOnAccount(accountId);
		sendPacket(new SM_GS_CHARACTER(accountId, characterCount));
	}
}
