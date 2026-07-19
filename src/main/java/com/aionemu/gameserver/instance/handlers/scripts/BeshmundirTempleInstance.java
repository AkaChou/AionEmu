package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

@InstanceID(300170000)
public class BeshmundirTempleInstance extends GeneralInstanceHandler {

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 730274 -> {
				if (instance.getNpc(799506) == null
						&& (canSummonRespondent(player, 30208, 182209610)
							|| canSummonRespondent(player, 30308, 182209710))) {
					spawn(799506, 1360, 390, 250, (byte) 183);
				}
			}
			case 730290 -> {
				if (player.getInventory().decreaseByItemId(185000091, 1)) {
					npc.getController().onDelete();
				} else {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403686));
				}
			}
		}
	}

	private static boolean canSummonRespondent(Player player, int questId, int itemId) {
		return canSummonRespondent(player.getQuestStateList().getQuestState(questId),
			player.getInventory().getItemCountByItemId(itemId));
	}

	static boolean canSummonRespondent(QuestState questState, long itemCount) {
		return questState != null && questState.getStatus() == QuestStatus.START && itemCount > 0;
	}

	private static void removeItems(Player player) {
		Storage storage = player.getInventory();
		for (int itemId = 185000091; itemId <= 185000096; itemId++) {
			storage.decreaseByItemId(itemId, storage.getItemCountByItemId(itemId));
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
}
