package com.aionemu.gameserver.ai.instance.sauroSupplyBase;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Sauro Supply Base 副本 NPC AI：Hidden Passage（@AIName "hidden_passage"），继承 NpcAI2。
 * Sauro Supply Base instance NPC AI: Hidden Passage (@AIName "hidden_passage"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("hidden_passage")
public class Hidden_PassageAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000179) != null) { // Danuar Omphanium 钥匙 / Danuar Omphanium Key.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352));
        }
    }

	/**
	 * 处理对话选择：消耗钥匙把玩家传送到对应 Boss 的房间。
	 * Handles dialog selection: consumes keys to teleport the player to the matching boss room.
	 *
	 * @param player 对话玩家 / dialog player
	 * @param dialogId 对话框选项 ID / dialog option ID
	 * @param questId 任务 ID / quest ID
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @return 始终为 true / always true
	 */
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 20008 && player.getInventory().decreaseByItemId(185000179, 1)) { // Danuar Omphanium 钥匙 / Danuar Omphanium Key.
			TeleportService2.teleportTo(player, 301130000, instanceId, 689.85376f, 903.41785f, 411.45676f, (byte) 105); // 卫队长 Ahuradim 的房间 / Guard Captain Ahuradim's.
		} else if (dialogId == 20009 && player.getInventory().decreaseByItemId(185000179, 2)) { // Danuar Omphanium 钥匙 / Danuar Omphanium Key.
			TeleportService2.teleportTo(player, 301130000, instanceId, 886.4798f, 876.16693f, 411.45676f, (byte) 15); // 旅团长 Sheba 的房间 / The Brigade General Sheba's.
		}
		return true;
	}
}
