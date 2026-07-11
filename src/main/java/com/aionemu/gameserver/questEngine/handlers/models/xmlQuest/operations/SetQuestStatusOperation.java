package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 设置当前任务状态并同步客户端的操作。
 * Operation that sets the current quest status and syncs the client.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SetQuestStatusOperation")
public class SetQuestStatusOperation extends QuestOperation {

	/** 目标任务状态 / Target quest status */
	@XmlAttribute(required = true)
	protected QuestStatus status;

	/**
	 * 更新任务状态、下发任务动作包；完成时刷新区域与附近任务。
	 * Updates quest status, sends the quest-action packet; refreshes zone and nearby quests on complete.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(QuestEnv env) {
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null) {
			qs.setStatus(status);
			PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(questId, qs.getStatus(), qs.getQuestVars().getQuestVars()));
			if (qs.getStatus() == QuestStatus.COMPLETE) {
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
			}
		}
	}
}
