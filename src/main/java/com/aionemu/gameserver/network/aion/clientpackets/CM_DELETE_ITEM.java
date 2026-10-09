package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 请求丢弃/销毁背包物品的客户端包。
 * Client packet requesting deletion of an inventory item.
 * @author Avol
 */
public class CM_DELETE_ITEM extends AionClientPacket {

	public int itemObjectId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_DELETE_ITEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		itemObjectId = readD();
	}

	@Override
	protected void runImpl() {

		Player player = getConnection().getActivePlayer();
		Storage inventory = player.getInventory();
		Item item = inventory.getItemByObjId(itemObjectId);

		if (item != null) {
			if (!item.getItemTemplate().isBreakable()) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_UNBREAKABLE_ITEM(new DescriptionId(item.getNameId())));
			} else {
				// 任务引用面（原版 `User_DestroyItem`）：进行中的任务引用该物品 ⇒ 由引擎发确认窗
				// （确认后放弃相关任务并删物品；不可放弃/拒绝/在途请求各有提示），物品暂不删。
				// The quest-reference gate (retail User_DestroyItem): the engine claims the destroy when
				// in-progress quests reference the item (confirm → stop the quests + delete).
				if (GameEngineServices.questEngine().tryQuestItemDestroy(player, item)) {
					return;
				}
				inventory.delete(item, ItemDeleteType.DISCARD);
			}
		}
	}
}