package com.aionemu.gameserver.questEngine.handlers.template;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.Monster;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.services.VortexService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 怪物猎杀任务模板：击杀配置怪物并按 6 位打包变量累计，完成后到结束 NPC 领奖；支持仇恨接取与入侵进图自动接取。
 * Monster-hunt quest template: kills configured monsters with 6-bit packed vars, turns in at end NPCs; supports aggro-start and invasion enter-world auto-start.
 */
public class MonsterHunt extends QuestHandler {
	/** 任务 ID / quest id */
	private final int questId;
	/** 起始 NPC ID 集合 / start NPC id set */
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	/** 结束 NPC ID 集合 / end NPC id set */
	private final Set<Integer> endNpcs = new HashSet<Integer>();
	/** 怪物配置到 NPC ID 集合的映射 / monster config to NPC id set map */
	private final Map<Monster, Set<Integer>> monsters;
	/** 起始对话页 ID，0 用默认 1011 / start dialog page, 0 uses default 1011 */
	private final int startDialog;
	/** 结束对话页 ID，0 用默认流程 / end dialog page, 0 uses default flow */
	private final int endDialog;
	/** 仇恨接取 NPC ID 集合 / aggro-start NPC id set */
	private final Set<Integer> aggroNpcs = new HashSet<Integer>();
	/** 入侵世界 ID，0 表示不启用 / invasion world id, 0 if disabled */
	private final int invasionWorldId;
	/** 是否击杀后立即进入奖励 / whether a kill immediately enters reward */
	private final boolean reward;

	/**
	 * 构造怪物猎杀任务处理器。
	 * Constructs a monster-hunt quest handler.
	 *
	 * quest id
	 * start NPC list
	 * @param endNpcIds 结束 NPC 列表，null 则复用起始 NPC / end NPC list, null reuses start NPCs
	 * @param monsters 怪物配置映射 / monster config map
	 * @param startDialog 起始对话页 / start dialog page
	 * @param endDialog 结束对话页 / end dialog page
	 * @param aggroNpcs 仇恨接取 NPC 列表，可为 null / aggro NPC list, may be null
	 * invasion world id
	 * @param reward 击杀后是否立即奖励 / whether kill immediately rewards
	 */
	public MonsterHunt(int questId, List<Integer> startNpcIds, List<Integer> endNpcIds, Map<Monster, Set<Integer>> monsters, int startDialog, int endDialog, List<Integer> aggroNpcs, int invasionWorld, boolean reward) {
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
		this.monsters = monsters;
		this.startDialog = startDialog;
		this.endDialog = endDialog;
		if (aggroNpcs != null) {
			this.aggroNpcs.addAll(aggroNpcs);
			this.aggroNpcs.remove(0);
		}
		this.invasionWorldId = invasionWorld;
		this.reward = reward;
	}

	/**
	 * 注册接取/交任务 NPC、击杀目标、仇恨列表与可选进图事件。
	 * Registers start/end NPCs, kill targets, aggro list and optional enter-world event.
	 */
	@Override
	public void register() {
		for (Integer startNpc : startNpcs) {
			qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
			qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
		}
		for (Set<Integer> monsterIds : monsters.values()) {
			for (Integer monsterId : monsterIds) {
				qe.registerQuestNpc(monsterId).addOnKillEvent(questId);
			}
		}
		for (Integer endNpc : endNpcs) {
			qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
		}
		for (Integer aggroNpc : aggroNpcs) {
			qe.registerQuestNpc(aggroNpc).addOnAddAggroListEvent(getQuestId());
		}
		if (invasionWorldId != 0) {
			qe.registerOnEnterWorld(questId);
		}
	}

	/**
	 * 处理接取、完成校验与奖励对话事件。
	 * Handles accept, completion-check and reward dialog events.
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
					return sendQuestDialog(env, startDialog != 0 ? startDialog : 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			for (Monster mi : monsters.keySet()) {
				int endVar = mi.getEndVar();
				int varId = mi.getVar();
				int total = 0;
				do {
					int currentVar = qs.getQuestVarById(varId);
					total += currentVar << ((varId - mi.getVar()) * 6);
					endVar >>= 6;
					varId++;
				} while (endVar > 0);
				if (mi.getEndVar() > total) {
					return false;
				}
			}
			if (endNpcs.contains(targetId)) {
				if (endDialog != 0) {
					switch (env.getDialog()) {
					case USE_OBJECT: {
						return sendQuestDialog(env, endDialog);
					}
					case SELECT_REWARD: {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestDialog(env, 5);
					}
					default:
						break;
					}
				} else {
					switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1352);
					}
					case SELECT_REWARD: {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestDialog(env, 5);
					}
					default:
						break;
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (endNpcs.contains(targetId)) {
				if (!aggroNpcs.isEmpty()) {
					switch (env.getDialog()) {
					case START_DIALOG:
					case USE_OBJECT:
						return sendQuestDialog(env, 10002);
					case SELECT_REWARD:
						return sendQuestDialog(env, 5);
					default:
						return sendQuestEndDialog(env);
					}
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}

	/**
	 * 处理击杀事件：按 6 位打包累加对应怪物变量，或在 reward 模式下直接进入奖励。
	 * Handles kill events: packs/increments monster vars with 6-bit packing, or enters reward immediately when reward mode is on.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the kill event was handled
	 */
	@Override
	public boolean onKillEvent(QuestEnv env) {
	Player player = env.getPlayer();
	QuestState qs = player.getQuestStateList().getQuestState(questId);
	if (qs != null && qs.getStatus() == QuestStatus.START) {
		for (Monster m : monsters.keySet()) {
			if (m.getNpcIds().contains(env.getTargetId())) {
				int endVar = m.getEndVar();
				int varId = m.getVar();
				int total = 0;
				do {
					int currentVar = qs.getQuestVarById(varId);
					total += currentVar << ((varId - m.getVar()) * 6);
					endVar >>= 6;
					varId++;
				} while (endVar > 0);
				total += 1;

				if (total <= m.getEndVar()) {
					if (reward) {
						qs.setQuestVarById(0, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
					} else {
						for (int varsUsed = m.getVar(); varsUsed < varId; varsUsed++) {
							int value = total & 0x3F;
							total >>= 6;
							qs.setQuestVarById(varsUsed, value);
						}
						updateQuestStatus(env);
					}
					return true;
				}
			}
		}
	}
	return false;
	}

	/**
	 * 处理加入仇恨列表事件：未接取时自动开始任务。
	 * Handles add-to-aggro-list events: auto-starts the quest when not yet accepted.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已启动任务 / whether the quest was started
	 */
	@Override
	public boolean onAddAggroListEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			QuestService.startQuest(env);
			return true;
		}
		return false;
	}

	/**
	 * 进入入侵世界时，若虚空/裂隙活跃则自动开始任务。
	 * On entering the invasion world, auto-starts when vortex/rift is active.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已启动任务 / whether the quest was started
	 */
	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		VortexLocation vortexLoc = GameLocationBootstrapServices.vortexService().getLocationByWorld(invasionWorldId);
		if (player.getWorldId() == invasionWorldId) {
			if ((qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat())) {
				if ((vortexLoc != null && vortexLoc.isActive()) || (searchOpenRift())) {
					return QuestService.startQuest(env);
				}
			}
		}
		return false;
	}

	/**
	 * 检查入侵世界是否存在已打开的裂隙。
	 * Checks whether any open rift exists for the invasion world.
	 *
	 * @return 是否存在打开的裂隙 / whether an open rift exists
	 */
	private boolean searchOpenRift() {
		for (RiftLocation loc : GameLocationBootstrapServices.riftService().getRiftLocations().values()) {
			if (loc.getWorldId() == invasionWorldId && loc.isOpened()) {
				return true;
			}
		}
		return false;
	}
}
