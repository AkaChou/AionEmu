package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 客户端装备重新鉴定结果确认请求包（接受/拒绝预览结果）。
 * Client packet answering the reidentify preview (accept or reject).
 * <p>对齐真端 {@code AnswerReidentifyPacket}：确认时把暂存值落盘，取消时清空暂存值。
 * Mirrors retail {@code AnswerReidentifyPacket}: confirm commits the staged values,
 * cancel discards them.</p>
 * @author Ghostfur (Aion-Unique)
 */
public class CM_TUNE_RESULT extends AionClientPacket {

	private int itemObjectId;
	private boolean hasAccepted;

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
		hasAccepted = readC() == 1;
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		Item item = player.getInventory().getItemByObjId(itemObjectId);
		if (item == null) {
			item = player.getEquipment().getEquippedItemByObjId(itemObjectId);
		}
		if (item == null || !item.hasPendingReidentify()) {
			return;
		}
		if (hasAccepted) {
			item.applyPendingReidentify();
			// 参数以 DescriptionId（物品 nameId）携带，对齐真端 AnswerReidentifyPacket 确认分支的 0x156436。
			// Parameter carried as a DescriptionId (item name id), mirroring retail's 0x156436 in AnswerReidentifyPacket.
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_APPLY_YES(new DescriptionId(item.getNameId())));
		} else {
			item.clearPendingReidentify();
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401911));
		}
		PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, item));
	}
}
