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
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 SimpleHunt 原生任务处理器（计划 §6.2 / P1 切换批）。
 * <p>
 * 完全基于真端表数据 {@link NativeQuestTableLoader} 与相机注册表 {@link CameraRegistry} 驱动，
 * 绝不生成 IR 节点图或通过旧编译器分派。
 * 状态基于 32 位 raw vars 与 ProgressCamera 双通道（0xf0 普通写入 / 0x100 推进写入）原子演进。
 * <p>
 * Retail SimpleHunt native quest handler (plan §6.2 / P1 switch batch).
 * Driven 100% by true-end table data and CameraRegistry; generates zero IR nodes
 * and bypasses legacy compiler overlays. Progress advances via 32-bit raw vars and
 * ProgressCamera dual-channel logic (0xf0 normal write / 0x100 advance write).
 */
public final class SimpleHuntHandler {

	/** 狩猎目标槽位引用：任务 ID + 槽位 (1..5)。 / Hunt target reference: quest id + slot. */
	public record HuntTargetRef(int questId, int slot) {
	}

	/** 过场引用：movie id + 触发动作 id（{@code cs1_haction}；-1 = 表未声明触发）。 */
	public record Cutscene(int movieId, int triggerAction) {
	}

	/** 获取拥有的任务数量。 / Returns managed quest count. */
	public int ownedQuestCount() {
		return ownedQuestIds.size();
	}

	private static volatile SimpleHuntHandler instance;

	private final NativeQuestTableLoader tableLoader;
	private final CameraRegistry cameraRegistry;
	private final NativeNpcNameResolver nameResolver;
	private final HtmlPagesRegistry pagesRegistry;
	/** 过场出口（真端交付节点 0x35 槽 PlayMovie）。 / The cutscene exit (retail hand-in slot 0x35). */
	private final NativeMoviePort moviePort;

	/** NPC ID → 监听该怪物的任务槽位集合。 / NPC ID → listening quest slots. */
	private final Map<Integer, List<HuntTargetRef>> targetsByNpcId;
	/** 任务 ID → 接取 NPC ID。 / Quest ID → acquire NPC ID. */
	private final Map<Integer, Integer> acquireNpcByQuestId;
	/** 任务 ID → 交付 NPC ID。 / Quest ID → reward NPC ID. */
	private final Map<Integer, Integer> rewardNpcByQuestId;
	/** 本处理器拥有的真端任务 ID 集合。 / Managed retail quest IDs. */
	private final Set<Integer> ownedQuestIds;
	/** 路由集 = 注册集 − XML-only 行（单一 owner 不变量）。 / Routing set = registration set minus XML-owned rows. */
	private final Set<Integer> routedQuestIds;

	/** 任务 ID → 链式接取窗的下一环（真端交付节点 0x1e 槽的 {@code con_quest}）。 / Quest id → the next quest of the chain window. */
	private final Map<Integer, Integer> conQuestByQuestId;
	/** 链式接取窗未闭环的行（fail-closed 证据面）。 / Rows whose chain window is not realized. */
	private final Set<Integer> unresolvedChainQuestIds;
	/** 任务 ID → 过场引用（表 {@code cutsceneid1}/{@code cs1_haction}）。 / Quest id → cutscene reference. */
	private final Map<Integer, Cutscene> cutsceneByQuestId;

	/** 完成/领奖口（计划 §6.2 NativeReportRewardFlow 完成半边）。 / The native completion/reward port. */
	private final NativeReportRewardFlow rewardFlow;

	private SimpleHuntHandler(NativeQuestTableLoader tableLoader, CameraRegistry cameraRegistry,
			NativeNpcNameResolver nameResolver, HtmlPagesRegistry pagesRegistry, Set<Integer> xmlOnlyIds) {
		this(tableLoader, cameraRegistry, nameResolver, pagesRegistry, xmlOnlyIds,
				NativeMoviePort.live(), NativeReportRewardFlow.instance());
	}

	SimpleHuntHandler(NativeQuestTableLoader tableLoader, CameraRegistry cameraRegistry,
			NativeNpcNameResolver nameResolver, HtmlPagesRegistry pagesRegistry, Set<Integer> xmlOnlyIds,
			NativeMoviePort moviePort, NativeReportRewardFlow rewardFlow) {
		this.tableLoader = tableLoader;
		this.cameraRegistry = cameraRegistry;
		this.nameResolver = nameResolver;
		this.pagesRegistry = pagesRegistry;
		this.moviePort = moviePort;
		this.rewardFlow = rewardFlow;

		Map<Integer, List<HuntTargetRef>> targets = new LinkedHashMap<>();
		Map<Integer, Integer> acquires = new LinkedHashMap<>();
		Map<Integer, Integer> rewards = new LinkedHashMap<>();
		Map<Integer, Integer> conQuests = new LinkedHashMap<>();
		Map<Integer, Cutscene> cutscenes = new LinkedHashMap<>();
		Set<Integer> unresolvedChain = new TreeSet<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();

		for (NativeQuestTableLoader.SimpleHuntRow row : tableLoader.rows()) {
			int qid = row.questId();
			owned.add(qid);
			if (xmlOnlyIds == null || !xmlOnlyIds.contains(qid)) {
				routed.add(qid);
			}

			// 接取 NPC 索引
			if (row.acquiredNpcName() != null && !row.acquiredNpcName().isBlank()) {
				NativeNpcNameResolver.Match m = nameResolver.resolve(row.acquiredNpcName());
				if (m.resolution() == NativeNpcNameResolver.Resolution.UNIQUE) {
					acquires.put(qid, m.npcIds().get(0));
				}
			}

			// 交付 NPC 索引
			if (row.rewardNpcName() != null && !row.rewardNpcName().isBlank()) {
				NativeNpcNameResolver.Match m = nameResolver.resolve(row.rewardNpcName());
				if (m.resolution() == NativeNpcNameResolver.Resolution.UNIQUE) {
					rewards.put(qid, m.npcIds().get(0));
				}
			}

			// 击杀目标索引
			for (Map.Entry<Integer, NativeQuestTableLoader.KillSlot> entry : row.killSlots().entrySet()) {
				int slot = entry.getKey();
				for (String monsterName : entry.getValue().monsters()) {
					List<Integer> candidateNpcIds = nameResolver.resolveMonsterIds(monsterName);
					for (int npcId : candidateNpcIds) {
						targets.computeIfAbsent(npcId, k -> new ArrayList<>())
								.add(new HuntTargetRef(qid, slot));
					}
				}
			}

			// 链式接取窗（真端 0x1e 槽）与过场（真端 0x35 槽）按原文装载，语义在构造尾与 onDialog 消费。
			// The chain window (slot 0x1e) and the cutscene (slot 0x35) load verbatim; their semantics are
			// consumed in the constructor tail and in onDialog.
			if (row.conQuest() != null) {
				conQuests.put(qid, row.conQuest());
			}
			if (row.cutsceneId() != null) {
				cutscenes.put(qid, new Cutscene(row.cutsceneId(),
						row.cutsceneAction() == null ? -1 : row.cutsceneAction()));
			}
		}

		// 真端 0x1e 槽（交付节点）：接续下一任务 {@code con_quest} 的接取窗。本车道接取路由按 NPC 建表，
		// 故该窗的等价物 = 「下一环的接取 NPC 恰是本行的交付 NPC」；本表内目标逐行验证，不闭环即登记
		// fail-closed 证据（跨族目标由逐行门按同一条不变量复算，不新增第二套路由）。
		// Retail slot 0x1e (on the hand-in node) opens the next quest's accept window. This lane keys accept
		// routes by NPC, so the equivalent is "the next quest acquires at this row's hand-in NPC"; in-table
		// targets are verified here and non-closing rows are recorded as fail-closed evidence.
		for (Map.Entry<Integer, Integer> entry : conQuests.entrySet()) {
			int questId = entry.getKey();
			int next = entry.getValue();
			Integer targetAcquire = acquires.get(next);
			Integer sourceReward = rewards.get(questId);
			if (routed.contains(next) && targetAcquire != null
					&& !targetAcquire.equals(sourceReward)) {
				unresolvedChain.add(questId);
			}
		}

		this.targetsByNpcId = Collections.unmodifiableMap(targets);
		this.acquireNpcByQuestId = Collections.unmodifiableMap(acquires);
		this.rewardNpcByQuestId = Collections.unmodifiableMap(rewards);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
		this.conQuestByQuestId = Collections.unmodifiableMap(conQuests);
		this.unresolvedChainQuestIds = Collections.unmodifiableSet(unresolvedChain);
		this.cutsceneByQuestId = Collections.unmodifiableMap(cutscenes);
	}

	public static SimpleHuntHandler instance() {
		SimpleHuntHandler local = instance;
		if (local == null) {
			synchronized (SimpleHuntHandler.class) {
				local = instance;
				if (local == null) {
					local = new SimpleHuntHandler(NativeQuestTableLoader.instance(),
							CameraRegistry.instance(),
							NativeNpcNameResolver.instance(),
							HtmlPagesRegistry.instance(),
							NativeQuestOwnerResolver.instance().xmlOnlyIds());
					instance = local;
				}
			}
		}
		return local;
	}

	/** 判断是否拥有该任务。 / Checks if this handler manages the quest. */
	public boolean owns(int questId) {
		return ownedQuestIds.contains(questId);
	}

	/**
	 * 判断该任务是否由 native 车道**路由**（注册集 − XML-only 行）。
	 * XML 定义仍在的真端表行由 XML 车道 owns，native 只装载不路由（单一 owner 不变量）。
	 * Whether the native lane routes this quest (registration set minus XML-owned rows).
	 * Retail table rows that still carry an XML definition stay owned by the XML lane.
	 */
	public boolean routes(int questId) {
		return routedQuestIds.contains(questId);
	}

	/** 路由集（不变量：与 XML-only 集交集为空）。 / The routing set (disjoint from the XML-owned set). */
	public Set<Integer> routedQuestIds() {
		return routedQuestIds;
	}

	/** 获取拥有的任务 ID 集合。 / Returns managed quest IDs. */
	public Set<Integer> ownedQuestIds() {
		return ownedQuestIds;
	}

	/** 获取任务起始 NPC ID。 / Returns the acquire NPC ID for the quest. */
	public Integer acquireNpc(int questId) {
		return acquireNpcByQuestId.get(questId);
	}

	/** 获取任务交付 NPC ID。 / Returns the reward NPC ID for the quest. */
	public Integer rewardNpc(int questId) {
		return rewardNpcByQuestId.get(questId);
	}

	/**
	 * 真端 {@code con_quest}（链式接取窗的下一环，交付节点 0x1e 槽）；未声明返回 null。
	 * <p>
	 * 本车道接取路由按 NPC 建表，故只要下一环的接取 NPC 等于本行的交付 NPC，该窗即已由下一环自身
	 * 那一行实现；{@link #unresolvedChainQuestIds()} 为空即全表闭环。
	 * The retail {@code con_quest} column (hand-in slot 0x1e); the window is realized by the next quest's
	 * own accept route whenever that row acquires at this row's hand-in NPC.
	 */
	public Integer conQuest(int questId) {
		return conQuestByQuestId.get(questId);
	}

	/** 链式接取窗未闭环的行（fail-closed 证据面）。 / Rows whose chain window is not realized. */
	public Set<Integer> unresolvedChainQuestIds() {
		return unresolvedChainQuestIds;
	}

	/** 过场引用（表未声明返回 null）。 / The cutscene reference (null when the row declares none). */
	public Cutscene cutscene(int questId) {
		return cutsceneByQuestId.get(questId);
	}

	/**
	 * 查询与指定 NPC 相关的 SimpleHunt 任务 ID 列表。
	 * Returns SimpleHunt quest IDs associated with the NPC.
	 */
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
		return result;
	}

	/**
	 * 在启动期将 SimpleHunt 涉及的 NPC 注册进 {@link QuestEngine} 索引。
	 * Registers NPC interests into {@link QuestEngine} at startup.
	 */
	public void installInterest(QuestEngine engine) {
		if (engine == null) {
			return;
		}
		for (Map.Entry<Integer, Integer> entry : acquireNpcByQuestId.entrySet()) {
			int qid = entry.getKey();
			if (!routedQuestIds.contains(qid)) {
				continue;
			}
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnQuestStart(qid);
			engine.registerQuestNpc(npcId).addOnTalkEvent(qid);
		}
		for (Map.Entry<Integer, Integer> entry : rewardNpcByQuestId.entrySet()) {
			int qid = entry.getKey();
			if (!routedQuestIds.contains(qid)) {
				continue;
			}
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnTalkEvent(qid);
		}
		for (Map.Entry<Integer, List<HuntTargetRef>> entry : targetsByNpcId.entrySet()) {
			int npcId = entry.getKey();
			for (HuntTargetRef ref : entry.getValue()) {
				if (!routedQuestIds.contains(ref.questId())) {
					continue;
				}
				engine.registerQuestNpc(npcId).addOnKillEvent(ref.questId());
			}
		}
	}

	/**
	 * 处理怪物击杀事件（真端相机推进）。
	 * Handles a monster kill event by advancing the retail progress camera.
	 *
	 * @param player 击杀玩家 / The killer player
	 * @param npcId  被击杀怪物 NPC ID / The killed NPC ID
	 * @return 是否有任务被处理推进 / true if any quest was advanced
	 */
	public boolean onKill(Player player, int npcId) {
		if (player == null || npcId <= 0) {
			return false;
		}
		List<HuntTargetRef> refs = targetsByNpcId.get(npcId);
		if (refs == null || refs.isEmpty()) {
			return false;
		}

		boolean handled = false;
		for (HuntTargetRef ref : refs) {
			if (!routes(ref.questId())) {
				// XML 定义仍在的行由 XML 车道 owns：native 只装载不推进（单一 owner 不变量）。
				continue;
			}
			QuestState qs = player.getQuestStateList().getQuestState(ref.questId());
			if (qs == null || qs.getStatus() != QuestStatus.START) {
				continue;
			}

			CameraRegistry.CameraRow row = cameraRegistry.require(ref.questId());
			int currentVars = qs.getQuestVars().getQuestVars();

			// 相机推进一步：三守卫检查 + 槽位加 1 + 满值推进检测
			ProgressCamera.Result result = ProgressCamera.advance(
					ProgressCamera.Status.START, currentVars, row, ref.slot(), true);

			if (result.outcome() == ProgressCamera.Outcome.NO_ACTION) {
				// 超杀或未达守卫条件：真端规范零动作
				continue;
			}

			qs.getQuestVars().setVar(result.newVars());
			if (result.outcome() == ProgressCamera.Outcome.ADVANCE_WRITE) {
				// 推进通道（真端 +0x100）：进入 REWARD 待领奖状态
				qs.setStatus(QuestStatus.REWARD);
			}

			qs.setPersistentState(PersistentState.UPDATE_REQUIRED);
			PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(ref.questId(), qs.getStatus(), qs.getQuestVars().getQuestVars()));
			handled = true;
		}

		return handled;
	}

	/**
	 * 处理 NPC 对话与翻页事件（接取 / 报告 / 交付）。
	 * Handles dialog and page progression events (accept / report / finish).
	 */
	public boolean onDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		boolean handled = handleDialog(env);
		if (handled) {
			playCutsceneIfTriggered(env.getPlayer(), env.getQuestId(), env.getDialogId());
		}
		return handled;
	}

	/**
	 * 真端过场（交付节点 0x35 槽 PlayMovie）：动作命中 {@code cs1_haction} 时经 {@link NativeMoviePort}
	 * 下发，是状态机之外的副作用（不推进节点、不建任务档）。
	 * Retail cutscene (hand-in slot 0x35): sent as a side effect when the client action matches
	 * cs1_haction; it never advances the node or creates quest state.
	 */
	private void playCutsceneIfTriggered(Player player, int questId, int dialogId) {
		Cutscene cutscene = cutsceneByQuestId.get(questId);
		if (cutscene != null && cutscene.triggerAction() == dialogId) {
			moviePort.play(player, cutscene.movieId());
		}
	}

	private boolean handleDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		if (!routes(questId)) {
			return false;
		}

		Npc npc = env.getVisibleObject() instanceof Npc n ? n : null;
		int npcId = npc != null ? npc.getNpcId() : 0;
		int targetObjectId = npc != null ? npc.getObjectId() : 0;
		int dialogId = env.getDialogId();

		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = qs != null ? qs.getStatus() : QuestStatus.NONE;

		// 1. 未接取状态：处理接取对话流
		if (status == QuestStatus.NONE || qs == null) {
			Integer acqNpc = acquireNpcByQuestId.get(questId);
			if (acqNpc != null && acqNpc == npcId) {
				if (dialogId == 31 || dialogId == 26) {
					// 接取入口页 = 客户端任务页声明的可渲染页（真端表无页列，见 QuestDialogContract#acceptEntryPage）。
					// The accept entry page is the page the client task HTML declares.
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId,
							QuestDialogContract.loadDefault().acceptEntryPage(questId), questId));
					return true;
				} else if (dialogId == QuestDialogPage.SELECT1_1.id()
						|| dialogId == QuestDialogPage.SELECT1_1_1.id()) {
					// select1 首屏翻页（真端 cab520 原样回发）；客户端未声明该页即 fail-closed。
					// select1 page turns (cab520 echoes them); undeclared pages fail closed.
					if (!QuestDialogContract.loadDefault().hasButtonPage(questId, dialogId)) {
						return false;
					}
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, dialogId, questId));
					return true;
				} else if (dialogId == 1002 || dialogId == 20000) {
					// 确认接取任务
					// 真端接取：条件判定 + 建档走 native 状态端口（不依赖 typed QuestTemplate）。
					if (NativeQuestStartPort.instance().start(player, questId).started()) {
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

		// 2. 进行中状态：检查中继对话或未完成提示
		if (status == QuestStatus.START) {
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
					if (rewardFlow.claim(env, rewardIndex).completed()) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 1008, questId));
						return true;
					}
				}
			}
			return false;
		}

		// 表声明的过场触发动作（真端把 movie 挂在页动作上；本族 3 行 = 1007 拒绝流页）：
		// 不推进状态，但被服务时 movie 由 onDialog 包装层下发。
		// The declared cutscene trigger action (this family's three rows use the 1007 refuse page):
		// it advances no state, and the wrapper sends the movie whenever it is served.
		Cutscene cutscene = cutsceneByQuestId.get(questId);
		if (cutscene != null && cutscene.triggerAction() == dialogId) {
			return true;
		}
		return false;
	}
}
