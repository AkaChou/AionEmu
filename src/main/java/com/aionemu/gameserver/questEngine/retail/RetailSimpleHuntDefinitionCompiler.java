package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCompilationException;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 真端 SimpleHunt 表 → 完整任务定义（无 shell）的合成器（M2 核心）。
 * <p>
 * 本类是家族唯一入口（迁移期 shell 方案 {@code RetailSimpleHuntIrCompiler} 已退役）：不再消费 quest-definition XML：
 * 元数据来自 {@link RetailQuestMetadataCompiler}，接取/报告/完成对话路由按真端
 * ScriptDLL64 语义与既有生产 DSL 的规范形合成（与 {@code QuestXmlBlockExpander} 的
 * npc-start / counter-grid / npc-report / npc-complete 展开结果同构）。
 * <p>
 * 合成不可表达的定义时返回稳定拒绝码（调用方降级 XML 并登记）：
 * <ul>
 * <li>{@code RETAIL_METADATA_UNRESOLVED}：真端元数据含未解析符号名；</li>
 * <li>{@code RETAIL_MULTI_TIER}：奖励多于 1 组（重复档位任务，后续切片支持）；</li>
 * <li>其余沿用原 shell 方案的网格校验语义（计数/槽位/路由）。</li>
 * </ul>
 * Compiles a retail SimpleHunt row and retail metadata into a full definition without a shell.
 */
public final class RetailSimpleHuntDefinitionCompiler {

	/** 真端 PvP Target Level Gap 的装载器缺省值（DataDrivenQuestLoader 构造 0x14 槽位 = 10）。 /
	 * The loader default of the retail "PvP Target Level Gap" slot (0x14) is 10. */
	private static final int DEFAULT_PVP_LEVEL_GAP = 10;

	/** 网格推进边只同步数据包。 / Grid edges sync packets only. */
	private static final List<AfterCommitAction> PACKET_ONLY_SYNC =
		List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));

	private RetailSimpleHuntDefinitionCompiler() {
	}

	/**
	 * 编译结果。 / Compilation outcome.
	 */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 网格形合成（家族入口）。
	 * <p>
	 * {@code questAreas} 是"区域发放是否已接线"的唯一判据：真端 {@code _area_} 行的接取归一为世界文件的
	 * {@code <questscript_area><quest>} 绑定（生产同义载体 = {@code ai-areas.xml} 的 {@code <quest_area>}），
	 * 只有该任务 id 已绑定才允许脱离 XML。
	 * <p>
	 * Grid synthesis (the family entry point). {@code questAreas} is the single judge of whether the
	 * area grant is wired: a retail {@code _area_} row is only allowed to leave XML when its quest id is
	 * bound by a {@code <quest_area>} entry.
	 */
	public static Outcome compile(RetailSimpleHuntPlan plan, RetailQuestMetadataCompiler.Outcome metadata,
			RetailClientRewardNpcs clientRewardNpcs, RetailQuestAreaIndex questAreas,
			RetailClientDialogExits clientDialogExits) {
		return compile(plan, metadata, clientRewardNpcs, questAreas, clientDialogExits, false);
	}

	/**
	 * DD 网格行的规范形通道（quest-native-dispatch P0-2 DD 切片）：对话走家族规范形（页 4 接取窗、
	 * 一步简报、满段 QUEST_SELECT 分档窗交付），网格/顺序链的进度语义不变。页链参数（入口页覆盖、
	 * 未满段报告路由、报告页登记）已随 `quest_client_report_pages.tsv` 退役整体删除。
	 * The canonical channel for DD grid rows (the quest-native-dispatch P0-2 DD slice): dialogs
	 * follow the family canonical shape while the grid/sequential progress semantics stay. The
	 * page-chain parameters retired together with the report-page registry.
	 */
	public static Outcome compileCanonical(RetailSimpleHuntPlan plan, RetailQuestMetadataCompiler.Outcome metadata,
			RetailClientRewardNpcs clientRewardNpcs, RetailQuestAreaIndex questAreas,
			RetailClientDialogExits clientDialogExits) {
		return compile(plan, metadata, clientRewardNpcs, questAreas, clientDialogExits, false);
	}

	/**
	 * DD 多段 hunt 行：顺序 SECTION 链合成。客户端 {@code quest_monster.csv} 只在当前段列出目标怪
	 * （上一段满后才切换清单），因此节点集 = 链式前缀状态、击杀边只推进首个未满段——语义与
	 * {@link #compileSerialChain} 一致。对话走家族规范形（P0-2 DD 顺序链切片）：页 4 接取窗、
	 * 一步简报、满段 QUEST_SELECT 直接翻 REWARD 并按档位查表下发奖励窗（页链/1009 中转删除）；
	 * 链式击杀边（首个未满段推进）与交付 NPC 集保持既有语义。仅限非 PVP 多段行：PVP 多段仍按
	 * {@code RETAIL_PVP_MULTI_STAGE_DEFERRED} 保留 XML（由 DD 编译器路由把关）。
	 * DD multi-stage hunt rows: sequential SECTION-chain synthesis. The client journal lists only
	 * the current stage's targets, so nodes are chained prefix states and kill edges advance the
	 * first unfinished slot — the same semantics as {@link #compileSerialChain}. Dialogs follow the
	 * family canonical shape (the P0-2 DD sequential slice): the page-4 ask window, one-step
	 * briefing, and the full node's QUEST_SELECT flipping REWARD with the tiered window (no page
	 * chain, no 1009 hop); chained kill edges and the delivery npc set keep their semantics.
	 * Non-PVP rows only; PVP multi-stage stays deferred at the DD router.
	 */
	public static Outcome compileSequentialStages(RetailSimpleHuntPlan plan,
			RetailQuestMetadataCompiler.Outcome metadata, RetailClientRewardNpcs clientRewardNpcs,
			RetailQuestAreaIndex questAreas, RetailClientDialogExits clientDialogExits) {
		return compile(plan, metadata, clientRewardNpcs, questAreas, clientDialogExits, true);
	}

	private static Outcome compile(RetailSimpleHuntPlan plan, RetailQuestMetadataCompiler.Outcome metadata,
			RetailClientRewardNpcs clientRewardNpcs, RetailQuestAreaIndex questAreas,
			RetailClientDialogExits clientDialogExits, boolean sequentialStages) {
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(clientRewardNpcs, "clientRewardNpcs");
		Objects.requireNonNull(questAreas, "questAreas");
		Objects.requireNonNull(clientDialogExits, "clientDialogExits");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		Outcome briefing = requireBriefing(plan);
		if (briefing != null) {
			return briefing;
		}
		Outcome blocked = precheck(plan, clientRewardNpcs, true, questAreas);
		if (blocked != null) {
			return blocked;
		}
		// 多段顺序链的接取/报告边按单 NPC 构造：真端对话名组的组员展开留待后续批，
		// 这里如实拒绝，而不是静默取组里的某一个 npc。
		// The multi-stage sequential chain builds its accept/report edges around a single npc;
		// expanding a declared dialog-name group is deferred, so reject honestly instead of
		// silently picking one member of the group.
		if (sequentialStages && (plan.acquiredNpcIsQuestAiNameGroup() || plan.rewardNpcIsQuestAiNameGroup())) {
			return new Outcome(null, "RETAIL_SERIAL_CHAIN_GROUP_DEFERRED",
				plan.acquiredNpcName() + " / " + plan.rewardNpcName());
		}
		try {
			QuestDefinition definition = build(plan, metadata.metadata(), clientRewardNpcs,
				plan.briefingNpcIds(), sequentialStages);
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 简报门判定：真端 {@code talk_npc1} 非空时，必须唯一解析到 NPC，且计数槽位与简报标志位 SECTION_5
	 * 不撞位，否则用稳定码拒绝并保留 XML。
	 * <p>
	 * 拒绝码：
	 * <ul>
	 * <li>{@code RETAIL_TALK_NPC_UNRESOLVED} / {@code RETAIL_TALK_NPC_AMBIGUOUS}：简报 NPC 名解析不出/多解；</li>
	 * <li>{@code RETAIL_BRIEFING_SLOT_CONFLICT}：计数槽位 ≥ 6，与简报标志位 SECTION_5（偏移 30）撞位。</li>
	 * </ul>
	 * W5-g4：客户端简报链的两条判据（{@code RETAIL_BRIEFING_CHAIN_MISSING} /
	 * {@code RETAIL_BRIEFING_TERMINAL_UNEXPECTED}）随 {@code quest_client_briefing_chains.tsv} 退役；其
	 * 覆盖不变量（宇宙内每个非空 {@code talk_npc1} 行都有 SETPRO 终点链）改由构建期常设门禁
	 * {@code RetailBriefingChainEvidenceGateTest}（冻结登记 + 全量断言）承担。
	 * Briefing gate: a non-blank {@code talk_npc1} must resolve to exactly one NPC and must not collide with
	 * SECTION_5. The two client-chain clauses retired with the registry; the coverage invariant is asserted
	 * at build time by RetailBriefingChainEvidenceGateTest.
	 */
	private static Outcome requireBriefing(RetailSimpleHuntPlan plan) {
		String raw = plan.briefingNpcName() == null ? "" : plan.briefingNpcName().trim();
		if (raw.isEmpty()) {
			return null;
		}
		Set<Integer> ids = plan.briefingNpcIds() == null ? Set.of() : plan.briefingNpcIds();
		if (ids.isEmpty()) {
			return new Outcome(null, "RETAIL_TALK_NPC_UNRESOLVED", raw);
		}
		if (ids.size() > 1) {
			return new Outcome(null, "RETAIL_TALK_NPC_AMBIGUOUS", raw + " -> " + ids);
		}
		for (RetailSimpleHuntPlan.BoundCounter counter : plan.counters()) {
			if (RetailHuntCounterLayout.shiftFor(counter.slot()) >= RetailHuntCounterLayout.shiftFor(6)) {
				return new Outcome(null, "RETAIL_BRIEFING_SLOT_CONFLICT",
					"slot " + counter.slot() + " collides with SECTION_5");
			}
		}
		return null;
	}

	/**
	 * SimpleSerialHunt 串行阶梯合成（P3）：客户端 {@code quest_monster.csv} 的链式
	 * {@code Progress(SECTION_n<count; SECTION_(n-1)==count')} 门控决定"乱序击杀不计数"，
	 * 因此节点集 = 链式前缀组合（前序段满、当前段计数 0..count），击杀边只从"首个未满段"推进；
	 * 接取/报告/完成流与网格形完全同构。阶段契约（段数、计数、刷怪 id 集）来自
	 * {@link RetailClientHuntStages}（{@code quest_client_hunt_stages.tsv}）。
	 * <p>
	 * Serial-ladder synthesis for SimpleSerialHunt: the client's chained section gates make
	 * out-of-order kills not count, so nodes are the chained prefix combos and kill edges only
	 * advance the first unfinished stage; accept/report/complete flows reuse the grid shapes.
	 */
	public static Outcome compileSerialChain(RetailSimpleHuntPlan plan, RetailClientHuntStages stageRegistry,
			RetailQuestMetadataCompiler.Outcome metadata) {
		return compileSerialChain(plan, stageRegistry, metadata, Set.of(), "");
	}

	/**
	 * 带简报步骤的串行阶梯合成：真端 {@code talk_npc1} 是"计数开始前必须见的中间 NPC"——
	 * 接取后先置 {@code SECTION_5}=1 只亮简报行，简报 NPC 的 QUEST_SELECT 一步清标志位后才开计数。
	 * 对话走家族规范形（P0-2）：QUEST_SELECT 直发接取窗（页 4）、简报一步清标志、满段
	 * QUEST_SELECT 直翻 REWARD + 分档奖励窗（页链/select2 简报页/1009 中转删除）。
	 * Serial-ladder synthesis with the retail {@code talk_npc1} briefing step: accept raises the
	 * briefing flag (SECTION_5) and the briefing NPC's QUEST_SELECT clears it in one step before
	 * counters open. Dialogs follow the family canonical shape (P0-2): page-4 ask window, one-step
	 * briefing clear, full-node QUEST_SELECT flipping REWARD with the tiered reward window.
	 *
	 * @param briefingNpcIds 简报 NPC 解析结果（空 = 无简报步骤）
	 * @param briefingNpcName 真端原始名（非空但解析不出/歧义 → 稳定拒绝码，保留 XML）
	 */
	public static Outcome compileSerialChain(RetailSimpleHuntPlan plan, RetailClientHuntStages stageRegistry,
			RetailQuestMetadataCompiler.Outcome metadata, Set<Integer> briefingNpcIds, String briefingNpcName) {
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(stageRegistry, "stageRegistry");
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(briefingNpcIds, "briefingNpcIds");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		if (stageRegistry.stages(plan.questId()).isEmpty()) {
			return new Outcome(null, "RETAIL_STAGE_REGISTRY_MISSING", String.valueOf(plan.questId()));
		}
		boolean briefing = briefingNpcName != null && !briefingNpcName.isBlank();
		if (briefing && briefingNpcIds.isEmpty()) {
			return new Outcome(null, "RETAIL_TALK_NPC_UNRESOLVED", briefingNpcName);
		}
		if (briefing && briefingNpcIds.size() > 1) {
			return new Outcome(null, "RETAIL_TALK_NPC_AMBIGUOUS", briefingNpcName);
		}
		Outcome blocked = precheck(plan, RetailClientRewardNpcs.empty(), false, RetailQuestAreaIndex.empty());
		if (blocked != null) {
			return blocked;
		}
		// 顺序链（每段一节阶梯）按单 NPC 构造接取/报告边：真端对话名组的组员展开留待后续批。
		// The serial chain builds single-npc accept/report edges; declared dialog-name group
		// expansion is deferred to a later batch.
		if (plan.acquiredNpcIsQuestAiNameGroup() || plan.rewardNpcIsQuestAiNameGroup()) {
			return new Outcome(null, "RETAIL_SERIAL_CHAIN_GROUP_DEFERRED",
				plan.acquiredNpcName() + " / " + plan.rewardNpcName());
		}
		List<Slot> slots = serialSlots(plan.questId(), stageRegistry);
		for (Slot slot : slots) {
			if (slot.required() > RetailHuntCounterLayout.SECTION_MASK) {
				return new Outcome(null, "RETAIL_COUNTER_EXCEEDS_6BIT",
					"slot " + slot.slot() + " count " + slot.required());
			}
		}
		try {
			QuestDefinition definition = buildSerialChain(plan, slots, metadata.metadata(),
				briefing ? briefingNpcIds : Set.of());
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			// 拒绝码只保留稳定码；排查期用 -Dretail.debugCompilationFailure=true 打印根异常
			// （驱动侧 rejectionCode() 不携带 detail，定位时只能从这里看）。
			// The stable code hides the cause; enable -Dretail.debugCompilationFailure=true to print it.
			if (Boolean.getBoolean("retail.debugCompilationFailure")) {
				e.printStackTrace(System.out);
			}
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/** 串行阶段 → 槽位（id 集来自客户端阶段契约，排序去重）。 / Serial stages to slots. */
	private static List<Slot> serialSlots(int questId, RetailClientHuntStages stageRegistry) {
		List<Slot> slots = new ArrayList<>();
		Set<Integer> seen = new LinkedHashSet<>();
		for (RetailClientHuntStages.Stage stage : stageRegistry.stages(questId)) {
			List<Integer> npcIds = new ArrayList<>(stage.npcIds());
			Collections.sort(npcIds);
			if (npcIds.isEmpty()) {
				throw new IllegalArgumentException("stage " + stage.stage() + " resolves no npc");
			}
			for (int npcId : npcIds) {
				if (!seen.add(npcId)) {
					throw new IllegalArgumentException("npc " + npcId + " claimed twice");
				}
			}
			slots.add(new Slot(stage.stage(), stage.count(), List.copyOf(npcIds)));
		}
		if (slots.isEmpty()) {
			throw new IllegalArgumentException("stage registry declares no stage");
		}
		return List.copyOf(slots);
	}

	/** 链式前缀组合：每步只推进首个未满段。 / Chained prefix combos; each step advances the first unfinished slot. */
	private static List<List<Integer>> chainedCombos(List<Slot> slots) {
		List<List<Integer>> combos = new ArrayList<>();
		combos.add(new ArrayList<>(Collections.nCopies(slots.size(), 0)));
		for (int index = 0; index < combos.size(); index++) {
			List<Integer> combo = combos.get(index);
			for (int dim = 0; dim < slots.size(); dim++) {
				if (combo.get(dim) < slots.get(dim).required()) {
					List<Integer> next = new ArrayList<>(combo);
					next.set(dim, combo.get(dim) + 1);
					combos.add(next);
					break;
				}
			}
		}
		return List.copyOf(combos);
	}

	/**
	 * 串行击杀边：每节点只从首个未满段推进；节点名取串行规范标签
	 * （{@code started}/{@code briefed}/{@code k1..kN}），不再用网格名。
	 * Serial kill edges; each node advances its first unfinished slot and uses the canonical
	 * ladder labels instead of the legacy grid names.
	 */
	private static List<QuestTransition> chainedEdges(List<Slot> slots, List<List<Integer>> combos,
			Map<Integer, String> labels) {
		List<QuestTransition> edges = new ArrayList<>();
		for (List<Integer> combo : combos) {
			for (int dim = 0; dim < slots.size(); dim++) {
				if (combo.get(dim) >= slots.get(dim).required()) {
					continue;
				}
				List<Integer> next = new ArrayList<>(combo);
				next.set(dim, combo.get(dim) + 1);
				String target = labels.get(pack(slots, next));
				for (int npcId : slots.get(dim).npcIds()) {
					edges.add(new QuestTransition(new QuestEvent.KillNpc(npcId), List.of(), List.of(), target,
						PACKET_ONLY_SYNC, null, labels.get(pack(slots, combo))));
				}
				break;
			}
		}
		return List.copyOf(edges);
	}

	private static QuestDefinition buildSerialChain(RetailSimpleHuntPlan plan, List<Slot> slots,
			QuestMetadata metadata, Set<Integer> briefingNpcIds) {
		int questId = plan.questId();
		boolean briefing = !briefingNpcIds.isEmpty();
		int briefingNpc = briefing ? briefingNpcIds.iterator().next() : 0;
		ProgressLayout.Builder layoutBuilder = new ProgressLayout.Builder();
		for (Slot slot : slots) {
			layoutBuilder.add(new BitField("var" + (slot.slot() - 1), RetailHuntCounterLayout.shiftFor(slot.slot()),
				RetailHuntCounterLayout.SECTION_BITS, 0, RetailHuntCounterLayout.SECTION_MASK,
				com.aionemu.gameserver.questEngine.definition.PersistenceMode.PERSISTENT,
				com.aionemu.gameserver.questEngine.definition.ProgressScope.LOCAL));
		}
		if (briefing) {
			// 简报标志位固定占 SECTION_5（真端/客户端行 0 的门控位）。 /
			// The briefing flag owns SECTION_5, the client row-0 gate.
			// 位宽 2（30..31）而非 6：SECTION_5 语义是 0/1，且 6 位会越过 32 位 quest_vars。
			// Width 2 (30..31), not 6: SECTION_5 is a 0/1 flag and 6 bits would overflow quest_vars.
			layoutBuilder.add(new BitField("var5", RetailHuntCounterLayout.shiftFor(6),
				2, 0, 1,
				com.aionemu.gameserver.questEngine.definition.PersistenceMode.PERSISTENT,
				com.aionemu.gameserver.questEngine.definition.ProgressScope.LOCAL));
		}
		ProgressLayout layout = layoutBuilder.build();

		List<List<Integer>> combos = chainedCombos(slots);
		Map<Integer, String> labels = new java.util.LinkedHashMap<>();
		List<QuestNode> nodes = new ArrayList<>();
		Map<String, Integer> zero = new java.util.LinkedHashMap<>();
		slots.forEach(slot -> zero.put("var" + (slot.slot() - 1), 0));
		if (briefing) {
			zero.put("var5", 0);
		}
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.copyOf(zero))));
		if (briefing) {
			Map<String, Integer> raised = new java.util.LinkedHashMap<>(zero);
			raised.put("var5", 1);
			nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, Map.copyOf(raised))));
			nodes.add(new QuestNode("briefed", new NodeProjection(QuestStatus.START, Map.copyOf(zero))));
		}
		for (int index = 0; index < combos.size(); index++) {
			String label = index == 0 ? (briefing ? "briefed" : "started") : "k" + index;
			labels.put(pack(slots, combos.get(index)), label);
			if (index > 0) {
				nodes.add(new QuestNode(label, new NodeProjection(QuestStatus.START,
					serialProjection(slots, combos.get(index), briefing))));
			} else if (!briefing) {
				nodes.add(new QuestNode(label, new NodeProjection(QuestStatus.START, Map.copyOf(zero))));
			}
		}
		List<Integer> full = new ArrayList<>();
		slots.forEach(slot -> full.add(slot.required()));
		String rewardLabel = "reward";
		String completeLabel = "complete";
		String fullLabel = labels.get(pack(slots, full));
		nodes.add(new QuestNode(rewardLabel, new NodeProjection(QuestStatus.REWARD,
			serialProjection(slots, full, briefing))));
		nodes.add(new QuestNode(completeLabel, new NodeProjection(QuestStatus.COMPLETE, Map.copyOf(zero))));

		boolean enterWorld = plan.grantKind() == RetailGrantKind.WORLD || plan.worldAcquireId() > 0;
		int acquiredNpc = enterWorld ? -1 : acquiredNpcId(plan);
		int rewardNpc = rewardNpcId(plan);
		List<QuestTransition> transitions = new ArrayList<>();
		if (enterWorld) {
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StartEligible(), new QuestCondition.WorldIs(plan.worldAcquireId(), true)),
				List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		} else {
			// 规范形：QUEST_SELECT 直发接取窗（页 4），页链（select1/1007/续页阶梯）不再由服务端驱动。
			// Canonical: QUEST_SELECT emits the ask window (page 4) directly; the letter-page chain
			// is no longer server-driven.
			transitions.addAll(canonicalAcceptFlow(acquiredNpc, "started"));
		}
		if (briefing) {
			// 规范形简报：简报 NPC 的 QUEST_SELECT 一步清 SECTION_5 标志并关窗（"见中间人才开计数"
			// 语义保留，select2 页链删除）；目标投影是标志位的权威（与原链末 SETPRO 收尾同机制）。
			// Canonical briefing: the briefing NPC's QUEST_SELECT clears the SECTION_5 flag and closes
			// in one step (the must-visit-middleman semantics stay; the select2 chain goes).
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(briefingNpc, QuestDialogAction.QUEST_SELECT.id()),
				List.of(), List.of(), "briefed",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()),
				null, "started"));
		}
		transitions.addAll(chainedEdges(slots, combos, labels));
		// 规范形交付：满段节点 QUEST_SELECT 直接翻 REWARD 并按档位查表下发奖励窗
		// （SELECT2 报告页、其 SETPRO1 按钮路由与 1009 中转删除；REWARD 态的重开预览仍由完成流提供）。
		// Canonical delivery: the full node's QUEST_SELECT flips REWARD and shows the tiered reward
		// window directly (no SELECT2 report page, no SETPRO1 button route, no 1009 hop); the
		// completion flow keeps the reward-state re-open previews.
		transitions.add(new QuestTransition(
			new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.QUEST_SELECT.id()), List.of(), List.of(),
			rewardLabel,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(rewardWindowPage(metadata))), null, fullLabel));
		transitions.addAll(completeFlow(metadata, rewardNpc, rewardLabel, completeLabel));
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/**
	 * 串行阶梯节点投影：计数槽 + 简报标志位（无简报时不声明 var5）。
	 * Serial ladder node projection: counter slots plus the briefing flag when declared.
	 */
	private static Map<String, Integer> serialProjection(List<Slot> slots, List<Integer> combo, boolean briefing) {
		Map<String, Integer> values = new java.util.LinkedHashMap<>(projection(slots, combo));
		if (briefing) {
			values.put("var5", 0);
		}
		return Map.copyOf(values);
	}

	/**
	 * 迁移判据：真端数据文件本身能否确定接取/报告 NPC 与击杀槽位。
	 * <p>
	 * 拒绝码（稳定，供保留清单消费）：
	 * <ul>
	 * <li>{@code RETAIL_ACQUIRE_NPC_SENTINEL} / {@code RETAIL_REWARD_NPC_SENTINEL}：
	 * 真端表用类别哨兵（{@code _challengetask_} / {@code _faction_} / {@code _area_}）代替 NPC 名，
	 * 而真端辅助表（{@code challenge_task.xml} / {@code npcfactions*.xml}）都不含 npc id，
	 * 该 NPC 只能由类别子系统在运行时决定 → 无法由真端文件推导；</li>
	 * <li>{@code RETAIL_ACQUIRE_NPC_UNRESOLVED} / {@code RETAIL_REWARD_NPC_UNRESOLVED}：名字在真端 NPC 注册表不存在；</li>
	 * <li>{@code RETAIL_ACQUIRE_NPC_AMBIGUOUS} / {@code RETAIL_REWARD_NPC_AMBIGUOUS}：名字解析出多个 npc id；</li>
	 * <li>{@code RETAIL_COUNTER_EXCEEDS_6BIT}：计数超过真端 6 位打包字段上限（63）；</li>
	 * <li>{@code RETAIL_MONSTER_UNRESOLVED}：槽位的怪名一个都没解析到。</li>
	 * </ul>
	 * Migration pre-check: whether the retail files alone determine the NPCs and the kill counters.
	 */
	private static Outcome precheck(RetailSimpleHuntPlan plan, RetailClientRewardNpcs clientRewardNpcs,
			boolean allowSystemGrant, RetailQuestAreaIndex questAreas) {
		Outcome acquired = requireAcquire(plan, allowSystemGrant, questAreas);
		if (acquired != null) {
			return acquired;
		}
		Outcome reward = requireNpc(plan.rewardNpcIds(), plan.rewardNpcName(), "REWARD",
			plan.rewardNpcIsQuestAiNameGroup());
		if (reward != null) {
			// 系统发放行的报告名可以是 {@code <地图>_<势力名>} 复合引用（不是 NPC 名）：交付 NPC 集
			// 取客户端任务书 dic 链登记；登记缺失才算拒绝。
			// Composite reward references resolve through the client quest-letter dic chain registry.
			if (!allowSystemGrant || !plan.grantKind().systemGrant()) {
				return reward;
			}
			if (!RetailQuestMetadataCompiler.isFactionComposite(plan.rewardNpcName())) {
				return reward;
			}
			if (clientRewardNpcs.rewardNpcs(plan.questId()).isEmpty()) {
				return new Outcome(null, "RETAIL_REWARD_NPC_FACTION_COMPOSITE", plan.rewardNpcName());
			}
		}
		for (RetailSimpleHuntPlan.BoundCounter counter : plan.counters()) {
			int maxCount = RetailHuntCounterLayout.WIDE_SECTION_MASK;
			if (counter.required() > maxCount) {
				return new Outcome(null, "RETAIL_COUNTER_EXCEEDS_6BIT",
					"slot " + counter.slot() + " count " + counter.required());
			}
			// PVP 计数槽没有目标 NPC（KillInWorld 通配），放行空目标集。
			if (counter.npcIds().isEmpty() && !plan.pvpProgress()) {
				return new Outcome(null, "RETAIL_MONSTER_UNRESOLVED",
					"slot " + counter.slot() + " " + counter.monsterNames());
			}
		}
		return null;
	}

	/**
	 * 接取名判定：普通名走唯一性门；{@code _faction_}（阵营日常轮换）在本服有发放入口 → 放行；
	 * {@code _area_} 的区域发放以 {@code <quest_area>} 绑定为判据（P0c-4 已按真端世界文件补齐），
	 * 未绑定才用独立稳定码保留 XML；
	 * {@code _challengetask_}（挑战任务）只有完成回调、没有受理入口，未知哨兵不属于任何发放系统 → 同样拒绝。
	 * Acquire-name gate: plain names must be unique; the faction sentinel has a live grant entry, while
	 * area rows are let through only when the quest-area table binds them, and
	 * challenge-task/unknown sentinels stay rejected with stable codes.
	 */
	private static Outcome requireAcquire(RetailSimpleHuntPlan plan, boolean allowSystemGrant,
			RetailQuestAreaIndex questAreas) {
		RetailGrantKind kind = plan.grantKind();
		if (kind == RetailGrantKind.WORLD || plan.worldAcquireId() > 0) {
			if (plan.worldAcquireId() <= 0) {
				return new Outcome(null, "RETAIL_ACQUIRE_GRANT_UNSUPPORTED", "missing world acquire id");
			}
			return null;
		}
		if (!kind.systemGrant()) {
			return requireNpc(plan.acquiredNpcIds(), plan.acquiredNpcName(), "ACQUIRE",
				plan.acquiredNpcIsQuestAiNameGroup());
		}
		String raw = plan.acquiredNpcName() == null ? "" : plan.acquiredNpcName().trim();
		if (allowSystemGrant && kind == RetailGrantKind.FACTION) {
			return null;
		}
		// _area_：区域表已绑定该任务 → 由 RetailAreaEngine 进区域发放；未绑定才保留 XML。
		// Area sentinel: a bound quest id is granted on area entry by RetailAreaEngine; unbound stays XML.
		if (allowSystemGrant && kind == RetailGrantKind.AREA && questAreas.isBound(plan.questId())) {
			return null;
		}
		String code = switch (kind) {
			case AREA -> "RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING";
			case CHALLENGE_TASK -> "RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH";
			default -> "RETAIL_ACQUIRE_NPC_SENTINEL";
		};
		return new Outcome(null, code, raw + " -> " + kind);
	}

	/** 单个 NPC 名的判定：唯一 → 放行；空集/多解按名字形态（哨兵或真名）归类。 / Single-NPC gate. */
	private static Outcome requireNpc(Set<Integer> ids, String name, String role, boolean declaredGroup) {
		if (ids != null && ids.size() == 1) {
			return null;
		}
		// 真端对话名组（守备队同组共用对话名）：多成员是数据本意，全组展开合法。
		// A declared retail dialog-name group: several members are the data's intent, so the
		// whole-group expansion is legal rather than ambiguous.
		if (declaredGroup && ids != null && ids.size() > 1) {
			return null;
		}
		String raw = name == null ? "" : name.trim();
		if (ids != null && ids.size() > 1) {
			return new Outcome(null, "RETAIL_" + role + "_NPC_AMBIGUOUS", raw + " -> " + ids);
		}
		String kind = isSentinel(raw) ? "_NPC_SENTINEL" : "_NPC_UNRESOLVED";
		return new Outcome(null, "RETAIL_" + role + kind, raw);
	}

	/** 真端类别哨兵：{@code _challengetask_}、{@code _faction_}、{@code _area_}。 / Retail category sentinel. */
	private static boolean isSentinel(String name) {
		return name.length() > 2 && name.startsWith("_") && name.endsWith("_");
	}

	private static QuestDefinition build(RetailSimpleHuntPlan plan, QuestMetadata metadata,
			RetailClientRewardNpcs clientRewardNpcs, Set<Integer> briefingNpcIds,
			boolean sequentialStages) {
		int questId = plan.questId();
		boolean briefing = briefingNpcIds != null && !briefingNpcIds.isEmpty();
		List<Slot> slots = slots(plan);
		boolean wide = slots.stream().anyMatch(s -> s.required() > RetailHuntCounterLayout.SECTION_MASK);
		if (wide) {
			return buildCanonicalCounterQuest(plan, slots, metadata, clientRewardNpcs, briefingNpcIds);
		}
		ProgressLayout.Builder layoutBuilder = new ProgressLayout.Builder();
		for (Slot slot : slots) {
			layoutBuilder.add(new BitField("var" + (slot.slot() - 1), RetailHuntCounterLayout.shiftFor(slot.slot()),
				RetailHuntCounterLayout.SECTION_BITS, 0, RetailHuntCounterLayout.SECTION_MASK,
				com.aionemu.gameserver.questEngine.definition.PersistenceMode.PERSISTENT,
				com.aionemu.gameserver.questEngine.definition.ProgressScope.LOCAL));
		}
		if (briefing) {
			// 简报标志位占 SECTION_5（偏移 30）：客户端击杀行的门控条件就是 SECTION_5==0，
			// 位宽 1 / max 1 与仓库内既有网格 XML 的 var5 完全一致（如 14112、24155）。
			// The briefing flag owns SECTION_5 (offset 30) with the same 1-bit shape the in-repo
			// grid XMLs already use (14112, 24155); the client gates kill rows on SECTION_5==0.
			layoutBuilder.add(new BitField("var5", RetailHuntCounterLayout.shiftFor(6), 1, 0, 1,
				com.aionemu.gameserver.questEngine.definition.PersistenceMode.PERSISTENT,
				com.aionemu.gameserver.questEngine.definition.ProgressScope.LOCAL));
		}
		ProgressLayout layout = layoutBuilder.build();

		// 顺序 SECTION 链（DD 多段行）：客户端任务书只列当前段目标 → 链式前缀状态，状态数线性；
		// 单段/既有网格族维持全积组合不变（冻结指纹）。
		// Sequential SECTION chain (DD multi-stage rows): chained prefix states with a linear count;
		// single-stage rows and the existing grid family keep the full product (frozen fingerprints).
		List<List<Integer>> combos = sequentialStages ? chainedCombos(slots) : combos(slots);
		Map<Integer, String> gridLabels = new java.util.LinkedHashMap<>();
		List<QuestNode> nodes = new ArrayList<>();
		Map<String, Integer> zero = new java.util.LinkedHashMap<>();
		slots.forEach(slot -> zero.put("var" + (slot.slot() - 1), 0));
		if (briefing) {
			zero.put("var5", 0);
		}
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.copyOf(zero))));
		if (briefing) {
			// started = 已接取、简报未完成（标志位 1）；网格零段节点（标志位 0）= 简报已完成、可计数。
			// started = accepted with the briefing pending; the zero-combo grid node is the briefed state.
			Map<String, Integer> started = new java.util.LinkedHashMap<>(zero);
			started.put("var5", 1);
			nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, Map.copyOf(started))));
		}
		for (List<Integer> combo : combos) {
			String label = label(combo);
			gridLabels.put(pack(slots, combo), label);
			nodes.add(new QuestNode(label, new NodeProjection(QuestStatus.START,
				briefing ? withFlagCleared(projection(slots, combo)) : projection(slots, combo))));
		}
		List<Integer> full = new ArrayList<>();
		slots.forEach(slot -> full.add(slot.required()));
		String rewardLabel = "reward";
		String completeLabel = "complete";
		nodes.add(new QuestNode(rewardLabel, new NodeProjection(QuestStatus.REWARD,
			briefing ? withFlagCleared(projection(slots, full)) : projection(slots, full))));
		nodes.add(new QuestNode(completeLabel, new NodeProjection(QuestStatus.COMPLETE, Map.copyOf(zero))));

		// 路由：接取流（npc-start 规范形）→ 计数网格 → 报告（npc-report 规范形）→ 完成（npc-complete 规范形）。
		// 系统发放形状（P0c-3）：接取名是类别哨兵（_faction_）时真端没有 NPC 接取——客户端任务书只有
		// 委托书页（HACTION_FINISH_DIALOG），发放由阵营日常轮换在服务端完成，因此定义不生成接取路由，
		// 只留一条 SystemGrant 边（NONE → 零段网格节点），其余击杀网格/报告/完成与普通行同构。
		// System-grant shape: sentinel acquire names carry no NPC accept route in retail; the faction
		// rotation grants the quest and the definition keeps a single SystemGrant edge.
		boolean enterWorld = plan.grantKind() == RetailGrantKind.WORLD || plan.worldAcquireId() > 0;
		boolean systemGrant = plan.grantKind().systemGrant() && !enterWorld;
		// 接取 NPC 集：普通行单值；真端对话名组（守备队同组共用对话名）按组员逐条发接取路由，
		// 与遗留 XML 的多 NPC_START 同构（组员顺序取 id 序，保证 IR 确定性）。
		// Accept npc set: single-valued for plain rows; a declared dialog-name group emits one accept
		// route per member, isomorphic to the legacy multi-NPC_START shape (members ordered by id so
		// the IR stays deterministic).
		List<Integer> acquiredNpcs = (systemGrant || enterWorld) ? List.of()
			: plan.acquiredNpcIds().stream().sorted().toList();
		List<Integer> rewardNpcs = rewardNpcs(plan, clientRewardNpcs);
		List<QuestTransition> transitions = new ArrayList<>();
		String firstGridLabel = gridLabels.get(pack(slots, combos.get(0)));
		// 有简报时接取落在 started（标志位 1）；SETPRO 按钮清位后才进入计数网格零段节点。
		// With a briefing the accept lands on started (flag raised); the SETPRO button clears it.
		String acceptTarget = briefing ? "started" : firstGridLabel;
		if (enterWorld) {
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StartEligible(), new QuestCondition.WorldIs(plan.worldAcquireId(), true)),
				List.of(), acceptTarget,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		} else if (systemGrant) {
			transitions.add(new QuestTransition(new QuestEvent.SystemGrant(),
				List.of(new QuestCondition.StartEligible()), List.of(), acceptTarget,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		} else {
			for (int acquiredNpc : acquiredNpcs) {
				// 规范形：QUEST_SELECT 直发接取窗（页 4），页链（select1/1007/续页阶梯）不再由服务端驱动。
				// 旧接取形 `acceptFlow`/`acceptContinuation` 的唯一剩余调用方是 HandinDialogFlow（三票否决、
				// 明确排除迁移），故仍留在本类。
				// Canonical: QUEST_SELECT emits the ask window (page 4) directly; the letter-page chain is
				// no longer server-driven. The legacy accept shapes stay only for the excluded hand-in flow.
				transitions.addAll(canonicalAcceptFlow(acquiredNpc, acceptTarget));
			}
		}
		if (briefing) {
			// 规范形简报：简报 NPC 的 QUEST_SELECT 一步清 SECTION_5 标志并关窗（"见中间人才开计数"
			// 语义保留，select2 页链删除）；目标投影是标志位的权威（与原链末 SETPRO 收尾同机制）。
			// Canonical briefing: the briefing NPC's QUEST_SELECT clears the SECTION_5 flag and closes
			// in one step (the must-visit-middleman semantics stay; the select2 chain goes).
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(briefingNpcIds.iterator().next(), QuestDialogAction.QUEST_SELECT.id()),
				List.of(), List.of(), firstGridLabel,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()),
				null, "started"));
		}
		// 顺序链复用链式击杀边（首个未满段推进、KillNpc 逐怪登记）；PVP 多段在 DD 路由处已被拒。
		// Sequential chains reuse the chained kill edges (first-unfinished-slot advance, per-npc
		// KillNpc); PVP multi-stage is rejected at the DD router.
		transitions.addAll(sequentialStages
			? chainedEdges(slots, combos, gridLabels)
			: gridEdges(plan, slots, combos));
		int fullPack = pack(slots, full);
		String fullLabel = gridLabels.get(fullPack);
		// 规范形交付：满段节点 QUEST_SELECT 直接翻 REWARD 并按档位查表下发奖励窗
		// （报告页 1352/2375 与 1009 中转删除；REWARD 态的重开预览仍由完成流提供）。
		// Canonical delivery: the full node's QUEST_SELECT flips REWARD and shows the tiered
		// reward window directly (no report page, no 1009 hop); the completion flow keeps the
		// reward-state re-open previews.
		int rewardWindowPage = rewardWindowPage(metadata);
		for (int rewardNpc : rewardNpcs) {
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.QUEST_SELECT.id()),
				List.of(), List.of(), rewardLabel,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(rewardWindowPage)),
				null, fullLabel));
		}
		transitions.addAll(completeFlow(metadata, rewardNpcs, rewardLabel, completeLabel));
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/** 简报标志位为 0 的投影（网格节点/领奖节点在简报完成后都在此态）。 / Projection with the flag cleared. */
	private static Map<String, Integer> withFlagCleared(Map<String, Integer> values) {
		Map<String, Integer> cleared = new java.util.LinkedHashMap<>(values);
		cleared.put("var5", 0);
		return Map.copyOf(cleared);
	}


	// ---------------------------------------------------------------- 网格

	private record Slot(int slot, int required, List<Integer> npcIds) {
	}

	private static QuestDefinition buildCanonicalCounterQuest(RetailSimpleHuntPlan plan, List<Slot> slots,
			QuestMetadata metadata, RetailClientRewardNpcs clientRewardNpcs, Set<Integer> briefingNpcIds) {
		int questId = plan.questId();
		boolean briefing = briefingNpcIds != null && !briefingNpcIds.isEmpty();

		ProgressLayout.Builder layoutBuilder = new ProgressLayout.Builder();
		for (int i = 0; i < slots.size(); i++) {
			layoutBuilder.add(new BitField("var" + i, i * RetailHuntCounterLayout.WIDE_SECTION_BITS,
				RetailHuntCounterLayout.WIDE_SECTION_BITS, 0,
				RetailHuntCounterLayout.WIDE_SECTION_MASK,
				com.aionemu.gameserver.questEngine.definition.PersistenceMode.PERSISTENT,
				com.aionemu.gameserver.questEngine.definition.ProgressScope.LOCAL));
		}
		if (briefing) {
			layoutBuilder.add(new BitField("var5", RetailHuntCounterLayout.shiftFor(6), 1, 0, 1,
				com.aionemu.gameserver.questEngine.definition.PersistenceMode.PERSISTENT,
				com.aionemu.gameserver.questEngine.definition.ProgressScope.LOCAL));
		}
		ProgressLayout layout = layoutBuilder.build();

		Map<String, Integer> zero = new java.util.LinkedHashMap<>();
		Map<String, Integer> requiredMap = new java.util.LinkedHashMap<>();
		for (int i = 0; i < slots.size(); i++) {
			zero.put("var" + i, 0);
			requiredMap.put("var" + i, slots.get(i).required());
		}
		if (briefing) {
			zero.put("var5", 0);
			requiredMap.put("var5", 0);
		}

		List<QuestNode> nodes = new ArrayList<>();
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE,
			briefing ? Map.of("var5", 0) : Map.of())));
		if (briefing) {
			Map<String, Integer> started = new java.util.LinkedHashMap<>(zero);
			started.put("var5", 1);
			nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, Map.copyOf(started))));
			nodes.add(new QuestNode("briefed", new NodeProjection(QuestStatus.START, Map.copyOf(zero))));
		} else {
			nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of())));
		}
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, Map.copyOf(requiredMap))));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, Map.copyOf(requiredMap))));

		boolean enterWorld = plan.grantKind() == RetailGrantKind.WORLD || plan.worldAcquireId() > 0;
		List<Integer> acquiredNpcs = enterWorld ? List.of()
			: plan.acquiredNpcIds().stream().sorted().toList();
		List<Integer> rewardNpcs = rewardNpcs(plan, clientRewardNpcs);

		List<QuestTransition> transitions = new ArrayList<>();
		String acceptTarget = "started";
		if (enterWorld) {
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StartEligible(), new QuestCondition.WorldIs(plan.worldAcquireId(), true)),
				List.of(), acceptTarget,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		} else {
			List<QuestAction> resetActions = new ArrayList<>();
			for (int i = 0; i < slots.size(); i++) {
				resetActions.add(new QuestAction.SetVariable("var" + i, 0));
			}
			if (briefing) {
				resetActions.add(new QuestAction.SetVariable("var5", 0));
			}
			for (int acquiredNpc : acquiredNpcs) {
				transitions.addAll(canonicalAcceptFlow(acquiredNpc, acceptTarget, resetActions));
			}
		}

		String huntSource = "started";
		if (briefing) {
			int briefingNpc = briefingNpcIds.iterator().next();
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(briefingNpc, QuestDialogAction.QUEST_SELECT.id()),
				List.of(), List.of(), "briefed",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()),
				null, "started"));
			huntSource = "briefed";
		}

		for (int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(i);
			String varName = "var" + i;
			int required = slot.required();
			for (int npcId : slot.npcIds()) {
				transitions.add(new QuestTransition(new QuestEvent.KillNpc(npcId),
					List.of(new QuestCondition.VariableBelow(varName, required - 1)),
					List.of(new QuestAction.IncrementVariable(varName, 1)),
					huntSource, PACKET_ONLY_SYNC, 1, huntSource));

				if (slots.size() == 1) {
					transitions.add(new QuestTransition(new QuestEvent.KillNpc(npcId),
						List.of(new QuestCondition.QuestVariableIs(varName, required - 1)),
						List.of(new QuestAction.IncrementVariable(varName, 1)),
						"reward", List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
						0, huntSource));
				} else {
					List<QuestCondition> fullConditions = new ArrayList<>();
					fullConditions.add(new QuestCondition.QuestVariableIs(varName, required - 1));
					for (int j = 0; j < slots.size(); j++) {
						if (j != i) {
							fullConditions.add(new QuestCondition.VariableAtLeast("var" + j, slots.get(j).required()));
						}
					}
					transitions.add(new QuestTransition(new QuestEvent.KillNpc(npcId),
						List.copyOf(fullConditions),
						List.of(new QuestAction.IncrementVariable(varName, 1)),
						"reward", List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
						0, huntSource));
					transitions.add(new QuestTransition(new QuestEvent.KillNpc(npcId),
						List.of(new QuestCondition.QuestVariableIs(varName, required - 1)),
						List.of(new QuestAction.IncrementVariable(varName, 1)),
					huntSource, PACKET_ONLY_SYNC, 1, huntSource));
				}
			}
		}

		int rewardWindowPage = rewardWindowPage(metadata);
		for (int rewardNpc : rewardNpcs) {
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.QUEST_SELECT.id()),
				List.of(), List.of(), "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(rewardWindowPage)),
				null, "reward"));
		}
		transitions.addAll(completeFlow(metadata, rewardNpcs, "reward", "complete"));

		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	private static List<Slot> slots(RetailSimpleHuntPlan plan) {
		List<Slot> slots = new ArrayList<>();
		Set<Integer> seen = new LinkedHashSet<>();
		for (RetailSimpleHuntPlan.BoundCounter counter : plan.counters()) {
			if (counter.required() < 1 || counter.required() > RetailHuntCounterLayout.WIDE_SECTION_MASK) {
				throw new IllegalArgumentException("slot " + counter.slot() + " count out of 6-bit range");
			}
			List<Integer> npcIds = new ArrayList<>(counter.npcIds());
			Collections.sort(npcIds);
			// PVP 计数槽无目标 NPC（KillInWorld 通配），空目标集合法。
			if (npcIds.isEmpty() && !plan.pvpProgress()) {
				throw new IllegalArgumentException("slot " + counter.slot() + " resolves no npc");
			}
			for (int npcId : npcIds) {
				if (!seen.add(npcId)) {
					throw new IllegalArgumentException("npc " + npcId + " claimed twice");
				}
			}
			slots.add(new Slot(counter.slot(), counter.required(), List.copyOf(npcIds)));
		}
		if (slots.isEmpty()) {
			throw new IllegalArgumentException("retail plan declares no counter slot");
		}
		return List.copyOf(slots);
	}

	private static List<List<Integer>> combos(List<Slot> slots) {
		List<List<Integer>> combos = new ArrayList<>();
		combos.add(List.of());
		for (Slot slot : slots) {
			List<List<Integer>> next = new ArrayList<>();
			for (List<Integer> prefix : combos) {
				for (int value = 0; value <= slot.required(); value++) {
					List<Integer> combo = new ArrayList<>(prefix);
					combo.add(value);
					next.add(List.copyOf(combo));
				}
			}
			combos = next;
		}
		return List.copyOf(combos);
	}

	private static int pack(List<Slot> slots, List<Integer> combo) {
		int packed = 0;
		for (int index = 0; index < slots.size(); index++) {
			packed |= combo.get(index) << RetailHuntCounterLayout.shiftFor(slots.get(index).slot());
		}
		return packed;
	}

	private static Map<String, Integer> projection(List<Slot> slots, List<Integer> combo) {
		Map<String, Integer> values = new java.util.LinkedHashMap<>();
		for (int index = 0; index < slots.size(); index++) {
			values.put("var" + (slots.get(index).slot() - 1), combo.get(index));
		}
		return Map.copyOf(values);
	}

	private static String label(List<Integer> combo) {
		StringBuilder label = new StringBuilder();
		for (int index = 0; index < combo.size(); index++) {
			label.append((char) ('a' + index)).append(combo.get(index));
		}
		return label.toString();
	}

	/** 网格边：未满槽位 × 怪 → 计数后继。 / Grid edges: per unfinished slot and npc. */
	private static List<QuestTransition> gridEdges(RetailSimpleHuntPlan plan, List<Slot> slots,
			List<List<Integer>> combos) {
		List<QuestTransition> edges = new ArrayList<>();
		for (List<Integer> combo : combos) {
			for (int index = 0; index < slots.size(); index++) {
				Slot slot = slots.get(index);
				if (combo.get(index) >= slot.required()) {
					continue;
				}
				List<Integer> next = new ArrayList<>(combo);
				next.set(index, combo.get(index) + 1);
			String target = label(List.copyOf(next));
			if (plan.pvpProgress()) {
				// PVP 计数（P5-4）：军衔阈值行发 KillRanked(minRank)（victim 军衔 ≥ 阈值计数，
				// 阈值匹配见 QuestEvent.matches）；其余按 KillInWorld(0) 世界通配——运行时
				// PvP 击杀服务携带具体世界 id 与事实。等级窗 = ScriptDLL "PvP Target Level Gap"
				//（victim 等级 ≥ killer - gap 才计数，单边上界，下界不设限）。
				// PVP counter (P5-4): rank-threshold rows emit KillRanked(minRank); the rest use the
				// KillInWorld(0) wildcard. The level window mirrors the ScriptDLL "PvP Target Level
				// Gap": one-sided, victim level >= killer level - gap.
				QuestEvent kill = plan.pvpMinRank() > 0
					? new QuestEvent.KillRanked(plan.pvpMinRank())
					: new QuestEvent.KillInWorld(0);
				int gap = plan.pvpLevelGap() > 0 ? plan.pvpLevelGap() : DEFAULT_PVP_LEVEL_GAP;
				edges.add(new QuestTransition(kill,
					List.of(new QuestCondition.PvpVictimLevelDelta(Integer.MIN_VALUE, gap)),
					List.of(), target, PACKET_ONLY_SYNC, null, label(combo)));
				continue;
			}
				for (int npcId : slot.npcIds()) {
					edges.add(new QuestTransition(new QuestEvent.KillNpc(npcId), List.of(), List.of(), target,
						PACKET_ONLY_SYNC, null, label(combo)));
				}
			}
		}
		return List.copyOf(edges);
	}

	// ---------------------------------------------------------------- 对话流

	private static int acquiredNpcId(RetailSimpleHuntPlan plan) {
		return singleNpc(plan.acquiredNpcIds(), "acquired_npc_name");
	}

	private static int rewardNpcId(RetailSimpleHuntPlan plan) {
		return singleNpc(plan.rewardNpcIds(), "reward_npc_name");
	}

	/**
	 * 交付 NPC 集：唯一名解析优先；复合势力引用（系统发放行）走客户端任务书 dic 链登记。
	 * Hand-in NPC set: unique name resolution first, composite faction references via the client registry.
	 */
	private static List<Integer> rewardNpcs(RetailSimpleHuntPlan plan, RetailClientRewardNpcs clientRewardNpcs) {
		Set<Integer> resolved = plan.rewardNpcIds();
		// 真端对话名组：全组员都是报告点（遗留 XML 的每员一条报告边同构）；普通行仍是单值。
		// A declared dialog-name group reports through every member (the legacy XML emits one report
		// edge per member); plain rows stay single-valued.
		if (resolved != null && !resolved.isEmpty()) {
			return resolved.stream().sorted().toList();
		}
		return clientRewardNpcs.rewardNpcs(plan.questId());
	}

	private static int singleNpc(Set<Integer> ids, String field) {
		if (ids == null || ids.size() != 1) {
			throw new IllegalArgumentException(field + " must resolve exactly one npc, got " + ids);
		}
		return ids.iterator().next();
	}

	/**
	 * 接取流：与 npc-start 展开同构（QUEST_SELECT/ASK_QUEST_ACCEPT/ACCEPT_1/ACCEPT_SIMPLE/
	 * REFUSE_1/REFUSE_2/REFUSE_SIMPLE/FINISH_DIALOG），全部挂在接取 NPC 上。
	 * <p>
	 * 报告 NPC 在未接态**不**提供选择页出口（P0c-6 按 XML + 客户端证据修正）：客户端 5.8 的
	 * {@code select1}/{@code ask_quest_accept} 页属于任务发布者；真端 XML 里报告 NPC 在 NONE 态
	 * 没有任何 {@code QUEST_SELECT} 路由（例：1346 的 NPC_REPORT=203965 无 NONE 路由），
	 * 而 286 个冻结等价行全部 {@code acquired == reward}（该分支从未生效）。
	 * <p>
	 * The canonical accept flow, isomorphic to the npc-start expansion, all on the acquiring NPC.
	 * The report NPC carries no NONE-state selection page exit: the client's select1 belongs to the
	 * quest giver, no retail XML declares such a route, and every frozen row has reward == acquired.
	 */
	/**
	 * 接取流（入口页可覆盖 + select_none 阶梯）：客户端未接态首屏是 {@code select_none} 且其唯一按钮
	 * 是续页 {@code SELECT_NONE_1}(4763)（接取/拒绝按钮落在续页上）时，服务端必须给出续页路由，
	 * 否则契约门禁报 BUTTON_WITHOUT_ROUTE（遗留 15478 形：
	 * {@code unaccepted→unaccepted SELECT_NONE_1 → SHOW SELECT_NONE_1}）。
	 * The accept flow with an overridable entry page and the select_none continuation rung; the
	 * legacy 15478 shape routes SELECT_NONE_1 → SHOW SELECT_NONE_1 from the accept state.
	 */
	static List<QuestTransition> acceptFlow(int acquiredNpc, String acceptTarget, int entryPage,
			boolean selectNoneLadder) {
		List<QuestTransition> flow = new ArrayList<>();
		String source = "unaccepted";
		String target = acceptTarget;
		int shownEntry = entryPage > 0 ? entryPage : QuestDialogPage.SELECT1.id();
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_SELECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(shownEntry))));
		if (selectNoneLadder) {
			// 阶梯续页：动作 id 与目标页都是页 id 4763（客户端以页 id 回传翻页动作）。
			// The ladder rung: both the action id and the target page are page id 4763 (the client
			// sends the page id back for page turns).
			flow.add(talk(acquiredNpc, QuestDialogAction.fromId(QuestDialogPage.SELECT_NONE_1.id()),
				source, source, null,
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE_1.id()))));
		}
		flow.add(talk(acquiredNpc, QuestDialogAction.ASK_QUEST_ACCEPT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
		List<QuestCondition> eligible = List.of(new QuestCondition.StartEligible());
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.QUEST_ACCEPT_1.id()),
			eligible, List.of(), target,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), null, source));
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()),
			eligible, List.of(), target,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), null, source));
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_REFUSE_1, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id()))));
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_REFUSE_2,
			QuestDialogAction.QUEST_REFUSE_SIMPLE)) {
			flow.add(talk(acquiredNpc, action, source, source, null, List.of(new AfterCommitAction.CloseDialog())));
		}
		for (String finishSource : new TreeSet<>(List.of(source, target))) {
			flow.add(talk(acquiredNpc, QuestDialogAction.FINISH_DIALOG, finishSource, finishSource, null,
				List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()))));
		}
		return List.copyOf(flow);
	}


	/**
	 * 规范形接取流（真端原生生命周期相位 A）：QUEST_SELECT 直发接取询问窗（页 4），客户端原生
	 * 接受/拒绝按钮（1002/1003）回传建档——简报信页（select1/1011）、ask 中转（1007）、
	 * select_none 阶梯与 select1 续页一律不再由服务端驱动。拒绝族（1003→1004 页、1004/20001 关窗）
	 * 与 FINISH_DIALOG→任务选择页（10）保持既有合同。
	 * The canonical accept flow (native lifecycle phase A): QUEST_SELECT emits the ask-accept window
	 * (page 4) directly and the native accept/refuse controls (1002/1003) commit; the letter page,
	 * the ask hop, the select_none ladder, and the select1 continuations are no longer server-driven.
	 * The refuse family and the FINISH_DIALOG selection-page exit keep the existing contract.
	 */
	static List<QuestTransition> canonicalAcceptFlow(int acquiredNpc, String acceptTarget) {
		return canonicalAcceptFlow(acquiredNpc, acceptTarget, List.of());
	}

	/**
	 * 带接取动作的规范形接取流（ItemPlay 族：接取提交即发演出道具）——形状与两参重载完全同构，
	 * 仅 1002/20000 两形提交边挂调用方的接取动作。
	 * The canonical accept flow with accept actions (the ItemPlay family grants the play item on
	 * the accept commit) — isomorphic to the two-arg overload except the 1002/20000 commit edges
	 * carry the caller's actions.
	 */
	static List<QuestTransition> canonicalAcceptFlow(int acquiredNpc, String acceptTarget,
			List<QuestAction> acceptActions) {
		List<QuestTransition> flow = new ArrayList<>();
		String source = "unaccepted";
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_SELECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
		List<QuestCondition> eligible = List.of(new QuestCondition.StartEligible());
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.QUEST_ACCEPT_1.id()),
			eligible, acceptActions, acceptTarget,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), null, source));
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()),
			eligible, acceptActions, acceptTarget,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), null, source));
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_REFUSE_1, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id()))));
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_REFUSE_2,
			QuestDialogAction.QUEST_REFUSE_SIMPLE)) {
			flow.add(talk(acquiredNpc, action, source, source, null, List.of(new AfterCommitAction.CloseDialog())));
		}
		for (String finishSource : new TreeSet<>(List.of(source, acceptTarget))) {
			flow.add(talk(acquiredNpc, QuestDialogAction.FINISH_DIALOG, finishSource, finishSource, null,
				List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()))));
		}
		return List.copyOf(flow);
	}

	/**
	 * 物品接取的规范形（真端原生相位 A 的无主形）：使用任务起始道具下发接取窗（页 4），
	 * 接受/拒绝/关窗为无主对话（1002/1003/1008）——没有接取 NPC 的物品接取行（UseItem 接取）共用本形；
	 * 与 NPC 形的 {@link #canonicalAcceptFlow} 同构，仅把 NPC 对话边换成无主对话边（与 SimpleUseItem
	 * 家族的接取段同形）。 / The canonical item-acquire shape (native phase A without an npc): using the
	 * quest-start item pops the ask window (page 4); accept/refuse/finish are targetless dialogs. Item-
	 * acquired rows share this shape; it is isomorphic to the npc canonical accept flow.
	 */
	static List<QuestTransition> canonicalItemAcceptFlow(int itemId, String acceptTarget) {
		List<QuestCondition> eligible = List.of(new QuestCondition.StartEligible());
		return List.of(
			new QuestTransition(new QuestEvent.UseItem(itemId, 0), List.of(), List.of(), "unaccepted",
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), null, "unaccepted"),
			new QuestTransition(new QuestEvent.QuestDialog(QuestDialogAction.QUEST_ACCEPT_1.id()),
				eligible, List.of(), acceptTarget,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()), null, "unaccepted"),
			new QuestTransition(new QuestEvent.QuestDialog(QuestDialogAction.QUEST_REFUSE_1.id()),
				List.of(), List.of(), "unaccepted", List.of(new AfterCommitAction.CloseDialog()), null,
				"unaccepted"),
			new QuestTransition(new QuestEvent.QuestDialog(QuestDialogAction.FINISH_DIALOG.id()),
				List.of(), List.of(), "unaccepted", List.of(new AfterCommitAction.CloseDialog()), null,
				"unaccepted"));
	}

	/**
	 * 分档奖励窗页：按奖励档位查表（QE-028），档位超出客户端六档窗时显式失败，禁线性推算。
	 * The tiered reward-window page by table lookup (QE-028); tiers beyond the six client windows fail
	 * explicitly instead of extrapolating a page id.
	 */
	private static int rewardWindowPage(QuestMetadata metadata) {
		return QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1)
			.orElseThrow(() -> new IllegalArgumentException(
				"reward tiers exceed the six client reward windows: " + metadata.rewardGroups().size()))
			.id();
	}


	/**
	 * 完成流：与 npc-complete 展开同构——固定奖励 + 每个可选项一条 SELECTED_QUEST_REWARD 路由
	 * + NOREWARD 收尾，finish=SELECTION_DIALOG；预览路由 USE_OBJECT/SELECT_QUEST_REWARD。
	 * The canonical completion flow, isomorphic to npc-complete.
	 */
	static List<QuestTransition> completeFlow(QuestMetadata metadata, int rewardNpc, String rewardLabel,
			String completeLabel) {
		return completeFlow(metadata, List.of(rewardNpc), rewardLabel, completeLabel);
	}

	/** 多交付 NPC 共用一套全局奖励窗路由。 / One global reward-window route set per quest. */
	static List<QuestTransition> completeFlow(QuestMetadata metadata, Collection<Integer> rewardNpcs,
			String rewardLabel, String completeLabel) {
		List<QuestTransition> flow = new ArrayList<>();
		for (int rewardNpc : rewardNpcs) {
			flow.addAll(npcCompleteFlow(metadata, rewardNpc, rewardLabel, completeLabel));
		}
		List<QuestAction> fixedRewards = new ArrayList<>();
		List<QuestReward> selectables = new ArrayList<>();
		for (QuestReward reward : metadata.rewardGroups().get(0).rewards()) {
			if (QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM) {
				selectables.add(reward);
			} else {
				fixedRewards.add(grant(reward));
			}
		}
		flow.addAll(rewardWindowAutoFlow(rewardNpcs, fixedRewards, selectables, metadata.classRewards(),
			rewardLabel, completeLabel));
		return List.copyOf(flow);
	}

	private static List<QuestTransition> npcCompleteFlow(QuestMetadata metadata, int rewardNpc,
			String rewardLabel, String completeLabel) {
		if (metadata.rewardGroups().size() != 1) {
			throw new IllegalArgumentException("multi-tier rewards not supported: "
				+ metadata.rewardGroups().size() + " groups");
		}
		List<QuestReward> group = metadata.rewardGroups().get(0).rewards();
		List<QuestAction> fixedRewards = new ArrayList<>();
		List<QuestReward> selectables = new ArrayList<>();
		for (QuestReward reward : group) {
			if (QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM) {
				selectables.add(reward);
			} else {
				fixedRewards.add(grant(reward));
			}
		}
		List<QuestTransition> flow = new ArrayList<>();
		for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
			QuestDialogAction.SELECT_QUEST_REWARD)) {
			flow.add(talk(rewardNpc, preview, rewardLabel, rewardLabel, null,
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))));
		}
		// 与 npc-complete 的 actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" 同构：
		// 确认区间为 dialogId 8..23 全段；第 i 个可选项挂在 SELECTED_QUEST_REWARD_i 上，
		// 其余确认 id 与 NOREWARD 只发固定奖励；每条路由都以 CompleteQuest 收尾。
		// The confirm range spans dialog ids 8..23 like the production npc-complete expansion.
		List<QuestDialogAction> confirmActions = new ArrayList<>();
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			confirmActions.add(QuestDialogAction.fromId(id));
		}
		// 职业可选奖励（真端 <class>_selectable_reward 区块）：无普通可选项时，确认段每条路由按
		// AdvancedClassIs 职业条件展开，动作 = 固定奖励 + 该职业物品 + CompleteQuest，
		// 与 npc-complete 的 CLASS 形同构（classRewardKey 与 QuestXmlBlockExpander 保持同一映射）。
		// Class-selectable rewards (retail <class>_selectable_reward blocks): without ordinary
		// selectables, every confirm route expands per AdvancedClassIs granting the class items,
		// isomorphic to the npc-complete CLASS shape (classRewardKey mirrors QuestXmlBlockExpander).
		boolean classSelectRewards = selectables.isEmpty() && !metadata.classRewards().isEmpty();
		for (QuestDialogAction action : confirmActions) {
			if (classSelectRewards) {
				for (int classIndex = 0; classIndex < CLASS_ROUTE_ORDER.size(); classIndex++) {
					PlayerClass playerClass = CLASS_ROUTE_ORDER.get(classIndex);
					List<QuestReward> classItems = metadata.classRewards().get(classRewardKey(playerClass));
					if (classItems == null) {
						continue;
					}
					// 职业路由与无条件兜底路由条件重叠，必须带显式互异优先级（AMBIGUOUS_TRANSITION
					// 校验）；priority = itemIndex * 11 + classIndex，与遗留 priority=职业序号 形对齐，
					// 同职业多物品乘档错开。
					// Class routes overlap the unconditional fallback, so they need explicit distinct
					// priorities (AMBIGUOUS_TRANSITION check); priority = itemIndex * 11 + classIndex
					// aligns with the legacy priority-per-class shape and strides multi-item classes.
					for (int itemIndex = 0; itemIndex < classItems.size(); itemIndex++) {
						List<QuestAction> actions = new ArrayList<>(fixedRewards);
						actions.add(grant(classItems.get(itemIndex)));
						actions.add(new QuestAction.CompleteQuest(0));
						flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, action.id()),
							List.of(new QuestCondition.AdvancedClassIs(playerClass)), List.copyOf(actions),
							completeLabel,
							List.of(new AfterCommitAction.RefreshPlayerStats(),
								new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
								new AfterCommitAction.ShowQuestSelectionDialog(10)),
							itemIndex * CLASS_ROUTE_ORDER.size() + classIndex, rewardLabel));
					}
				}
				// 无条件兜底路由（遗留 priority=11 块）：无高级职业条件可匹配时仍以固定奖励交付，
				// 优先级排在全部职业路由之后（路由索引按优先级升序取首个匹配）。
				// Unconditional fallback (the legacy priority=11 block): delivery still completes with
				// fixed rewards when no advanced-class condition matches; its priority sits after every
				// class route (the route index takes the first match in ascending priority order).
				List<QuestAction> fallbackActions = new ArrayList<>(fixedRewards);
				fallbackActions.add(new QuestAction.CompleteQuest(0));
				flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, action.id()), List.of(),
					List.copyOf(fallbackActions), completeLabel,
					List.of(new AfterCommitAction.RefreshPlayerStats(),
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
						new AfterCommitAction.ShowQuestSelectionDialog(10)),
					REWARD_FALLBACK_PRIORITY, rewardLabel));
				continue;
			}
			int selectableIndex = action.id() - QuestDialogAction.SELECTED_QUEST_REWARD1.id();
			List<QuestAction> actions = new ArrayList<>(fixedRewards);
			if (action != QuestDialogAction.SELECTED_QUEST_NOREWARD && selectableIndex < selectables.size()) {
				actions.add(grant(selectables.get(selectableIndex)));
			}
			actions.add(new QuestAction.CompleteQuest(0));
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, action.id()), List.of(),
				List.copyOf(actions), completeLabel,
				List.of(new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(10)),
				null, rewardLabel));
		}
		return List.copyOf(flow);
	}

	/**
	 * 奖励窗口自动确认通道：SELECTED_QUEST_AUTO_REWARD(108) 与 SELECTED_QUEST_AUTO_REWARD1+i(110+i)
	 * 由全局窗口 UI 发出，可能不带 NPC 上下文——按双协议注册（QuestDialog 无主键 + TalkToNpc 报告
	 * NPC 键），afterCommit 以 CloseDialog 收窗（遗留 targetless 契约形）；对话页确认通道保持回
	 * 任务选择页。固定奖励挂 108；可选奖励第 k 项挂 110+k；职业可选奖励整梯挂 110（与遗留
	 * QUEST_ACTION 块同形：窗口侧无兜底路由）。
	 * The reward-window auto-confirm channel: SELECTED_QUEST_AUTO_REWARD (108) and
	 * SELECTED_QUEST_AUTO_REWARD1+i (110+i) come from the global window UI and may lack NPC context,
	 * so routes register under both protocols (npc-agnostic QuestDialog plus report-NPC TalkToNpc)
	 * and close the window after commit (the legacy targetless contract shape); the talk-page confirm
	 * channel keeps returning to the quest-selection page. Fixed rewards bind 108, selectable slot k
	 * binds 110+k, and the class ladder binds 110 wholesale (the legacy QUEST_ACTION block shape:
	 * no fallback on the window side).
	 */
	static List<QuestTransition> rewardWindowAutoFlow(int rewardNpc, List<QuestAction> fixedRewards,
			List<QuestReward> selectables, Map<String, List<QuestReward>> classRewards, String rewardLabel,
			String completeLabel) {
		return rewardWindowAutoFlow(java.util.List.of(rewardNpc), fixedRewards, selectables, classRewards,
			rewardLabel, completeLabel);
	}

	/** 变体家族形：对话页通道整族一次、报告 NPC 通道逐实例（50052 形同名多刷点）。 /
	 * Family shape: the dialog-page channel once per family, the report-npc channel per instance. */
	static List<QuestTransition> rewardWindowAutoFlow(java.util.Collection<Integer> rewardNpcs,
			List<QuestAction> fixedRewards, List<QuestReward> selectables,
			Map<String, List<QuestReward>> classRewards, String rewardLabel, String completeLabel) {
		List<AfterCommitAction> closeWindow = List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.CloseDialog());
		List<QuestTransition> flow = new ArrayList<>();
		if (selectables.isEmpty() && !classRewards.isEmpty()) {
			for (int classIndex = 0; classIndex < CLASS_ROUTE_ORDER.size(); classIndex++) {
				PlayerClass playerClass = CLASS_ROUTE_ORDER.get(classIndex);
				List<QuestReward> classItems = classRewards.get(classRewardKey(playerClass));
				if (classItems == null) {
					continue;
				}
				for (int itemIndex = 0; itemIndex < classItems.size(); itemIndex++) {
					List<QuestAction> actions = new ArrayList<>(fixedRewards);
					actions.add(grant(classItems.get(itemIndex)));
					actions.add(new QuestAction.CompleteQuest(0));
					addRewardWindowRoutes(flow, rewardNpcs,
						QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id(),
						List.of(new QuestCondition.AdvancedClassIs(playerClass)), actions, closeWindow,
						itemIndex * CLASS_ROUTE_ORDER.size() + classIndex, completeLabel, rewardLabel);
				}
			}
			return List.copyOf(flow);
		}
		if (!selectables.isEmpty()) {
			int slots = Math.min(selectables.size(), QuestDialogAction.AUTO_REWARD_SLOT_COUNT);
			for (int slot = 0; slot < slots; slot++) {
				List<QuestAction> actions = new ArrayList<>(fixedRewards);
				actions.add(grant(selectables.get(slot)));
				actions.add(new QuestAction.CompleteQuest(0));
				addRewardWindowRoutes(flow, rewardNpcs,
					QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + slot, List.of(), actions,
					closeWindow, null, completeLabel, rewardLabel);
			}
			return List.copyOf(flow);
		}
		List<QuestAction> actions = new ArrayList<>(fixedRewards);
		actions.add(new QuestAction.CompleteQuest(0));
		addRewardWindowRoutes(flow, rewardNpcs, QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id(), List.of(),
			actions, closeWindow, null, completeLabel, rewardLabel);
		return List.copyOf(flow);
	}

	/** 同一逻辑交付路由的双协议注册（QuestDialog 无 NPC 域整族一次 + TalkToNpc 报告 NPC 键逐实例）。 /
	 * Dual-protocol registration: the npc-agnostic dialog-page channel once per family, the
	 * report-npc channel per instance (single-npc rows keep the historical two-line shape). */
	private static void addRewardWindowRoutes(List<QuestTransition> flow,
			java.util.Collection<Integer> rewardNpcs, int dialogId, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit, Integer priority,
			String completeLabel, String rewardLabel) {
		flow.add(new QuestTransition(new QuestEvent.QuestDialog(dialogId), List.copyOf(conditions),
			List.copyOf(actions), completeLabel, afterCommit, priority, rewardLabel));
		for (int rewardNpc : rewardNpcs) {
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, dialogId),
				List.copyOf(conditions), List.copyOf(actions), completeLabel, afterCommit, priority,
				rewardLabel));
		}
	}

	/** 职业完成路由的规范顺序（npc-complete CLASS 形的 11 个高阶职业）。 / Canonical class-route order. */
	private static final List<PlayerClass> CLASS_ROUTE_ORDER = List.of(PlayerClass.GLADIATOR,
		PlayerClass.TEMPLAR, PlayerClass.RANGER, PlayerClass.ASSASSIN, PlayerClass.SORCERER,
		PlayerClass.SPIRIT_MASTER, PlayerClass.CLERIC, PlayerClass.CHANTER, PlayerClass.GUNSLINGER,
		PlayerClass.SONGWEAVER, PlayerClass.AETHERTECH);

	/**
	 * 无条件交付兜底路由的优先级：必须大于全部职业路由优先级（职业路由
	 * priority = itemIndex * 11 + classIndex，可选槽至多 15 时上界 10 + 14 * 11 = 164）。
	 * Priority of the unconditional delivery fallback: greater than every class-route priority
	 * (class routes use itemIndex * 11 + classIndex, bounded by 10 + 14 * 11 = 164 for at most
	 * 15 selectable slots).
	 */
	private static final int REWARD_FALLBACK_PRIORITY = 176;

	private static QuestAction.GrantReward grant(QuestReward reward) {
		QuestRewardKind kind = QuestRewardKind.fromWire(reward.kind());
		QuestRewardKind actionKind = kind == QuestRewardKind.SELECTABLE_ITEM ? QuestRewardKind.ITEM : kind;
		QuestRewardAmountMode mode = switch (actionKind) {
			case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
			default -> QuestRewardAmountMode.EXACT;
		};
		return new QuestAction.GrantReward(actionKind.name(), reward.id(), reward.amount(), mode);
	}

	/**
	 * 高阶职业 → class_rewards 元数据键（与 {@code QuestXmlBlockExpander#classRewardKey} 同一映射）。
	 * Advanced class to the classRewards metadata key (same mapping as the XML expander).
	 */
	private static String classRewardKey(PlayerClass playerClass) {
		return switch (playerClass) {
			case GLADIATOR -> "FIGHTER";
			case TEMPLAR -> "KNIGHT";
			case RANGER -> "RANGER";
			case ASSASSIN -> "ASSASSIN";
			case SORCERER -> "WIZARD";
			case SPIRIT_MASTER -> "ELEMENTALIST";
			case CLERIC -> "PRIEST";
			case CHANTER -> "CHANTER";
			case GUNSLINGER -> "GUNSLINGER";
			case SONGWEAVER -> "SONGWEAVER";
			case AETHERTECH -> "AETHERTECH";
			default -> throw new IllegalArgumentException("unsupported advanced class " + playerClass);
		};
	}

	static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			Integer priority, List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, priority, source);
	}
}
