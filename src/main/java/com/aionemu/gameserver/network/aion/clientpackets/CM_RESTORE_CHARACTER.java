package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RESTORE_CHARACTER;
import com.aionemu.gameserver.services.player.PlayerService;

/**
 * 客户端取消角色删除（恢复角色）请求包。
 * Client packet for cancelling character deletion (restoring a character).
 *
 * @author -Nemesiss-
 */
public class CM_RESTORE_CHARACTER extends AionClientPacket {

	/**
	 * PlayOk2（忽略） / PlayOk2 - we dont care
	 */
	@SuppressWarnings("unused")
	private int playOk2;
	/**
	 * 取消删除的角色对象 ID / ObjectId of character that deletion should be canceled
	 */
	private int chaOid;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_RESTORE_CHARACTER(int opcode, State state, State... restStates) {
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
		Account account = getConnection().getAccount();
		PlayerAccountData pad = account.getPlayerAccountData(chaOid);

		boolean success = pad != null && PlayerService.cancelPlayerDeletion(pad);
		sendPacket(new SM_RESTORE_CHARACTER(chaOid, success));
	}
}
