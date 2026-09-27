package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.ProgressScope;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 真端 SimpleCollectItem 表行 → 完整任务定义（无 shell）的合成器。
 * <p>
 * 主导形状（M5-a 勘察：138 个名称可解析行中 105 个同形；P0-2 起规范形生命周期）：
 * <ol>
 * <li>接取：QUEST_SELECT 直发询问窗（页 4）+ 真端表 {@code SETPRO1} 备选接取路由；SELECT1 入口页、
 *     1007 中转与 SELECT1_1 续页梯删除；</li>
 * <li>推进：每个 {@code objectN} 一条对象对话路由 + 一条 {@code CAN_ACT(template, ACTION_ITEM_USE)} 路由
 *     （掉落生效行 = {@code collect_progress}）；中间 NPC 的 QUEST_SELECT 一步推进到采集行
 *     （select2 页链删除）；</li>
 * <li>交付：QUEST_SELECT 带整组 HasItem 门控直翻 REWARD 并按档位下发奖励窗；报告页（SELECT5）与
 *     {@code 39/20002} 检查对随页链删除；</li>
 * <li>完成：按 {@code npc-complete} 展开（预览窗 + 奖励确认 8..23，每条发固定奖励并 complete）。</li>
 * </ol>
 * 其它形状（选择型奖励、异形事件）按稳定拒绝码留在 XML 降级。
 * <p>
 * Synthesizes the dominant retail SimpleCollectItem shape under the canonical native lifecycle
 * (since quest-native-dispatch P0-2); other shapes get stable rejection codes.
 */
public final class RetailSimpleCollectItemDefinitionCompiler {

	private static final int FIRST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	private RetailSimpleCollectItemDefinitionCompiler() {
	}

	/** 编译结果（拒绝时 {@code rejectionCode} 稳定可登记）。 / Compilation outcome with a stable rejection code. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 编译一行（唯一生产入口）：真端行声明中间 NPC（{@code talk_npc1}）且掉落生效步
	 * （{@code collect_progress}）大于 0 时，先与该 NPC 走客户端简报页链推进到 {@code var0=collect_progress}
	 * 行，采集对象与交付检查都在该行生效——与旧 XML 的 {@code started --QUEST_SELECT--> SELECT2
	 * --SELECT2_1--> SELECT2_1 --SETPRO1--> v1} 形状一致，也满足启动期
	 * {@code QuestInteractionObjectValidator} 的"掉落步必须有同 var0 的 ACTION_ITEM_USE 路由"合同。
	 * <p>
	 * 中间 NPC 名与客户端页链都在本方法内解析：未解析/多解/未登记页链 → 稳定拒绝码（该行保留 XML）。
	 * 生产驱动与门禁夹具都必须走这个入口，否则夹具会测到与生产不同的形状（P0c-5b 的"假绿"教训）。
	 * The single production entry point for one row: the mid NPC name and the client briefing page chain are
	 * resolved here so that the driver and the gate fixtures cannot drift apart. Unresolved, ambiguous or
	 * unregistered briefing steps yield stable rejection codes and the row stays on XML.
	 */
	public static Outcome compile(RetailSimpleCollectItemTable.Entry entry, RetailNpcNameIndex npcIndex,
			RetailQuestMetadataCompiler.Outcome metadata, RetailClientDialogExits exits,
			RetailClientSummaryRows summaryRows, RetailClientRewardNpcs clientRewardNpcs) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(npcIndex, "npcIndex");
		String briefingNpcName = entry.talkNpc() == null ? "" : entry.talkNpc();
		Set<Integer> briefingNpcIds = npcIndex.resolveAll(List.of(briefingNpcName)).npcIds();
		return compile(entry, npcIndex, metadata, exits, summaryRows, clientRewardNpcs,
			new BriefingStep(briefingNpcIds, briefingNpcName));
	}

	/**
	 * 简报步骤（{@code talk_npc1}）的解析结果：NPC 候选集 + 真端原始名（客户端页链自 W5-g4 起不再登记，
	 * 覆盖不变量由构建期门禁 {@code RetailBriefingChainEvidenceGateTest} 承担）。
	 * Resolved briefing step: npc candidates plus the retail raw name (the client-chain registry retired in
	 * W5-g4; the coverage invariant moved to the build-time evidence gate).
	 */
	private record BriefingStep(Set<Integer> npcIds, String name) {

		/** 该行是否声明了中间 NPC。 / Whether the row declares a mid NPC. */
		boolean declared() {
			return name != null && !name.isBlank();
		}
	}

	/**
	 * 编译一行的内部分支：{@code briefing} 由 {@link #compile} 解析后传入。
	 * Internal branch of {@link #compile}; the briefing pair is resolved by the public entry point.
	 */
	private static Outcome compile(RetailSimpleCollectItemTable.Entry entry, RetailNpcNameIndex npcIndex,
			RetailQuestMetadataCompiler.Outcome metadata, RetailClientDialogExits exits,
			RetailClientSummaryRows summaryRows, RetailClientRewardNpcs clientRewardNpcs,
			BriefingStep briefing) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(npcIndex, "npcIndex");
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(exits, "exits");
		Objects.requireNonNull(summaryRows, "summaryRows");
		Objects.requireNonNull(clientRewardNpcs, "clientRewardNpcs");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		Objects.requireNonNull(briefing, "briefing");
		Outcome blocked = precheck(entry, npcIndex, metadata.metadata(), clientRewardNpcs, briefing);
		if (blocked != null) {
			return blocked;
		}
		try {
			QuestDefinition definition = build(entry, npcIndex, metadata.metadata(), exits, summaryRows,
				clientRewardNpcs, briefing);
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 迁移判据（稳定拒绝码）：
	 * <ul>
	 * <li>{@code RETAIL_ACQUIRE_NPC_*}/{@code RETAIL_REWARD_NPC_*}/{@code RETAIL_COLLECT_OBJECT_*}：名字解析不到/多解/哨兵；
	 * 未知哨兵（不对应任何真端发放系统）仍按哨兵拒绝；已知发放哨兵（{@code _faction_} 等）走系统发放形状；</li>
	 * <li>{@code RETAIL_COLLECT_ITEM_SHAPE}：真端 quest.xml 的交付物为空（多交付物按整组检查/扣除，
	 * 与 npc-item-report 检查对同构，P1b 起支持）；</li>
	 * <li>{@code RETAIL_COLLECT_SELECTABLE_REWARD}：可选奖励超过 16 项或多奖励档（客户端确认动作 8..23
	 * 共 16 个，P1b 起按 npc-complete choice 展开逐项路由）。</li>
	 * </ul>
	 * Migration pre-check with stable rejection codes.
	 */
	private static Outcome precheck(RetailSimpleCollectItemTable.Entry entry, RetailNpcNameIndex npcIndex,
			QuestMetadata metadata, RetailClientRewardNpcs clientRewardNpcs, BriefingStep briefing) {
		if (briefing.declared()) {
			if (briefing.npcIds().isEmpty()) {
				return new Outcome(null, "RETAIL_TALK_NPC_UNRESOLVED", briefing.name());
			}
			if (briefing.npcIds().size() > 1) {
				return new Outcome(null, "RETAIL_TALK_NPC_AMBIGUOUS", briefing.name());
			}
			// 简报步骤的落点是掉落生效步（真端 collect_progress）；该列为 0 时没有可推进的采集行，
			// 简报链无处接线，同样保留 XML。
			// The briefing step lands on the retail collect_progress row; without it there is no collect
			// row to advance to, so the row stays on XML.
			if (metadata.drops().stream().noneMatch(drop -> drop.collectingStep() > 0)) {
				return new Outcome(null, "RETAIL_TALK_NPC_WITHOUT_COLLECT_STEP", briefing.name());
			}
		}
		if (entry.grantKind() == RetailGrantKind.UNKNOWN_SENTINEL) {
			return new Outcome(null, "RETAIL_ACQUIRE_NPC_SENTINEL",
				entry.acquiredNpc() + " -> 无对应真端发放系统");
		}
		if (!entry.grantKind().systemGrant()) {
			Outcome acquired = requireNpc(npcIndex, entry.acquiredNpc(), "ACQUIRE");
			if (acquired != null) {
				return acquired;
			}
		}
		Outcome reward = requireNpc(npcIndex, entry.rewardNpc(), "REWARD");
		if (reward != null) {
			// 真端家族表的奖励引用可以是 {@code <地图>_<势力名>} 复合名（如 LDF5a_Silverlin_L）——
			// 不是 NPC 名（真端 npcs.xml 亦无此名）。交付 NPC 集由客户端任务书 dic 链登记
			// （{@code quest_client_reward_npcs.tsv}，P1a）；登记缺失才拒绝。
			// Composite reward references resolve through the client quest-letter dic chain registry;
			// missing registry coverage still rejects.
			if (entry.grantKind().systemGrant() && RetailQuestMetadataCompiler.isFactionComposite(entry.rewardNpc())
				&& clientRewardNpcs.rewardNpcs(entry.questId()).isEmpty()) {
				return new Outcome(null, "RETAIL_REWARD_NPC_FACTION_COMPOSITE", entry.rewardNpc());
			}
			if (!RetailQuestMetadataCompiler.isFactionComposite(entry.rewardNpc())) {
				return reward;
			}
		}
		if (entry.objects().isEmpty()) {
			return new Outcome(null, "RETAIL_COLLECT_OBJECT_UNRESOLVED", "objectN 为空");
		}
		for (String object : entry.objects()) {
			Set<Integer> ids = npcIndex.resolveAll(List.of(object)).npcIds();
			if (ids.size() != 1) {
				String kind = ids.size() > 1 ? "_AMBIGUOUS" : "_UNRESOLVED";
				return new Outcome(null, "RETAIL_COLLECT_OBJECT" + kind, object + " -> " + ids);
			}
		}
		if (metadata.itemRequirements().isEmpty()) {
			return new Outcome(null, "RETAIL_COLLECT_ITEM_SHAPE", "真端交付物为空");
		}
		if (selectableRewards(metadata).size() > LAST_CONFIRM_ACTION - FIRST_CONFIRM_ACTION + 1) {
			return new Outcome(null, "RETAIL_COLLECT_SELECTABLE_REWARD",
				selectableRewards(metadata).toString());
		}
		if (metadata.rewardGroups().size() > 1) {
			return new Outcome(null, "RETAIL_COLLECT_SELECTABLE_REWARD",
				"multi-tier rewards: " + metadata.rewardGroups().size() + " groups");
		}
		return null;
	}

	/** 单个 NPC 名判定：唯一 → 放行；空集/多解按名字形态归类。 / Single-NPC gate. */
	private static Outcome requireNpc(RetailNpcNameIndex index, String name, String role) {
		Set<Integer> ids = index.resolveAll(List.of(name == null ? "" : name)).npcIds();
		if (ids.size() == 1) {
			return null;
		}
		String raw = name == null ? "" : name.trim();
		if (ids.size() > 1) {
			return new Outcome(null, "RETAIL_" + role + "_NPC_AMBIGUOUS", raw + " -> " + ids);
		}
		String kind = raw.length() > 2 && raw.startsWith("_") && raw.endsWith("_")
			? "_NPC_SENTINEL" : "_NPC_UNRESOLVED";
		return new Outcome(null, "RETAIL_" + role + kind, raw);
	}

	private static List<QuestReward> selectableRewards(QuestMetadata metadata) {
		List<QuestReward> group = metadata.rewardGroups().isEmpty()
			? List.of() : metadata.rewardGroups().get(0).rewards();
		return group.stream()
			.filter(reward -> QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM)
			.toList();
	}

	private static QuestDefinition build(RetailSimpleCollectItemTable.Entry entry, RetailNpcNameIndex npcIndex,
			QuestMetadata metadata, RetailClientDialogExits exits, RetailClientSummaryRows summaryRows,
			RetailClientRewardNpcs clientRewardNpcs, BriefingStep briefing) {
		// 系统发放形状（M5-b3x）：接取名是真端类别哨兵（_faction_ 等）时，真端没有 NPC 接取——
		// 普查 43/43 无生命周期三元组、客户端 40/40 只有 ask_quest_accept；发放走真端系统
		// （阵营星期位 npcfactions_quest / 挑战任务 / quest_area），定义只保留报告 + 采集对象 + 领奖。
		// System-grant shape: sentinel acquire names have no NPC accept in retail (census: no lifecycle
		// triplet; client letter page only); grants come from the retail grant systems, so the definition
		// keeps only report + collect objects + rewards.
		boolean systemGrant = entry.grantKind().systemGrant();
		int acquiredNpc = systemGrant ? -1
			: npcIndex.resolveAll(List.of(entry.acquiredNpc())).npcIds().iterator().next();
		// 交付 NPC：唯一名解析优先；复合名（系统发放行）走客户端任务书 dic 链登记，可点名 2–3 个支部 NPC。
		// Hand-in NPCs: unique name resolution first; composite names resolve through the client
		// quest-letter registry (2-3 branch NPCs).
		Set<Integer> resolvedReward = npcIndex.resolveAll(List.of(entry.rewardNpc())).npcIds();
		List<Integer> rewardNpcs = resolvedReward.size() == 1
			? List.of(resolvedReward.iterator().next())
			: clientRewardNpcs.rewardNpcs(entry.questId());
		List<Integer> objectIds = new ArrayList<>(entry.objects().size());
		for (String object : entry.objects()) {
			objectIds.add(npcIndex.resolveAll(List.of(object)).npcIds().iterator().next());
		}
		List<QuestItemRequirement> handIns = metadata.itemRequirements();
		// 掉落生效步 = 真端 collect_progress（掉落只在任务书该行计数）。有中间 NPC 时先对话推进到该行，
		// 采集对象与交付检查都挂在 v{step} 上；否则维持 started 单行形状（step=0 的老口径）。
		// The collect step is the retail collect_progress; when the row names a mid NPC, the object and
		// hand-in routes live on v{step} after that talk, otherwise the single-row started shape stays.
		int collectStep = metadata.drops().stream().mapToInt(QuestDrop::collectingStep).max().orElse(0);
		boolean talksFirst = briefing.declared() && collectStep > 0;
		int talkNpc = talksFirst ? briefing.npcIds().iterator().next() : -1;
		String collectSource = talksFirst ? "v" + collectStep : "started";

		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		// 领奖投影 = 客户端任务书末行行号（QE-051）；真端模板表没有任务书行数，
		// 行数来自客户端 quest_summary 页登记（与 SimpleTalk 同一口径）。
		// Reward projection = the last client journal row index (QE-051), same rule as SimpleTalk.
		Map<String, Integer> rewardRow = Map.of("var0", summaryRows.lastRowIndex(entry.questId()));
		List<QuestNode> nodes = new ArrayList<>();
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)));
		nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, zero)));
		if (talksFirst) {
			nodes.add(new QuestNode(collectSource,
				new NodeProjection(QuestStatus.START, Map.of("var0", collectStep))));
		}
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));

		List<QuestTransition> transitions = new ArrayList<>();
		if (!systemGrant) {
			// 规范形接取（quest-native-dispatch P0-2）：页 4 直发，SELECT1 入口页/1007 中转/续页梯删除。
			// Canonical accept (quest-native-dispatch P0-2): page 4 directly; the SELECT1 entry page,
			// the 1007 hop and the SELECT1_1 ladder are gone.
			transitions.addAll(canonicalAcceptFlow(acquiredNpc));
			transitions.add(setproRoute(acquiredNpc));
		} else {
			// 系统发放边：定义图在无接取路由时仍从 NONE 连通到 START；发放服务（阵营日常轮换等）
			// 后续显式分发 {@link QuestEvent.SystemGrant}，或按 RetailAreaEngine 先例直启。
			// The system-grant edge keeps the definition graph connected without accept routes;
			// grant services dispatch SystemGrant explicitly or start the quest directly.
			transitions.add(new QuestTransition(new QuestEvent.SystemGrant(),
				List.of(new QuestCondition.StartEligible()), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		}
		if (talksFirst) {
			// 规范形简报（quest-native-dispatch P0-2）：QUEST_SELECT 一步直达采集行，select2 页链删除。
			// Canonical briefing (quest-native-dispatch P0-2): QUEST_SELECT lands on the collect row in
			// one step; the select2 page chain is gone.
			transitions.addAll(canonicalBriefingFlow(talkNpc, collectSource));
		}
		for (int objectId : new LinkedHashSet<>(objectIds)) {
			transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(objectId), List.of(), List.of(),
				collectSource, List.of(), null, collectSource));
			transitions.add(new QuestTransition(new QuestEvent.CanAct(objectId, "ACTION_ITEM_USE"), List.of(),
				List.of(), collectSource, List.of(), null, collectSource));
		}
		// 规范形交付窗：统一解析（有奖励组按档位查表 QE-028，零组回落固定窗 1）。
		// The canonical delivery window resolves through the shared helper.
		int rewardWindowPage = deliveryWindowPage(metadata, entry.questId());
		List<QuestCondition> hasItems = handIns.stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> removeItems = handIns.stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		for (int rewardNpc : rewardNpcs) {
			// 规范形交付（quest-native-dispatch P0-2）：QUEST_SELECT 带整组 HasItem 门控直翻 REWARD 并
			// 下发档位奖励窗；报告页（SELECT5）与 39/20002 检查对随页链删除——未集齐时零路由，
			// 关窗兜底交给 DialogService。
			// Canonical delivery (quest-native-dispatch P0-2): QUEST_SELECT gated by the whole hand-in
			// set flips REWARD and shows the tiered window; the SELECT5 report page and the 39/20002
			// check pairs are gone — an incomplete hand-in has no route and DialogService closes.
			transitions.add(canonicalDelivery(rewardNpc, hasItems, removeItems, rewardWindowPage, collectSource));
			transitions.addAll(reportNpcExit(systemGrant, acquiredNpc, rewardNpc, collectSource));
		}
		transitions.addAll(completeFlow(entry.questId(), rewardNpcs, metadata));
		transitions.addAll(journalRowRepair(rewardRow.get("var0")));
		return new QuestDefinition(entry.questId(), 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/** 接取流：与 {@code npc-start} 展开同构。 / The canonical npc-start expansion. */
	static List<QuestTransition> acceptFlow(int acquiredNpc) {
		String source = "unaccepted";
		String target = "started";
		List<QuestCondition> eligible = List.of(new QuestCondition.StartEligible());
		List<QuestTransition> flow = new ArrayList<>(9);
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_SELECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id()))));
		flow.add(talk(acquiredNpc, QuestDialogAction.ASK_QUEST_ACCEPT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
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

	/** 真端表自带的备选接取路由 {@code SETPRO1}（133/138 行有）。 / The retail {@code SETPRO1} accept variant. */
	static QuestTransition setproRoute(int acquiredNpc) {
		return new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.SETPRO1.id()),
			List.of(new QuestCondition.StartEligible()), List.of(), "started",
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), null, "unaccepted");
	}

	/**
	 * 领奖行修复路由（QE-051 的本服兼容面）：客户端任务书存在多行时，末行行号 > 0；
	 * 旧版本的领奖投影把多行任务停留在第 0 行，已持久化的存档进入世界时纠正为末行。
	 * <p>
	 * 真端表没有"任务书行数"，本路由与领奖投影同源（都来自客户端 quest_summary 登记），
	 * 只有多行任务（末行 > 0）才可能产生陈旧值，因此单行任务不登记该路由。
	 * Journal-row repair for saves written by the older projection; derived from the same client
	 * journal registry as the reward projection and emitted only for multi-row quests.
	 */
	static List<QuestTransition> journalRowRepair(int lastRow) {
		if (lastRow <= 0) {
			return List.of();
		}
		return List.of(new QuestTransition(new QuestEvent.EnterWorld(),
			List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 0)),
			List.of(new QuestAction.SetVariable("var0", lastRow)), "reward",
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			null, null));
	}

	/**
	 * 规范形接取流（quest-native-dispatch P0-2）：QUEST_SELECT 直发询问窗（页 4），1002/20000 两形
	 * 提交、拒绝族与 FINISH_DIALOG 关窗保持生命周期语义；SELECT1(1011) 入口页、1007 中转删除。
	 * Canonical accept flow (quest-native-dispatch P0-2): QUEST_SELECT shows the ask-accept window
	 * (page 4) directly; the 1002/20000 commits, the refuse family and the FINISH_DIALOG close keep
	 * their lifecycle semantics; the SELECT1(1011) entry page and the 1007 hop are gone.
	 */
	static List<QuestTransition> canonicalAcceptFlow(int acquiredNpc) {
		String source = "unaccepted";
		String target = "started";
		List<QuestCondition> eligible = List.of(new QuestCondition.StartEligible());
		List<QuestTransition> flow = new ArrayList<>(9);
		flow.add(talk(acquiredNpc, QuestDialogAction.QUEST_SELECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
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
	 * 交付窗页解析（单一真源）：有奖励组按档位查表（QE-028）；**零奖励组**的行（事件任务等）回落固定
	 * 窗 1——与完成流的预览出口（{@code SHOW_SELECT_QUEST_REWARD_WINDOW1}）同口径，禁分档推算。
	 * Delivery window page (one source of truth): tier lookup for reward groups (QE-028); a zero-group
	 * row (event quests) falls back to the fixed window 1, the same caliber as the completion-flow
	 * preview exits.
	 */
	static int deliveryWindowPage(QuestMetadata metadata, int questId) {
		if (metadata.rewardGroups().isEmpty()) {
			return QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id();
		}
		return QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1)
			.orElseThrow(() -> new IllegalArgumentException(
				"reward tiers exceed the six client reward windows: " + questId)).id();
	}

	/**
	 * 规范形交付边（quest-native-dispatch P0-2）：QUEST_SELECT 带整组 HasItem 门控直翻 REWARD 并下发
	 * 档位奖励窗（调用方按 QE-028 查表）；报告页（SELECT5）与 39/20002 检查对随页链删除——未集齐时零路由，
	 * 关窗兜底交给 DialogService。采集族与 DD 交付行共用本形（单一真源）。
	 * Canonical delivery edge: QUEST_SELECT gated by the whole hand-in set flips REWARD and shows the
	 * tiered window (the caller does the QE-028 lookup); the SELECT5 report page and the 39/20002 check
	 * pairs are gone — an incomplete hand-in has no route and DialogService closes. The collect family
	 * and the DD hand-in rows share this shape (one source of truth).
	 */
	static QuestTransition canonicalDelivery(int rewardNpc, List<QuestCondition> hasItems,
			List<QuestAction> removeItems, int rewardWindowPage, String collectSource) {
		return new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.QUEST_SELECT.id()),
			hasItems, removeItems, "reward",
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(rewardWindowPage)), null, collectSource);
	}

	/**
	 * 规范形简报（quest-native-dispatch P0-2）：中间 NPC 的 QUEST_SELECT 一步直达采集行
	 * {@code v{collect_progress}}；select2 页链不再由服务端驱动，LEVEL_AND_VISIBILITY_REFRESH 让
	 * 任务书从"去见中间 NPC"行切到采集行（掉落门 {@code var0 == collectingStep} 随目标节点生效）。
	 * Canonical briefing (quest-native-dispatch P0-2): the mid NPC's QUEST_SELECT lands on the collect
	 * row {@code v{collect_progress}} in one step; the select2 page chain is no longer server-driven,
	 * and LEVEL_AND_VISIBILITY_REFRESH re-renders the journal onto the collect row (the drop gate
	 * {@code var0 == collectingStep} takes effect through the target node).
	 */
	private static List<QuestTransition> canonicalBriefingFlow(int briefingNpc, String collectSource) {
		return List.of(talk(briefingNpc, QuestDialogAction.QUEST_SELECT, "started", collectSource, null,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog())));
	}

	/**
	 * 报告 NPC 的关窗出口：交付检查失败会下发 select6 页，该页按钮是 {@code HACTION_FINISH_DIALOG}。
	 * 普通行里接取 NPC 相同时接取流已登记同形路由（{@code started + FINISH_DIALOG}），重复登记会让 IR 判定
	 * {@code AMBIGUOUS_TRANSITION}；系统发放行没有接取流，报告 NPC 关窗出口必须无条件登记。
	 * Close-dialog exit for the report NPC; skipped for normal rows when both NPCs coincide
	 * (same canonical route), mandatory for system-grant rows (no accept flow exists).
	 */
	static List<QuestTransition> reportNpcExit(boolean systemGrant, int acquiredNpc, int rewardNpc,
			String collectSource) {
		return reportNpcExit(!systemGrant && acquiredNpc == rewardNpc, rewardNpc, collectSource);
	}

	/** 变体家族形：调用方以「奖励 NPC 是否属于接取变体集」决定是否省略关窗出口。 /
	 * Variant-family shape: the caller decides by whether the reward npc is in the acquire set. */
	static List<QuestTransition> reportNpcExit(boolean skipExit, int rewardNpc, String collectSource) {
		if (skipExit) {
			return List.of();
		}
		return List.of(talk(rewardNpc, QuestDialogAction.FINISH_DIALOG, collectSource, collectSource, null,
			List.of(new AfterCommitAction.CloseDialog())));
	}

	/**
	 * 接取续页流：真端模板表没有"页链"列，{@code SELECT1_1} 来自客户端对话出口登记表
	 * （客户端 5.8 的 select1 页按钮 {@code HACTION_SELECT1_1}）。
	 * Accept continuation derived from the client dialog exit registry.
	 */
	static List<QuestTransition> acceptContinuation(int acquiredNpc, boolean continues) {
		List<QuestTransition> flow = new ArrayList<>(2);
		flow.add(talk(acquiredNpc, QuestDialogAction.SELECT1_1, "unaccepted", "unaccepted", null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id()))));
		if (continues) {
			flow.add(talk(acquiredNpc, QuestDialogAction.SELECT1_1_1, "unaccepted", "unaccepted", null,
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1_1.id()))));
		}
		return List.copyOf(flow);
	}

	/**
	 * 交付检查：与 {@code npc-item-report} 展开同构（动作 39 / 20002）；失败页按客户端是否有 select6 决定。
	 * 多交付物按整组检查/扣除（P1b：与 XML 的多条 {@code has-item}/{@code remove-item} 同构）。
	 * Canonical npc-item-report expansion; the failure page follows the client select6 exit.
	 * Multi-item quests check and remove the whole set in one pair (P1b).
	 */
	static List<QuestTransition> itemReport(int rewardNpc, List<QuestItemRequirement> items,
			boolean hasFailurePage, String source) {
		List<QuestCondition> hasItems = items.stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> removeItems = items.stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		List<AfterCommitAction> success = List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
		return List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
				hasItems, removeItems, "reward", success, 0, source),
			new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
				List.of(), List.of(), source, List.of(hasFailurePage
					? new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())
					: new AfterCommitAction.CloseDialog()), 1, source),
			new QuestTransition(
				new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()),
				hasItems, removeItems, "reward", success, 0, source),
			new QuestTransition(
				new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()),
				List.of(), List.of(), source, List.of(new AfterCommitAction.CloseDialog()), 1, source));
	}

	/**
	 * 完成流：与 {@code npc-complete} 展开同构。无可选奖励 = 预览窗 + 确认区间 8..23 每条发固定奖励；
	 * 有可选奖励 = 按 choice 逐项路由（第 k 个可选项绑确认动作 8+k，动作为固定奖励 + 该项 + 完成），
	 * 与 XML 的 {@code <choice action="SELECTED_QUEST_REWARDk" reward-index="N">} 逐一对齐（P1b）。
	 * Canonical npc-complete expansion: fixed-only quests confirm on the full 8..23 range, quests with
	 * selectable rewards bind confirm action 8+k to the k-th selectable reward (P1b).
	 */
	static List<QuestTransition> completeFlow(int questId, int rewardNpc, QuestMetadata metadata) {
		return completeFlow(questId, java.util.List.of(rewardNpc), metadata);
	}

	/** 变体家族形：对话页确认梯逐实例，奖励窗口整族一份（对话页通道无 NPC 域只发一次）。 /
	 * Family shape: the talk-page confirm ladder per instance, one reward window for the family. */
	static List<QuestTransition> completeFlow(int questId, java.util.Collection<Integer> rewardNpcs,
			QuestMetadata metadata) {
		if (metadata.rewardGroups().size() > 1) {
			throw new IllegalArgumentException("multi-tier rewards not supported: "
				+ metadata.rewardGroups().size() + " groups");
		}
		List<QuestReward> group = metadata.rewardGroups().isEmpty()
			? List.of() : metadata.rewardGroups().get(0).rewards();
		List<QuestReward> selectables = selectableRewards(metadata);
		List<QuestAction> fixedRewards = new ArrayList<>();
		for (QuestReward reward : group) {
			if (QuestRewardKind.fromWire(reward.kind()) != QuestRewardKind.SELECTABLE_ITEM) {
				fixedRewards.add(grant(reward));
			}
		}
		List<AfterCommitAction> afterCommit = List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10));
		List<QuestTransition> flow = new ArrayList<>();
		for (int rewardNpc : rewardNpcs) {
			// 领奖窗预览出口（USE_OBJECT / SELECT_QUEST_REWARD）按 NPC 分域，逐实例登记。
			// The reward-window preview exits are npc-scoped and registered per instance.
			for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
					QuestDialogAction.SELECT_QUEST_REWARD)) {
				flow.add(talk(rewardNpc, preview, "reward", "reward", null,
					List.of(new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))));
			}
			if (selectables.isEmpty()) {
				List<QuestAction> withCompletion = new ArrayList<>(fixedRewards);
				withCompletion.add(new QuestAction.CompleteQuest(0));
				for (int id = FIRST_CONFIRM_ACTION; id <= LAST_CONFIRM_ACTION; id++) {
					flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, id), List.of(),
						withCompletion, "complete", afterCommit, null, "reward"));
				}
			} else {
				for (int choice = 0; choice < selectables.size(); choice++) {
					List<QuestAction> actions = new ArrayList<>(fixedRewards);
					actions.add(grant(selectables.get(choice)));
					actions.add(new QuestAction.CompleteQuest(0));
					flow.add(new QuestTransition(
						new QuestEvent.TalkToNpc(rewardNpc, FIRST_CONFIRM_ACTION + choice), List.of(),
						actions, "complete", afterCommit, null, "reward"));
				}
			}
		}
		// 奖励窗口自动确认通道（108 / 110+i，双协议注册 + CloseDialog 收窗）：对话页通道无 NPC 域，
		// 整族只发一次；报告 NPC 通道逐实例（家族形见 rewardWindowAutoFlow 集合重载）。
		// The reward-window auto-confirm channel: the npc-agnostic dialog-page channel runs once per
		// family; the report-npc channel runs per instance (the collection overload).
		flow.addAll(RetailSimpleHuntDefinitionCompiler.rewardWindowAutoFlow(rewardNpcs, fixedRewards,
			selectables, metadata.classRewards(), "reward", "complete"));
		return List.copyOf(flow);
	}

	private static QuestAction grant(QuestReward reward) {
		QuestRewardKind kind = QuestRewardKind.fromWire(reward.kind());
		// 可选物品授予以 ITEM kind 落地（SimpleHunt 职业完成段同形；运行时消费方按 ITEM 发物）。
		// Selectable items grant as the ITEM kind (the SimpleHunt class-completion shape; runtime
		// consumers grant by ITEM).
		QuestRewardKind actionKind = kind == QuestRewardKind.SELECTABLE_ITEM ? QuestRewardKind.ITEM : kind;
		QuestRewardAmountMode mode = switch (kind) {
			case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
			default -> QuestRewardAmountMode.EXACT;
		};
		return new QuestAction.GrantReward(actionKind.name(), reward.id(), reward.amount(), mode);
	}

	static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			Integer priority, List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, priority, source);
	}
}
