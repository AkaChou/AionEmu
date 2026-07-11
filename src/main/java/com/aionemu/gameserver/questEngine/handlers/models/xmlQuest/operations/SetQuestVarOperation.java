package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 设置当前任务指定变量槽位并同步客户端的操作。
 * Operation that sets a quest-var slot on the current quest and syncs the client.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SetQuestVarOperation")
public class SetQuestVarOperation extends QuestOperation {

	/** 任务变量槽位 ID / Quest-var slot id */
	@XmlAttribute(name = "var_id", required = true)
	protected int varId;
	/** 写入的变量值 / Value to write */
	@XmlAttribute(required = true)
	protected int value;

	/**
	 * 写入任务变量并下发任务动作包。
	 * Writes the quest var and sends the quest-action packet.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(QuestEnv env) {
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null) {
			qs.getQuestVars().setVarById(varId, value);
			PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(questId, qs.getStatus(), qs.getQuestVars().getQuestVars()));
		}
	}
}
