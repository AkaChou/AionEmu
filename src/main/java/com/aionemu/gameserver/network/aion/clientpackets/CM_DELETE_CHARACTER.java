package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.dao.PlayerPasskeyDAO;
import com.aionemu.gameserver.model.account.CharacterPasskey.ConnectType;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHARACTER_SELECT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_CHARACTER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.player.PlayerService;

/**
 * 请求删除角色的客户端包。
 * Client packet requesting deletion of a character.
 *
 * @author -Nemesiss-
 */
public class CM_DELETE_CHARACTER extends AionClientPacket {

	/** PlayOk2未使用 / PlayOk2 (unused) */
	@SuppressWarnings("unused")
	private int playOk2;
	/** 待删除角色的对象 ID / object id of character to delete */
	private int chaOid;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_DELETE_CHARACTER(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		playOk2 = readD();
		chaOid = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		AionConnection client = getConnection();
		PlayerAccountData playerAccData = client.getAccount().getPlayerAccountData(chaOid);
		if (playerAccData != null && !playerAccData.isLegionMember()) {
			// 通行密钥检查 / passkey check
			if (SecurityConfig.PASSKEY_ENABLE && !client.getAccount().getCharacterPasskey().isPass()) {
				client.getAccount().getCharacterPasskey().setConnectType(ConnectType.DELETE);
				client.getAccount().getCharacterPasskey().setObjectId(chaOid);
				boolean isExistPasskey = DAOManager.getDAO(PlayerPasskeyDAO.class)
						.existCheckPlayerPasskey(client.getAccount().getId());

				if (!isExistPasskey) {
					client.sendPacket(new SM_CHARACTER_SELECT(0));
				} else {
					client.sendPacket(new SM_CHARACTER_SELECT(1));
				}
			} else {
				PlayerService.deletePlayer(playerAccData);
				client.sendPacket(new SM_DELETE_CHARACTER(chaOid, playerAccData.getDeletionTimeInSeconds()));
			}
		} else {
			client.sendPacket(SM_SYSTEM_MESSAGE.STR_GUILD_DISPERSE_STAYMODE_CANCEL_1);
		}
	}
}