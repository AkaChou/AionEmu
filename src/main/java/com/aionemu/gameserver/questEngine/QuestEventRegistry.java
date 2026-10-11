package com.aionemu.gameserver.questEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.model.templates.quest.QuestNpc;

/**
 * 任务事件注册表：集中维护 NPC、物品、区域等兴趣面索引，供引擎注册与查询。
 * Quest event registry: central store of the NPC, item and zone interest indexes used by the
 * engine for registration and lookup.
 * <p>本类只做注册与查询，不做任何事件分发；分发语义仍由 {@link QuestEngine} 门面持有。</p>
 * <p>This store only registers and looks up; dispatch semantics stay with
 * the {@link QuestEngine} facade.</p>
 */
final class QuestEventRegistry {

	/** NPC 关联任务索引 / NPC-related quest index */
	final IntObjectHashMap<QuestNpc> questNpcs = new IntObjectHashMap<>();
	/** 物品使用关联任务 / Item-use related quests */
	final IntObjectHashMap<IntArrayList> questItemRelated = new IntObjectHashMap<>();
	/** 获得物品关联任务 / Item-obtain related quests */
	final IntObjectHashMap<IntArrayList> questItems = new IntObjectHashMap<>();
	/** 区域任务结束监听列表 / Zone-mission-end listeners */
	final IntArrayList questOnEnterZoneMissionEnd = new IntArrayList();
	/** 死亡监听列表 / Death listeners */
	final IntArrayList questOnDie = new IntArrayList();
	/** 登出监听列表 / Logout listeners */
	final IntArrayList questOnLogOut = new IntArrayList();
	/** 穿过飞行环监听 / Pass-flying-ring listeners */
	final Map<String, IntArrayList> questOnPassFlyingRings = new LinkedHashMap<>();
	/** 动画结束监听 / Movie-end listeners */
	final IntObjectHashMap<IntArrayList> questOnMovieEnd = new IntObjectHashMap<>();
	/** 隐形计时器结束监听 / Invisible-timer-end listeners */
	final List<Integer> onInvisibleTimerEnd = new ArrayList<>();
	/** 装备物品监听 / Equip-item listeners */
	final Map<Integer, Set<Integer>> questOnEquipItem = new HashMap<>();
	/** 可行动作监听 / Can-act listeners */
	final IntObjectHashMap<IntArrayList> questCanAct = new IntObjectHashMap<>();
	/** 跟随到达目标监听 / Reach-target listeners */
	final IntArrayList reachTarget = new IntArrayList();
	/** 跟随丢失目标监听 / Lost-target listeners */
	final IntArrayList lostTarget = new IntArrayList();
	/** 进入风道监听 / Enter-windstream listeners */
	final IntArrayList questOnEnterWindStream = new IntArrayList();

	/**
	 * 注册（或获取）NPC 的任务关联对象。
	 * Register (or obtain) the quest association for an NPC.
	 * NPC 模板 ID / NPC template id
	 * QuestNpc association
	 */
	public QuestNpc registerQuestNpc(int npcId) {
		if (!questNpcs.containsKey(npcId)) {
			questNpcs.put(npcId, new QuestNpc(npcId));
		}
		return questNpcs.get(npcId);
	}

	/**
	 * 注册物品使用关联任务。
	 * Register a quest for item-use events.
	 * Item id
	 * Quest id
	 */
	public void registerQuestItem(int itemId, int questId) {
		if (!questItemRelated.containsKey(itemId)) {
			IntArrayList itemRelatedQuests = new IntArrayList();
			itemRelatedQuests.add(questId);
			questItemRelated.put(itemId, itemRelatedQuests);
		} else {
			questItemRelated.get(itemId).add(questId);
		}
	}

	/**
	 * 注册获得物品关联任务。
	 * Register a quest for item-obtain events.
	 * Item id
	 * Quest id
	 */
	public void registerGetingItem(int itemId, int questId) {
		if (!questItems.containsKey(itemId)) {
			IntArrayList questItemsToReg = new IntArrayList();
			questItemsToReg.add(questId);
			questItems.put(itemId, questItemsToReg);
		} else {
			questItems.get(itemId).add(questId);
		}
	}

	/**
	 * 注册区域任务结束监听。
	 * Register a zone-mission-end listener.
	 * Quest id
	 */
	public void registerOnEnterZoneMissionEnd(int questId) {
		if (!questOnEnterZoneMissionEnd.contains(questId)) {
			questOnEnterZoneMissionEnd.add(questId);
		}
	}

	/**
	 * 注册死亡监听。
	 * Register a death listener.
	 * Quest id
	 */
	public void registerOnDie(int questId) {
		if (!questOnDie.contains(questId)) {
			questOnDie.add(questId);
		}
	}

	/**
	 * 注册登出监听。
	 * Register a logout listener.
	 * Quest id
	 */
	public void registerOnLogOut(int questId) {
		if (!questOnLogOut.contains(questId)) {
			questOnLogOut.add(questId);
		}
	}

	/**
	 * 注册穿过飞行环监听。
	 * Register a pass-flying-ring listener.
	 * @param flyingRing 飞行环标识 / Flying-ring key
	 * Quest id
	 */
	public void registerOnPassFlyingRings(String flyingRing, int questId) {
		if (!questOnPassFlyingRings.containsKey(flyingRing)) {
			IntArrayList onPassFlyingRingsQuests = new IntArrayList();
			onPassFlyingRingsQuests.add(questId);
			questOnPassFlyingRings.put(flyingRing, onPassFlyingRingsQuests);
		} else {
			questOnPassFlyingRings.get(flyingRing).add(questId);
		}
	}

	/**
	 * 注册动画结束监听。
	 * Register a movie-end listener.
	 * Movie id
	 * Quest id
	 */
	public void registerOnMovieEndQuest(int moveId, int questId) {
		if (!questOnMovieEnd.containsKey(moveId)) {
			IntArrayList onMovieEndQuests = new IntArrayList();
			onMovieEndQuests.add(questId);
			questOnMovieEnd.put(moveId, onMovieEndQuests);
		} else {
			questOnMovieEnd.get(moveId).add(questId);
		}
	}

	/**
	 * 注册隐形计时器结束监听。
	 * Register an invisible-timer-end listener.
	 * Quest id
	 */
	public void registerOnInvisibleTimerEnd(int questId) {
		if (!onInvisibleTimerEnd.contains(questId)) {
			onInvisibleTimerEnd.add(questId);
		}
	}

	/**
	 * 注册装备物品监听。
	 * Register an equip-item listener.
	 * Item id
	 * Quest id
	 */
	public void registerOnEquipItem(int itemId, int questId) {
		if (!questOnEquipItem.containsKey(itemId)) {
			Set<Integer> questIds = new HashSet<>();
			questIds.add(questId);
			questOnEquipItem.put(itemId, questIds);
		} else {
			questOnEquipItem.get(itemId).add(questId);
		}
	}

	/**
	 * 注册可行动作监听。
	 * Register a can-act listener for a template.
	 * Quest id
	 * Template id
	 */
	public void registerCanAct(int questId, int templateId) {
		if (!questCanAct.containsKey(templateId)) {
			IntArrayList questSkills = new IntArrayList();
			questSkills.add(questId);
			questCanAct.put(templateId, questSkills);
		} else {
			questCanAct.get(templateId).add(questId);
		}
	}

	/**
	 * 注册进入风道监听。
	 * Register an enter-windstream listener.
	 * Quest id
	 */
	public void registerOnEnterWindStream(int questId) {
		if (!questOnEnterWindStream.contains(questId))
			questOnEnterWindStream.add(questId);
	}

	/**
	 * 注册跟随到达目标监听。
	 * Register a reach-target listener.
	 * Quest id
	 */
	public void registerAddOnReachTargetEvent(int questId) {
		if (!reachTarget.contains(questId))
			reachTarget.add(questId);
	}

	/**
	 * 注册跟随丢失目标监听。
	 * Register a lost-target listener.
	 * Quest id
	 */
	public void registerAddOnLostTargetEvent(int questId) {
		if (!lostTarget.contains(questId))
			lostTarget.add(questId);
	}

	/**
	 * 获取 NPC 的任务关联对象；未注册时返回空壳。
	 * Return the quest association for an NPC, or an empty shell if unregistered.
	 * NPC 模板 ID / NPC template id
	 * QuestNpc association
	 */
	public QuestNpc getQuestNpc(int npcId) {
		if (questNpcs.containsKey(npcId)) {
			return questNpcs.get(npcId);
		}
		return new QuestNpc(npcId);
	}

	/**
	 * 查询穿过飞行环关联任务。
	 * Look up quests related to passing a flying ring.
	 * @param flyingRing 飞行环标识 / Flying-ring key
	 * Quest id list
	 */
	IntArrayList getOnPassFlyingRingsQuests(String flyingRing) {
		if (questOnPassFlyingRings.containsKey(flyingRing)) {
			return questOnPassFlyingRings.get(flyingRing);
		}
		return new IntArrayList();
	}

	/**
	 * 清空所有事件注册表。
	 * Clear every event registry.
	 */
	void clear() {
		questNpcs.clear();
		questItemRelated.clear();
		questItems.clear();
		questOnEnterZoneMissionEnd.clear();
		questOnDie.clear();
		questOnLogOut.clear();
		questOnMovieEnd.clear();
		questOnPassFlyingRings.clear();
		onInvisibleTimerEnd.clear();
		questOnEquipItem.clear();
		questCanAct.clear();
		reachTarget.clear();
		lostTarget.clear();
		questOnEnterWindStream.clear();
	}
}
