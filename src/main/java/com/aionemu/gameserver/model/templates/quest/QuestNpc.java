package com.aionemu.gameserver.model.templates.quest;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.QuestEngine;
import lombok.Getter;

/**
 * 任务 NPC 模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@Slf4j
public class QuestNpc {

	/** 返回任务开始事件列表 / Returns the on-quest-start events. */
	@Getter
	private final List<Integer> onQuestStart;
	/** 返回击杀事件列表 / Returns the on-kill events */
	@Getter
	private final List<Integer> onKillEvent;
	/** 返回对话事件列表 / Returns the on-talk events */
	@Getter
	private final List<Integer> onTalkEvent;
	/** 返回攻击事件列表 / Returns the on-attack events. */
	@Getter
	private final List<Integer> onAttackEvent;
	/** 返回丢失目标事件列表 / Returns the on-lost-target events */
	@Getter
	private final List<Integer> onLostTargetEvent;
	/** 返回到达目标事件列表 / Returns the on-reach-target events */
	@Getter
	private final List<Integer> onReachTargetEvent;
	/** 返回入仇恨列表事件列表 / Returns the on-add-aggro-list events */
	@Getter
	private final List<Integer> onAddAggroListEvent;
	private final List<Integer> onAtDistanceEvent;
	/** 返回 NPC ID / Returns the npc id */
	@Getter
	private final int npcId;

	public QuestNpc(int npcId) {
		this.npcId = npcId;
		onQuestStart = new ArrayList<Integer>();
		onKillEvent = new ArrayList<Integer>();
		onTalkEvent = new ArrayList<Integer>();
		onAttackEvent = new ArrayList<Integer>();
		onLostTargetEvent = new ArrayList<Integer>();
		onReachTargetEvent = new ArrayList<Integer>();
		onAddAggroListEvent = new ArrayList<Integer>();
		onAtDistanceEvent = new ArrayList<Integer>();
	}

	private void registerCanAct(int questId, int npcId) {
		NpcTemplate template = DataManager.NPC_DATA.getNpcTemplate(npcId);
		if (template == null) {
			log.warn(I18n.get("log.70f31426a9c4", npcId, questId));
			return;
		}
		String aiName = DataManager.NPC_DATA.getNpcTemplate(npcId).getAi();
		if ("quest_use_item".equals(aiName)) {
			GameEngineServices.questEngine().registerCanAct(questId, npcId);
		}
	}

	/** 添加任务开始事件 / Adds an on-quest-start event */
	public void addOnQuestStart(int questId) {
		if (!onQuestStart.contains(questId)) {
			onQuestStart.add(questId);
		}
	}

	/** 添加攻击事件 / Adds an on-attack event */
	public void addOnAttackEvent(int questId) {
		if (!onAttackEvent.contains(questId)) {
			onAttackEvent.add(questId);
		}
	}

	/** 添加击杀事件 / Adds an on-kill event */
	public void addOnKillEvent(int questId) {
		if (!onKillEvent.contains(questId)) {
			onKillEvent.add(questId);
			registerCanAct(questId, npcId);
		}
	}

	/** 添加对话事件 / Adds an on-talk event */
	public void addOnTalkEvent(int questId) {
		if (!onTalkEvent.contains(questId)) {
			onTalkEvent.add(questId);
			registerCanAct(questId, npcId);
		}
	}

	/** 添加到达目标事件 / Adds an on-reach-target event */
	public void addOnReachTargetEvent(int questId) {
		if (!onReachTargetEvent.contains(questId)) {
			onReachTargetEvent.add(questId);
		}
	}

	/** 添加丢失目标事件 / Adds an on-lost-target event */
	public void addOnLostTargetEvent(int questId) {
		if (!onLostTargetEvent.contains(questId)) {
			onLostTargetEvent.add(questId);
		}
	}

	/** 添加入仇恨列表事件 / Adds an on-add-aggro-list event */
	public void addOnAddAggroListEvent(int questId) {
		if (!onAddAggroListEvent.contains(questId)) {
			onAddAggroListEvent.add(questId);
			registerCanAct(questId, npcId);
		}
	}

	/** 添加距离触发事件 / Adds an on-at-distance event */
	public void addOnAtDistanceEvent(int questId) {
		if (!onAtDistanceEvent.contains(questId)) {
			onAtDistanceEvent.add(questId);
			registerCanAct(questId, npcId);
		}
	}

	/** 返回距离触发事件列表 / Returns the on-distance events */
	public List<Integer> getOnDistanceEvent() {
		return onAtDistanceEvent;
	}
}
