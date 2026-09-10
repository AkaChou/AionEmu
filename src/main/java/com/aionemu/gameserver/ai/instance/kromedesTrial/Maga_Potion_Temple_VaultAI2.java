package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.instance.handlers.scripts.KromedesTrialInstance;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Kromedes Trial 副本 NPC AI：Maga Potion Temple Vault（@AIName "maga_potion_1"），继承 NpcAI2。
 * Kromedes Trial instance NPC AI: Maga Potion Temple Vault (@AIName "maga_potion_1"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("maga_potion_1")
public class Maga_Potion_Temple_VaultAI2 extends NpcAI2
{
	private static final int QUEST_ID = 18602;
	private static final int RELIC_KEY_ID = 185000109;

	@Override
    protected void handleDialogStart(Player player) {
        QuestState questState = player.getQuestStateList().getQuestState(QUEST_ID);
        if (questState != null && questState.getStatus() == QuestStatus.START
                && questState.getQuestVarById(0) == 1) {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1352, QUEST_ID));
            return;
        }
        if (player.getInventory().getFirstItemByItemId(RELIC_KEY_ID) != null) { // 遗物钥匙 / Relic Key.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
    }
	
	@Override
	public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, resolveQuestId(player, questId), dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env)) {
			if (dialogId == QuestDialogAction.SETPRO2.id()
				&& getPosition().getWorldMapInstance().getInstanceHandler()
					instanceof KromedesTrialInstance instanceHandler) {
				instanceHandler.synchronizeRobstinNpc(player);
			}
			return true;
		}
		int instanceId = getPosition().getInstanceId();
		if (dialogId == QuestDialogAction.SETPRO1.id() || dialogId == QuestDialogAction.SETPRO2.id()) { // 遗物钥匙 / Relic Key.
		    switch (getNpcId()) {
			    case 730308: // 玛加药剂 / Maga's Potion.
					if (player.getInventory().getFirstItemByItemId(RELIC_KEY_ID) != null) {
						TeleportService2.teleportTo(player, 300230000, instanceId, 687.56116f, 681.68225f, 200.28648f, (byte) 30);
					}
				break;
			}
		} else if (dialogId == 1012) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1012));
		}
		return true;
	}

	/**
	 * 客户端有时不会把任务 ID 带回连续任务页；活动步骤明确时补回本 NPC 的任务 owner。
	 * The client may omit the quest ID on a follow-up page; restore this NPC's quest owner while its step is active.
	 */
	private int resolveQuestId(Player player, int questId) {
		if (questId != 0) {
			return questId;
		}
		QuestState questState = player.getQuestStateList().getQuestState(QUEST_ID);
		return questState != null && questState.getStatus() == QuestStatus.START
			&& questState.getQuestVarById(0) == 1 ? QUEST_ID : 0;
	}
}
