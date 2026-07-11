package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 击杀当前可见 NPC 的操作（由玩家作为击杀者）。
 * Operation that kills the currently visible NPC with the player as killer.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KillOperation")
public class KillOperation extends QuestOperation {

	/**
	 * 若可见对象为 NPC，则触发其死亡处理。
	 * If the visible object is an NPC, triggers its death handling.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(QuestEnv env) {
		if (env.getVisibleObject() instanceof Npc) {
			((Npc) env.getVisibleObject()).getController().onDie(env.getPlayer());
		}
	}
}
