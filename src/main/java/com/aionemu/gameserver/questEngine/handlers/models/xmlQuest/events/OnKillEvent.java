package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.events;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.handlers.models.Monster;
import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations.QuestOperations;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 击杀事件：按怪物配置推进任务变量，并在完成后执行操作。
 * On-kill event: advances quest vars from monster config and runs complete operations when done.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OnKillEvent", propOrder = { "monster", "complite" })
public class OnKillEvent extends QuestEvent {

	/** 可击杀怪物配置列表 / Configured killable monster list */
	@XmlElement(name = "monster")
	protected List<Monster> monster;
	/** 全部目标完成后执行的操作（字段名保持 XML 历史拼写） / Operations when all targets are done (field name keeps XML spelling) */
	protected QuestOperations complite;

	/**
	 * 返回怪物配置的实时列表（JAXB 可变列表）。
	 * Returns the live monster config list (JAXB live list).
	 *
	 * Monster list
	 */
	public List<Monster> getMonsters() {
		if (monster == null) {
			monster = new ArrayList<Monster>();
		}
		return this.monster;
	}

	/**
	 * 处理击杀：匹配怪物并递增变量，再尝试执行完成操作。
	 * Handles a kill: matches monsters, increments vars, then tries complete operations.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 当前实现恒返回 false / Always returns false in the current implementation
	 */
	public boolean operate(QuestEnv env) {
		if (monster == null || !(env.getVisibleObject() instanceof Npc)) {
			return false;
		}
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(env.getQuestId());
		if (qs == null) {
			return false;
		}
		Npc npc = (Npc) env.getVisibleObject();
		for (Monster m : monster) {
			if (m.getNpcIds().contains(npc.getNpcId())) {
				int var = qs.getQuestVarById(m.getVar());
				if (var >= (m.getStartVar() == null ? 0 : m.getStartVar()) && var < m.getEndVar()) {
					qs.setQuestVarById(m.getVar(), var + 1);
					PacketSendUtility.sendPacket(env.getPlayer(),
							new SM_QUEST_ACTION(env.getQuestId(), qs.getStatus(), qs.getQuestVars().getQuestVars()));
				}
			}
		}

		if (complite != null) {
			for (Monster m : monster) {
				if (qs.getQuestVarById(m.getVar()) != qs.getQuestVarById(m.getVar())) {
					return false;
				}
			}
			complite.operate(env);
		}
		return false;
	}
}
