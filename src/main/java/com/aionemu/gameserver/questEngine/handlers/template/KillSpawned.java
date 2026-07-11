package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.Monster;
import com.aionemu.gameserver.questEngine.handlers.models.SpawnedMonster;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

import com.aionemu.commons.utils.collections.IntArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 击杀召唤怪任务模板：与刷怪物体交互召唤目标，击杀配置怪物后推进变量并领奖。
 * Kill-spawned quest template: interact with spawner objects to summon targets, kill configured monsters, then claim reward.
 */
public class KillSpawned extends QuestHandler {
	/** 任务 ID / quest id */
	private final int questId;
	/** 起始 NPC ID 集合 / start NPC id set */
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	/** 结束 NPC ID 集合 / end NPC id set */
	private final Set<Integer> endNpcs = new HashSet<Integer>();
	/** 刷新配置：NPC ID 列表到 SpawnedMonster / spawn config: NPC id lists to SpawnedMonster */
	private final Map<List<Integer>, SpawnedMonster> spawnedMonsters;
	/** 刷怪物体 NPC ID 列表 / spawner object NPC ids */
	private IntArrayList spawnerObjects;

	/**
	 * 构造击杀召唤怪任务处理器。
	 * Constructs a kill-spawned quest handler.
	 *
	 * quest id
	 * start NPC list
	 * @param endNpcIds 结束 NPC 列表，null 则复用起始 NPC / end NPC list, null reuses start NPCs
	 * @param spawnedMonsters 召唤怪配置映射 / spawned monster config map
	 */
	public KillSpawned(int questId, List<Integer> startNpcIds, List<Integer> endNpcIds,
			Map<List<Integer>, SpawnedMonster> spawnedMonsters) {
		super(questId);
		this.questId = questId;
		this.startNpcs.addAll(startNpcIds);
		this.startNpcs.remove(0);
		if (endNpcIds == null) {
			this.endNpcs.addAll(startNpcs);
		} else {
			this.endNpcs.addAll(endNpcIds);
			this.endNpcs.remove(0);
		}
		this.spawnedMonsters = spawnedMonsters;
		this.spawnerObjects = new IntArrayList();
		for (SpawnedMonster m : spawnedMonsters.values()) {
			spawnerObjects.add(m.getSpawnerObject());
		}
	}

	/**
	 * 注册接取/对话 NPC、击杀目标与刷怪物体事件。
	 * Registers start/talk NPCs, kill targets and spawner object events.
	 */
	@Override
	public void register() {
		Iterator<Integer> iterator = startNpcs.iterator();
		while (iterator.hasNext()) {
			int startNpc = iterator.next();
			qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
			qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
		}
		for (List<Integer> spawnedMonsterIds : spawnedMonsters.keySet()) {
			iterator = spawnedMonsterIds.iterator();
			while (iterator.hasNext()) {
				int spawnedMonsterId = iterator.next();
				qe.registerQuestNpc(spawnedMonsterId).addOnKillEvent(questId);
			}
		}
		iterator = endNpcs.iterator();
		while (iterator.hasNext()) {
			int endNpc = iterator.next();
			qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
		}
		for (int i = 0; i < spawnerObjects.size(); i++) {
			qe.registerQuestNpc(spawnerObjects.get(i)).addOnTalkEvent(questId);
		}
	}

	/**
	 * 处理接取、刷怪物体交互与交任务对话。
	 * Handles accept, spawner-object interaction and turn-in dialogs.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (startNpcs.isEmpty() || startNpcs.contains(targetId)) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (spawnerObjects.contains(targetId)) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					int monsterId = 0;
					for (SpawnedMonster m : spawnedMonsters.values()) {
						if (m.getSpawnerObject() == targetId) {
							monsterId = m.getNpcIds().get(0);
							break;
						}
					}
					SpawnSearchResult searchResult = DataManager.SPAWNS_DATA2.getFirstSpawnByNpcId(player.getWorldId(),
							targetId);
					QuestService.addNewSpawn(player.getWorldId(), player.getInstanceId(), monsterId,
							searchResult.getSpot().getX(), searchResult.getSpot().getY(), searchResult.getSpot().getZ(),
							searchResult.getSpot().getHeading());
					return true;
				}
			} else {
				for (Monster mi : spawnedMonsters.values()) {
					if (mi.getEndVar() > qs.getQuestVarById(mi.getVar())) {
						return false;
					}
				}
				if (endNpcs.contains(targetId)) {
					if (env.getDialog() == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 10002);
					} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
						return sendQuestDialog(env, 5);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (endNpcs.contains(targetId)) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	/**
	 * 处理击杀召唤怪事件：推进对应任务变量，全部完成后进入奖励。
	 * Handles spawned-monster kill events: advances matching quest vars and enters reward when all complete.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the kill event was handled
	 */
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			for (SpawnedMonster m : spawnedMonsters.values()) {
				if (m.getNpcIds().contains(env.getTargetId())) {
					if (qs.getQuestVarById(m.getVar()) < m.getEndVar()) {
						qs.setQuestVarById(m.getVar(), qs.getQuestVarById(m.getVar()) + 1);
						for (Monster mi : spawnedMonsters.values()) {
							if (qs.getQuestVarById(mi.getVar()) < mi.getEndVar()) {
								updateQuestStatus(env);
								return true;
							}
						}
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return true;
					}
				}
			}
		}
		return false;
	}
}
