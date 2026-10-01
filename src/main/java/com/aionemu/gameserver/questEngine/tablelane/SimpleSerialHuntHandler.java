package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 SimpleSerialHunt 原生任务处理器（计划 §6.2 / P2 切换批）。
 * <p>
 * 完全基于真端表数据 {@link NativeQuestTableLoader} 与相机注册表 {@link CameraRegistry} 驱动，
 * 绝不生成 IR 节点图或通过旧编译器分派。
 * 串行猎杀阶段规则：仅当前激活阶段（首个未满阶段）的击杀才推进（+0xf0），乱序或已满阶段击杀零副作用；
 * 简报规则：声明了 talk_npc 的任务在接取时置简报守卫位 0x40000000（1 << 30），向简报 NPC 对话确认后清零，
 * 方可开启 Stage 1 击杀。整行全部阶段满值后走推进通道（0x100）进入 REWARD 状态。
 * <p>
 * Retail SimpleSerialHunt native quest handler (plan §6.2 / P2 switch batch).
 * Driven 100% by true-end table data and CameraRegistry; generates zero IR nodes.
 * Progress advances strictly sequentially (only the first unfinished stage advances on kill;
 * out-of-order kills have zero side effects). Briefing gates (talk_npc) set guard bit 1<<30
 * on accept and clear it upon briefing dialog before stage 1 can count.
 */
public final class SimpleSerialHuntHandler {

	/** 狩猎目标槽位引用：任务 ID + 阶段/槽位 (1..5)。 / Hunt target reference: quest id + slot. */
	public record HuntTargetRef(int questId, int slot) {
	}

	private static volatile SimpleSerialHuntHandler instance;

	private final NativeQuestTableLoader tableLoader;
	private final CameraRegistry cameraRegistry;
	private final NativeNpcNameResolver nameResolver;

	/** NPC ID → 监听该怪物的任务槽位集合。 / NPC ID → listening quest slots. */
	private final Map<Integer, List<HuntTargetRef>> targetsByNpcId;
	/** 任务 ID → 接取 NPC ID。 / Quest ID → acquire NPC ID. */
	private final Map<Integer, Integer> acquireNpcByQuestId;
	/** 任务 ID → 交付 NPC ID。 / Quest ID → reward NPC ID. */
	private final Map<Integer, Integer> rewardNpcByQuestId;
	/** 任务 ID → 简报 NPC ID 集合。 / Quest ID → briefing NPC IDs. */
	private final Map<Integer, Set<Integer>> briefingNpcsByQuestId;
	/** 任务 ID → 阶段列表。 / Quest ID → stages list. */
	private final Map<Integer, List<NativeQuestTableLoader.SerialStage>> stagesByQuestId;
	/** 任务 ID 集合。 / Managed quest IDs. */
	private final Set<Integer> managedQuestIds;

	private SimpleSerialHuntHandler(NativeQuestTableLoader tableLoader, CameraRegistry cameraRegistry,
			NativeNpcNameResolver nameResolver) {
		this.tableLoader = tableLoader;
		this.cameraRegistry = cameraRegistry;
		this.nameResolver = nameResolver;

		Map<Integer, List<HuntTargetRef>> targets = new LinkedHashMap<>();
		Map<Integer, Integer> acquireNpcs = new LinkedHashMap<>();
		Map<Integer, Integer> rewardNpcs = new LinkedHashMap<>();
		Map<Integer, Set<Integer>> briefingNpcs = new LinkedHashMap<>();
		Map<Integer, List<NativeQuestTableLoader.SerialStage>> stagesMap = new LinkedHashMap<>();
		Set<Integer> questIds = new TreeSet<>();

		for (NativeQuestTableLoader.SimpleSerialHuntRow row : tableLoader.serialHuntRows()) {
			int questId = row.questId();
			questIds.add(questId);
			stagesMap.put(questId, row.stages());

			if (row.acquiredNpcName() != null && !row.acquiredNpcName().isBlank()) {
				acquireNpcs.put(questId, nameResolver.uniqueId(row.acquiredNpcName()));
			}
			if (row.rewardNpcName() != null && !row.rewardNpcName().isBlank()) {
				rewardNpcs.put(questId, nameResolver.uniqueId(row.rewardNpcName()));
			}
			if (row.talkNpcNames() != null && !row.talkNpcNames().isEmpty()) {
				Set<Integer> talkIds = new TreeSet<>();
				for (String talkName : row.talkNpcNames()) {
					talkIds.add(nameResolver.uniqueId(talkName));
				}
				briefingNpcs.put(questId, Collections.unmodifiableSet(talkIds));
			}

			for (NativeQuestTableLoader.SerialStage stage : row.stages()) {
				int slot = stage.stage();
				for (String monsterName : stage.monsters()) {
					List<Integer> npcIds = nameResolver.resolveMonsterIds(monsterName);
					if (npcIds.isEmpty()) {
						throw new IllegalStateException("NATIVE_NAME_UNRESOLVED: monster name '"
								+ monsterName + "' in serial quest " + questId);
					}
					for (int npcId : npcIds) {
						targets.computeIfAbsent(npcId, k -> new ArrayList<>())
								.add(new HuntTargetRef(questId, slot));
					}
				}
			}
		}

		this.targetsByNpcId = Collections.unmodifiableMap(targets);
		this.acquireNpcByQuestId = Collections.unmodifiableMap(acquireNpcs);
		this.rewardNpcByQuestId = Collections.unmodifiableMap(rewardNpcs);
		this.briefingNpcsByQuestId = Collections.unmodifiableMap(briefingNpcs);
		this.stagesByQuestId = Collections.unmodifiableMap(stagesMap);
		this.managedQuestIds = Collections.unmodifiableSet(questIds);
	}

	public static SimpleSerialHuntHandler instance() {
		SimpleSerialHuntHandler local = instance;
		if (local == null) {
			synchronized (SimpleSerialHuntHandler.class) {
				local = instance;
				if (local == null) {
					local = new SimpleSerialHuntHandler(
							NativeQuestTableLoader.instance(),
							CameraRegistry.instance(),
							NativeNpcNameResolver.instance());
					instance = local;
				}
			}
		}
		return local;
	}

	public static void ensureLoaded() {
		instance();
	}

	public boolean owns(int questId) {
		return managedQuestIds.contains(questId);
	}

	public Set<Integer> ownedQuestIds() {
		return managedQuestIds;
	}

	public int ownedQuestCount() {
		return managedQuestIds.size();
	}

	public Integer acquireNpc(int questId) {
		return acquireNpcByQuestId.get(questId);
	}

	public Integer rewardNpc(int questId) {
		return rewardNpcByQuestId.get(questId);
	}

	public List<Integer> questsForNpc(int npcId) {
		if (npcId <= 0) {
			return Collections.emptyList();
		}
		List<Integer> result = new ArrayList<>();
		for (Map.Entry<Integer, Integer> e : acquireNpcByQuestId.entrySet()) {
			if (e.getValue() == npcId) {
				result.add(e.getKey());
			}
		}
		for (Map.Entry<Integer, Integer> e : rewardNpcByQuestId.entrySet()) {
			if (e.getValue() == npcId && !result.contains(e.getKey())) {
				result.add(e.getKey());
			}
		}
		for (Map.Entry<Integer, Set<Integer>> e : briefingNpcsByQuestId.entrySet()) {
			if (e.getValue().contains(npcId) && !result.contains(e.getKey())) {
				result.add(e.getKey());
			}
		}
		return result;
	}

	public void installInterest(QuestEngine engine) {
		if (engine == null) {
			return;
		}
		for (Map.Entry<Integer, Integer> entry : acquireNpcByQuestId.entrySet()) {
			int qid = entry.getKey();
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnQuestStart(qid);
			engine.registerQuestNpc(npcId).addOnTalkEvent(qid);
		}
		for (Map.Entry<Integer, Integer> entry : rewardNpcByQuestId.entrySet()) {
			int qid = entry.getKey();
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnTalkEvent(qid);
		}
		for (Map.Entry<Integer, Set<Integer>> entry : briefingNpcsByQuestId.entrySet()) {
			int qid = entry.getKey();
			for (int talkId : entry.getValue()) {
				engine.registerQuestNpc(talkId).addOnTalkEvent(qid);
			}
		}
		for (Map.Entry<Integer, List<HuntTargetRef>> entry : targetsByNpcId.entrySet()) {
			int npcId = entry.getKey();
			for (HuntTargetRef ref : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnKillEvent(ref.questId());
			}
		}
	}

	public boolean onKill(QuestEnv env) {
		if (env == null || env.getPlayer() == null || !(env.getVisibleObject() instanceof Npc npc)) {
			return false;
		}
		return onKill(env.getPlayer(), npc.getNpcId());
	}

	public boolean onKill(Player player, int npcId) {
		if (player == null || npcId <= 0) {
			return false;
		}
		List<HuntTargetRef> targets = targetsByNpcId.get(npcId);
		if (targets == null || targets.isEmpty()) {
			return false;
		}
		boolean handled = false;
		for (HuntTargetRef ref : targets) {
			int questId = ref.questId();
			QuestState state = player.getQuestStateList().getQuestState(questId);
			if (state == null || state.getStatus() != QuestStatus.START) {
				continue;
			}
			int currentVars = state.getQuestVars().getQuestVars();
			if (!RawQuestVarsCodec.guardClear(currentVars)) {
				// 简报尚未完成，或处于守卫态，拒绝击杀推进
				continue;
			}
			CameraRegistry.CameraRow row = cameraRegistry.require(questId);
			List<NativeQuestTableLoader.SerialStage> stages = stagesByQuestId.get(questId);
			if (stages == null || stages.isEmpty()) {
				continue;
			}
			// 确定当前激活阶段（首个未满阶段）
			int activeSlot = -1;
			for (NativeQuestTableLoader.SerialStage stage : stages) {
				int s = stage.stage();
				int val = RawQuestVarsCodec.slotValue(row.width(), currentVars, s);
				if (val < stage.count()) {
					activeSlot = s;
					break;
				}
			}
			// 乱序击杀或已全部完成：不动作
			if (activeSlot == -1 || ref.slot() != activeSlot) {
				continue;
			}
			ProgressCamera.Result result = ProgressCamera.advance(ProgressCamera.Status.START,
					currentVars, row, activeSlot, true);
			if (result.outcome() == ProgressCamera.Outcome.NORMAL_WRITE) {
				state.getQuestVars().setVar(result.newVars());
				state.setPersistentState(PersistentState.UPDATE_REQUIRED);
				PacketSendUtility.sendPacket(player,
						new SM_QUEST_ACTION(questId, QuestStatus.START, result.newVars()));
				handled = true;
			} else if (result.outcome() == ProgressCamera.Outcome.ADVANCE_WRITE) {
				state.getQuestVars().setVar(result.newVars());
				state.setStatus(QuestStatus.REWARD);
				state.setPersistentState(PersistentState.UPDATE_REQUIRED);
				PacketSendUtility.sendPacket(player,
						new SM_QUEST_ACTION(questId, QuestStatus.REWARD, result.newVars()));
				handled = true;
			}
		}
		return handled;
	}

	public boolean onDialog(QuestEnv env) {
		Player player = env.getPlayer();
		if (player == null) {
			return false;
		}
		int questId = env.getQuestId();
		if (!managedQuestIds.contains(questId)) {
			return false;
		}
		Npc target = (Npc) env.getVisibleObject();
		if (target == null) {
			return false;
		}
		int npcId = target.getNpcId();
		int dialogId = env.getDialogId();
		int targetObjectId = target.getObjectId();
		QuestState state = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state != null ? state.getStatus() : QuestStatus.NONE;

		// 1. 未接取状态：在起始 NPC 处接取
		if (status == QuestStatus.NONE) {
			Integer acqNpc = acquireNpcByQuestId.get(questId);
			if (acqNpc != null && acqNpc == npcId) {
				if (dialogId == 26 || dialogId == 31 || dialogId == 1007 || dialogId == -1) {
					// 打开任务描述与接受窗口 (select1 / page 4)
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 4, questId));
					return true;
				} else if (dialogId == 1002 || dialogId == 20000) {
					// 确认接取任务
					if (QuestService.startQuest(env)) {
						Set<Integer> briefingNpcs = briefingNpcsByQuestId.get(questId);
						if (briefingNpcs != null && !briefingNpcs.isEmpty()) {
							QuestState qs = player.getQuestStateList().getQuestState(questId);
							if (qs != null) {
								qs.getQuestVars().setVar(0x40000000);
								PacketSendUtility.sendPacket(player,
										new SM_QUEST_ACTION(questId, QuestStatus.START, 0x40000000));
							}
						}
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 1003, questId));
						return true;
					}
				} else if (dialogId == 1003 || dialogId == 1004 || dialogId == 20001) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 1004, questId));
					return true;
				}
			}
			return false;
		}

		// 2. 进行中状态：简报 NPC 对话或未完成提示
		if (status == QuestStatus.START) {
			Set<Integer> briefingNpcs = briefingNpcsByQuestId.get(questId);
			if (briefingNpcs != null && briefingNpcs.contains(npcId)) {
				int vars = state.getQuestVars().getQuestVars();
				if (!RawQuestVarsCodec.guardClear(vars)) {
					if (dialogId == 26 || dialogId == 31 || dialogId == -1) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 10, questId));
						return true;
					} else if (dialogId == 10000 || dialogId == 1003 || dialogId == 10001 || dialogId == 31
							|| dialogId == 20000) {
						state.getQuestVars().setVar(0);
						state.setPersistentState(PersistentState.UPDATE_REQUIRED);
						PacketSendUtility.sendPacket(player,
								new SM_QUEST_ACTION(questId, QuestStatus.START, 0));
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 10000, questId));
						return true;
					}
				}
			}
			Integer rewNpc = rewardNpcByQuestId.get(questId);
			if (rewNpc != null && rewNpc == npcId) {
				if (dialogId == 31 || dialogId == 26) {
					// 尚未完成杀怪：常规未完成对话提示 (page 10)
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 10, questId));
					return true;
				}
			}
			return false;
		}

		// 3. 待交付状态 (REWARD)：在交付 NPC 处领取奖励
		if (status == QuestStatus.REWARD) {
			Integer rewNpc = rewardNpcByQuestId.get(questId);
			if (rewNpc != null && rewNpc == npcId) {
				if (dialogId == 31 || dialogId == 26 || dialogId == 1009 || dialogId == -1) {
					// 展示奖励选择窗口 (select_quest_reward1 / page 5)
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 5, questId));
					return true;
				} else if ((dialogId >= 8 && dialogId <= 23) || dialogId == 108 || (dialogId >= 110 && dialogId <= 124)) {
					// 结算奖励并完成任务
					int rewardIndex = (dialogId >= 8 && dialogId <= 23) ? (dialogId - 8) : 0;
					if (QuestService.finishQuest(env, rewardIndex)) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 1008, questId));
						return true;
					}
				}
			}
			return false;
		}

		return false;
	}
}
