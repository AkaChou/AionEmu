package com.aionemu.gameserver.questEngine.handlers.template;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.services.VortexService;

/**
 * 指定世界击杀任务模板：在配置世界内击杀达到数量后领奖，可选入侵世界进图自动接取。
 * Kill-in-world quest template: count kills inside configured worlds for reward; optional auto-start on invasion world enter.
 */
public class KillInWorld extends QuestHandler {
	/** 任务 ID / quest id */
	private final int questId;
	/** 起始 NPC ID 集合 / start NPC id set */
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	/** 结束 NPC ID 集合 / end NPC id set */
	private final Set<Integer> endNpcs = new HashSet<Integer>();
	/** 计入击杀的世界 ID 集合 / world ids where kills count */
	private final Set<Integer> worldIds = new HashSet<Integer>();
	/** 需要击杀的数量 / required kill amount */
	private final int killAmount;
	/** 入侵世界 ID，0 表示不启用进图自动接取 / invasion world id, 0 disables enter-world auto-start */
	private final int invasionWorldId;
	/** 已进入奖励状态时的对话页 / reward-state dialog page */
	private final int rewardDialogId;

	/**
	 * 构造指定世界击杀任务处理器。
	 * Constructs a kill-in-world quest handler.
	 *
	 * quest id
	 * @param endNpcIds 结束 NPC 列表，null 则复用起始 NPC / end NPC list, null reuses start NPCs
	 * @param startNpcIds 起始 NPC 列表，可为 null / start NPC list, may be null
	 * world id list
	 * kill amount
	 * invasion world id
	 */
	public KillInWorld(int questId, List<Integer> endNpcIds, List<Integer> startNpcIds, List<Integer> worldIds,
			int killAmount, int invasionWorld, int rewardDialogId) {
		super(questId);
		if (startNpcIds != null) {
			this.startNpcs.addAll(startNpcIds);
			this.startNpcs.remove(0);
		}
		if (endNpcIds == null) {
			this.endNpcs.addAll(startNpcs);
		} else {
			this.endNpcs.addAll(endNpcIds);
			this.endNpcs.remove(0);
		}
		this.questId = questId;
		this.worldIds.addAll(worldIds);
		this.worldIds.remove(0);
		this.killAmount = killAmount;
		this.invasionWorldId = invasionWorld;
		this.rewardDialogId = rewardDialogId;
	}

	/**
	 * 注册接取/交任务 NPC、世界击杀监听及可选进图事件。
	 * Registers start/end NPCs, world-kill listeners and optional enter-world event.
	 */
	@Override
	public void register() {
		Iterator<Integer> iterator = startNpcs.iterator();
		while (iterator.hasNext()) {
			int startNpc = iterator.next();
			qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
			qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
		}
		iterator = endNpcs.iterator();
		while (iterator.hasNext()) {
			int endNpc = iterator.next();
			qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
		}
		iterator = worldIds.iterator();
		while (iterator.hasNext()) {
			int worldId = iterator.next();
			qe.registerOnKillInWorld(worldId, questId);
		}
		if (invasionWorldId != 0) {
			qe.registerOnEnterWorld(questId);
		}
	}

	/**
	 * 处理接取与奖励对话事件。
	 * Handles accept and reward dialog events.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (startNpcs.isEmpty() || startNpcs.contains(targetId)) {
				switch (dialog) {
				case START_DIALOG: {
					return sendQuestDialog(env, 4762);
				}
				case ACCEPT_QUEST: {
					return sendQuestStartDialog(env);
				}
				default: {
					return sendQuestStartDialog(env);
				}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (endNpcs.contains(targetId)) {
				if (rewardDialogId != 0 && dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, rewardDialogId);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	/**
	 * 进入入侵世界时，若虚空/裂隙处于活跃则自动开始任务。
	 * On entering the invasion world, auto-starts the quest when vortex/rift is active.
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

	/**
	 * 处理世界内击杀事件，累计到配置数量后进入奖励。
	 * Handles in-world kill events, advancing to reward once the configured amount is reached.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the kill event was handled
	 */
	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		return defaultOnKillRankedEvent(env, 0, killAmount, true);
	}
}
