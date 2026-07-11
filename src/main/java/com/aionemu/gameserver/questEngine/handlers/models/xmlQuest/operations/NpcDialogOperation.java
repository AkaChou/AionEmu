package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 向玩家打开 NPC 对话窗口的操作。
 * Operation that opens an NPC dialog window for the player.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NpcDialogOperation")
public class NpcDialogOperation extends QuestOperation {

	/** 对话窗口 ID / Dialog window id */
	@XmlAttribute(required = true)
	protected int id;
	/** 可选任务 ID；为空时使用当前任务 / Optional quest id; current quest when null */
	@XmlAttribute(name = "quest_id")
	protected Integer questId;

	/**
	 * 发送对话窗口包；任务 ID 为 0 时不附带任务。
	 * Sends the dialog-window packet; omits quest binding when quest id is 0.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(QuestEnv env) {
		Player player = env.getPlayer();
		VisibleObject obj = env.getVisibleObject();
		int qId = env.getQuestId();
		if (questId != null) {
			qId = questId;
		}
		if (qId == 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(obj.getObjectId(), id));
		} else {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(obj.getObjectId(), id, qId));
		}
	}
}
