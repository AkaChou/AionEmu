package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端装备鉴定结果确认请求包（接受/拒绝重新鉴定结果）。
 * Client packet confirming an item re-identify/tune result (accept or reject).
 *
 * @author Ghostfur (Aion-Unique)
 */
public class CM_TUNE_RESULT extends AionClientPacket {

	private int itemObjectId;
	private int unk;
	private int accept;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TUNE_RESULT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		itemObjectId = readD();
		unk = readC();

		switch (unk) {
		case 0: {
			accept = 0;
			break;
		}
		case 1: {
			accept = 1;
			break;

		}
		default:
			break;
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (accept > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_APPLY_YES(
					player.getInventory().getItemByObjId(itemObjectId).getItemName()));
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401911));
		}
	}
}
