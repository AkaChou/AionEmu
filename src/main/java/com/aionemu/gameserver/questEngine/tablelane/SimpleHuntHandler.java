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
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
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
public final class SimpleHuntHandler implements NativeSystemGrantLane {

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
	/** 接取 NPC 成员集（任一成员可接取）。 / Acquire NPC member set. */
	private final Map<Integer, List<Integer>> acquireNpcIdsByQuestId;
	/** 任务 ID → 交付 NPC ID。 / Quest ID → reward NPC ID. */
	/** 交付 NPC 成员集（任一成员可交付）。 / Reward NPC member set. */
	private final Map<Integer, List<Integer>> rewardNpcIdsByQuestId;
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

	/** 任务 ID → 接取名类别（真端哨兵 = 系统发放）。 / Quest id → acquire-name category. */
	private final Map<Integer, RetailGrantKind> grantKindByQuestId;
	/** 任务 ID → 真端势力 id（quest.xml {@code npcfaction_name}）。 / Quest id → retail faction id. */
	private final Map<Integer, Integer> factionIdByQuestId;

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
		Map<Integer, List<Integer>> acquires = new LinkedHashMap<>();
		Map<Integer, List<Integer>> rewards = new LinkedHashMap<>();
		Map<Integer, Integer> conQuests = new LinkedHashMap<>();
		Map<Integer, Cutscene> cutscenes = new LinkedHashMap<>();
		Set<Integer> unresolvedChain = new TreeSet<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Map<Integer, RetailGrantKind> grantKinds = new LinkedHashMap<>();
		Map<Integer, Integer> factions = new LinkedHashMap<>();

		for (NativeQuestTableLoader.SimpleHuntRow row : tableLoader.rows()) {
			int qid = row.questId();
			owned.add(qid);
			if (xmlOnlyIds == null || !xmlOnlyIds.contains(qid)) {
				routed.add(qid);
			}

			// 接取 NPC 索引（哨兵行解析必然失败 = 无 NPC 接取面，接取归系统发放——
			// 真端宿主面裁定 §10.3-#25：faction/area 发放路径与家族无关）。
			// Acquire-NPC index (sentinel names intentionally fail to resolve — their acquire face is
			// the system grant; the retail host path is family-agnostic, §10.3-#25 adjudication).
			grantKinds.put(qid, RetailGrantKind.of(row.acquiredNpcName()));
			int factionId = NativeNpcFactionNames.idOf(
					NativeQuestXmlTable.instance().find(qid).map(meta -> meta.text("npcfaction_name")).orElse(""));
			if (factionId != 0) {
				factions.put(qid, factionId);
			}
			if (row.acquiredNpcName() != null && !row.acquiredNpcName().isBlank()) {
				List<Integer> members = nameResolver.resolveMembers(row.acquiredNpcName());
				if (!members.isEmpty()) {
					acquires.put(qid, members);
				}
			}

			// 交付 NPC 索引
			if (row.rewardNpcName() != null && !row.rewardNpcName().isBlank()) {
				List<Integer> members = nameResolver.resolveMembers(row.rewardNpcName());
				if (!members.isEmpty()) {
					rewards.put(qid, members);
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
			List<Integer> targetAcquires = acquires.get(next);
			List<Integer> sourceRewards = rewards.get(questId);
			if (routed.contains(next) && targetAcquires != null
					&& (sourceRewards == null || Collections.disjoint(targetAcquires, sourceRewards))) {
				unresolvedChain.add(questId);
			}
		}

		this.targetsByNpcId = Collections.unmodifiableMap(targets);
		this.acquireNpcIdsByQuestId = Collections.unmodifiableMap(acquires);
		this.rewardNpcIdsByQuestId = Collections.unmodifiableMap(rewards);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
		this.conQuestByQuestId = Collections.unmodifiableMap(conQuests);
		this.unresolvedChainQuestIds = Collections.unmodifiableSet(unresolvedChain);
		this.cutsceneByQuestId = Collections.unmodifiableMap(cutscenes);
		this.grantKindByQuestId = Collections.unmodifiableMap(grantKinds);
		this.factionIdByQuestId = Collections.unmodifiableMap(factions);
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

	/** 获取任务起始 NPC ID（成员集首项；未解析为 null）。 / Returns the acquire NPC ID for the quest. */
	public Integer acquireNpc(int questId) {
		List<Integer> members = acquireNpcIdsByQuestId.get(questId);
		return members == null || members.isEmpty() ? null : members.get(0);
	}

	/** 接取 NPC 成员集（空表 = 未解析）。 / The acquire NPC member set. */
	public List<Integer> acquireNpcs(int questId) {
		return acquireNpcIdsByQuestId.getOrDefault(questId, List.of());
	}

	/** 获取任务交付 NPC ID（成员集首项；未解析为 null）。 / Returns the reward NPC ID for the quest. */
	public Integer rewardNpc(int questId) {
		List<Integer> members = rewardNpcIdsByQuestId.get(questId);
		return members == null || members.isEmpty() ? null : members.get(0);
	}

	/** 交付 NPC 成员集（空表 = 未解析）。 / The reward NPC member set. */
	public List<Integer> rewardNpcs(int questId) {
		return rewardNpcIdsByQuestId.getOrDefault(questId, List.of());
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

	// ===== 系统发放面（NativeSystemGrantLane；§10.3-#25 宿主面裁定 = 与家族无关） =====
	// ===== System-grant face (NativeSystemGrantLane; §10.3-#25 host-face verdict = family-agnostic). =====

	@Override
	public RetailGrantKind grantKind(int questId) {
		return grantKindByQuestId.getOrDefault(questId, RetailGrantKind.NPC);
	}

	/**
	 * 是否系统发放（哨兵行且本服确有该发放入口）。挑战任务哨兵（{@code _challengetask_}）在本服只有
	 * 完成回调、没有受理入口，若按已知哨兵放行，{@code grantSystemStart} 会替它建档——与
	 * {@link SimpleTalkHandler} 同一拒绝面。
	 * Whether the row is system-granted here; challenge-task rows stay rejected (no intake).
	 */
	@Override
	public boolean isSystemGranted(int questId) {
		RetailGrantKind kind = grantKind(questId);
		return routes(questId) && kind != RetailGrantKind.NPC && kind.grantable();
	}

	/** 真端势力 id（quest.xml {@code npcfaction_name}；无则 0）。 / The retail faction id, or 0. */
	@Override
	public int factionId(int questId) {
		return factionIdByQuestId.getOrDefault(questId, 0);
	}

	/** 指定势力当前可轮换的已路由任务 id（哨兵 = FACTION 行）。 / Faction-rotation candidates. */
	@Override
	public Set<Integer> factionRotationCandidates(int factionId) {
		Set<Integer> candidates = new TreeSet<>();
		for (Map.Entry<Integer, Integer> entry : factionIdByQuestId.entrySet()) {
			int questId = entry.getKey();
			if (entry.getValue() == factionId && routes(questId)
					&& grantKind(questId) == RetailGrantKind.FACTION) {
				candidates.add(questId);
			}
		}
		return candidates;
	}

	/**
	 * 系统发放入口：无进度时直接建档到 START（与 Talk/Collect 车道同一条
	 * {@code NativeQuestStartPort.grant} 面——真端宿主发放链对全部家族共用同一原语）。
	 * The grant entry: create the row at START via the same port as the Talk/Collect lanes — the
	 * retail host grant chain shares one primitive across families (§10.3-#25 verdict).
	 */
	@Override
	public boolean grantSystemStart(Player player, int questId) {
		if (player == null || !isSystemGranted(questId)) {
			return false;
		}
		QuestState existing = player.getQuestStateList().getQuestState(questId);
		if (existing != null && existing.getStatus() != QuestStatus.NONE) {
			return false;
		}
		return NativeQuestStartPort.instance().grant(player, questId).started();
	}

	/** 阵营日常轮换资格（真端 quest.xml 轴直读，判据抽到 NativeFactionRotation 共用）。 / Rotation eligibility. */
	@Override
	public boolean factionRotationEligible(Player player, int questId, int factionId) {
		return NativeFactionRotation.eligible(player, questId, factionId, isSystemGranted(questId),
				NativeFactionRotation.factionKind(grantKind(questId)), factionId(questId));
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
		for (Map.Entry<Integer, List<Integer>> e : acquireNpcIdsByQuestId.entrySet()) {
			if (e.getValue().contains(npcId)) {
				result.add(e.getKey());
			}
		}
		for (Map.Entry<Integer, List<Integer>> e : rewardNpcIdsByQuestId.entrySet()) {
			if (e.getValue().contains(npcId) && !result.contains(e.getKey())) {
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
		for (Map.Entry<Integer, List<Integer>> entry : acquireNpcIdsByQuestId.entrySet()) {
			int qid = entry.getKey();
			if (!routedQuestIds.contains(qid)) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnQuestStart(qid);
				engine.registerQuestNpc(npcId).addOnTalkEvent(qid);
			}
		}
		for (Map.Entry<Integer, List<Integer>> entry : rewardNpcIdsByQuestId.entrySet()) {
			int qid = entry.getKey();
			if (!routedQuestIds.contains(qid)) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnTalkEvent(qid);
			}
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
			List<Integer> acqNpcs = acquireNpcIdsByQuestId.get(questId);
			if (acqNpcs != null && acqNpcs.contains(npcId)) {
				if (dialogId == 31 || dialogId == 26) {
					// 接取入口页 = 真端信页/阶段页（页 4 只能由 1007 打开，见 QuestDialogContract#retailEntryPage）。
					// The accept entry page is the retail letter/stage page (page 4 is 1007-only).
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId,
							QuestDialogContract.loadDefault().retailEntryPage(questId), questId));
					return true;
				} else if (dialogId == QuestDialogAction.ASK_QUEST_ACCEPT.id()) {
					// 真端页动作 1007（ASK_QUEST_ACCEPT → mgr+0x1a0）：打开接取窗页 4；客户端未声明即 fail-closed。
					// Retail page action 1007 (mgr+0x1a0) opens ask window page 4; undeclared pages fail closed.
					int askWindow = QuestDialogContract.loadDefault().askWindowPage(questId);
					if (askWindow < 0) {
						return false;
					}
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, askWindow, questId));
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
					// 确认接取任务（拒绝走 startTraced 打 QUEST-TRACE，不再静默）。
					// Confirm acquire; refusals are traced instead of silent.
					if (NativeQuestStartPort.instance().startTraced(player, questId, dialogId).started()) {
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
			List<Integer> rewNpcs = rewardNpcIdsByQuestId.get(questId);
			if (rewNpcs != null && rewNpcs.contains(npcId)) {
				if (dialogId == 31 || dialogId == 26) {
					// 尚未完成杀怪：常规未完成对话提示 (page 10)
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 10));
					return true;
				}
			}
			return false;
		}

		// 3. 待交付状态 (REWARD)：在交付 NPC 处领取奖励
		if (status == QuestStatus.REWARD) {
			List<Integer> rewNpcs = rewardNpcIdsByQuestId.get(questId);
			if (rewNpcs != null && rewNpcs.contains(npcId)) {
				if (dialogId == 31 || dialogId == 26 || dialogId == 1009 || dialogId == -1) {
					// 展示奖励选择窗口 (select_quest_reward1 / page 5)
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 5, questId));
					return true;
				} else if ((dialogId >= 8 && dialogId <= 22)
						|| dialogId == QuestDialogAction.SELECTED_QUEST_NOREWARD.id()
						|| dialogId == 108 || (dialogId >= 110 && dialogId <= 124)) {
					// 选项段只有 SELECTED_QUEST_REWARD1..15（8..22）；23 = NOREWARD 无选择确认，
					// 不占选项下标——发放由结算体按 dialogId==23 + extendedRewardIndex 决定。
					// Only SELECTED_QUEST_REWARD1..15 (8..22) index options; 23 is the no-selection
					// confirm whose grant the settlement resolves via dialogId==23 + extendedRewardIndex.
					int rewardIndex = (dialogId >= 8 && dialogId <= 22) ? (dialogId - 8) : 0;
					if (rewardFlow.claim(env, rewardIndex).completed()) {
						// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG（4801/4805）：回选择对话页
						// （页 10，questId=0；9/28 旧引擎基线「状态=5 → 页=10」）。
						// The claim tail follows the retail npc-complete finish=SELECTION_DIALOG: back to
						// the selection dialog (page 10, questId=0; the legacy 9/28 log baseline).
						PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(targetObjectId, QuestDialogPage.SELECT_QUEST.id()));
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
