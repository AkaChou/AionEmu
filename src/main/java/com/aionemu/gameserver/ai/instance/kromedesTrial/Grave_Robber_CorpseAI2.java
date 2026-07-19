package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Grave Robber Corpse（@AIName "Grave_Robber_Corpse"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Grave Robber Corpse (@AIName "Grave_Robber_Corpse"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Grave_Robber_Corpse")
public class Grave_Robber_CorpseAI2 extends NpcAI2
{
	private static final int ELYOS_QUEST_ID = 18604;
	private static final int ASMODIAN_QUEST_ID = 28604;
	private static final int SILVER_BLADE_ROTAN_ID = 164000141;

    @Override
    protected void handleDialogStart(Player player) {
		QuestState elyosQuest = player.getQuestStateList().getQuestState(ELYOS_QUEST_ID);
		QuestState asmodianQuest = player.getQuestStateList().getQuestState(ASMODIAN_QUEST_ID);
		long itemCount = player.getInventory().getItemCountByItemId(SILVER_BLADE_ROTAN_ID);
		int dialogId = elyosQuest == null && asmodianQuest == null ? 1097 : itemCount > 0 ? 27 : 1011;
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId));
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestState elyosQuest = player.getQuestStateList().getQuestState(ELYOS_QUEST_ID);
		QuestState asmodianQuest = player.getQuestStateList().getQuestState(ASMODIAN_QUEST_ID);
		long itemCount = player.getInventory().getItemCountByItemId(SILVER_BLADE_ROTAN_ID);
		if (dialogId == 1012 && canRecover(elyosQuest, asmodianQuest, itemCount)
			&& ItemService.addItem(player, SILVER_BLADE_ROTAN_ID, 1) == 0) {
            // 你获得了强大物品。可从背包拖到快捷栏以便使用。 / You have obtained an object with great power. For quick access, drag the item from your Cube to your Quickbar.
            PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDCROMEDE_SKILL_01, 0);
			// 可用银刃罗坦摧毁通往神殿宝库的石门。 / You can use a Silver Blade Rotan to destroy the rock door leading to the Temple Vault.
			PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDCROMEDE_DOOR, 10000);
        }
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
        return true;
    }

	static boolean canRecover(QuestState elyosQuest, QuestState asmodianQuest, long itemCount) {
		return itemCount == 0 && (elyosQuest != null || asmodianQuest != null);
	}
}
