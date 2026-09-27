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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 真端 SimpleUseItem 表行 → 完整任务定义的合成器（P3b）。
 * <p>
 * 规范形状（1107 展开口径；P0-2 交付规范形）：用物品接取（{@code UseItem} → 接取确认窗）+
 * 无目标对话接受/拒绝/关窗（{@code QuestDialog 1002/1003/1008}）+ 报告 NPC QUEST_SELECT 直翻
 * REWARD + 分档奖励窗（QE-028；SELECT5 报告页/39·20002 检查对/1009 中转已随页链退役）+
 * npc-complete（预览窗 + 确认 8..23）。道具 id = 表符号去 {@code ITEM_} 前缀转小写后的
 * 物品 {@code name_desc}（全族 104/104 唯一解析）。
 * <p>
 * Synthesizes the canonical retail SimpleUseItem shape: item-use accept, targetless accept dialog,
 * QUEST_SELECT delivery flipping REWARD with the tiered reward window (the SELECT5 report page,
 * the 39/20002 check pair and the 1009 hop are retired with the page chain), and the canonical
 * npc-complete expansion.
 */
public final class RetailSimpleUseItemDefinitionCompiler {

	private static final int FIRST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
	private static final Set<Integer> CAKE_EVENTS = Set.of(80008, 80009);

	private RetailSimpleUseItemDefinitionCompiler() {
	}

	/** 编译结果（拒绝时 {@code rejectionCode} 稳定可登记）。 / Compilation outcome with a stable rejection code. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 编译一行；{@code summaryRows} 提供领奖行投影（QE-051），{@code reportModes} 提供客户端
	 * select5 页的交付模式（CHECK = 39/20002 检查对，REWARD = 1009 直接交付）。
	 * Compiles one row; {@code reportModes} supplies the client report mode per quest.
	 */
	public static Outcome compile(RetailSimpleUseItemTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex, RetailQuestMetadataCompiler.Outcome metadata,
			RetailClientSummaryRows summaryRows, RetailClientUseItemReport reportModes,
			RetailClientDialogExits exits) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(itemIndex, "itemIndex");
		Objects.requireNonNull(npcIndex, "npcIndex");
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(summaryRows, "summaryRows");
		Objects.requireNonNull(reportModes, "reportModes");
		// exits 形参保留以稳定公开签名；SELECT6 失败页出口随页链退役，build 不再消费。
		// The exits parameter stays for signature stability; the SELECT6 failure-page exit is
		// retired with the page chain and no longer consumed by build.
		Objects.requireNonNull(exits, "exits");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		Outcome blocked = precheck(entry, itemIndex, npcIndex);
		if (blocked != null) {
			return blocked;
		}
		try {
			QuestDefinition definition = build(entry, itemIndex, npcIndex, metadata.metadata(), summaryRows,
				reportModes);
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 迁移判据（稳定拒绝码）：
	 * <ul>
	 * <li>{@code RETAIL_USE_ITEM_UNRESOLVED}：道具符号按去 {@code ITEM_} 前缀小写规则解析不到唯一物品；</li>
	 * <li>{@code RETAIL_REWARD_NPC_*}：报告 NPC 名解析不到/多解/哨兵。</li>
	 * </ul>
	 * Migration pre-check with stable rejection codes.
	 */
	private static Outcome precheck(RetailSimpleUseItemTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex) {
		if (resolveItemId(entry, itemIndex) == null) {
			return new Outcome(null, "RETAIL_USE_ITEM_UNRESOLVED", entry.useItemName());
		}
		Set<Integer> ids = npcIndex.resolveAll(List.of(entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
		if (ids.size() == 1) {
			return null;
		}
		String raw = entry.rewardNpc() == null ? "" : entry.rewardNpc().trim();
		if (ids.size() > 1) {
			return new Outcome(null, "RETAIL_REWARD_NPC_AMBIGUOUS", raw + " -> " + ids);
		}
		String kind = raw.length() > 2 && raw.startsWith("_") && raw.endsWith("_")
			? "_NPC_SENTINEL" : "_NPC_UNRESOLVED";
		return new Outcome(null, "RETAIL_REWARD_NPC" + kind, raw);
	}

	/**
	 * 道具符号 → 本服物品 id：去 {@code ITEM_} 前缀转小写 = 物品 {@code name_desc}。
	 * Resolves the use-item symbol by stripping the {@code ITEM_} prefix and lower-casing.
	 */
	private static Integer resolveItemId(RetailSimpleUseItemTable.Entry entry, RetailItemNameIndex itemIndex) {
		String symbol = entry.useItemName();
		if (symbol == null || symbol.isBlank()) {
			return null;
		}
		String stem = symbol.toLowerCase(Locale.ROOT);
		Integer direct = itemIndex.resolve(stem);
		if (direct != null) {
			return direct;
		}
		if (stem.startsWith("item_")) {
			return itemIndex.resolve(stem.substring("item_".length()));
		}
		return null;
	}

	private static QuestDefinition build(RetailSimpleUseItemTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex, QuestMetadata metadata, RetailClientSummaryRows summaryRows,
			RetailClientUseItemReport reportModes) {
		int questId = entry.questId();
		int useItemId = resolveItemId(entry, itemIndex);
		int rewardNpc = npcIndex.resolveAll(List.of(entry.rewardNpc())).npcIds().iterator().next();
		boolean checkHandIn = reportModes.mode(questId) == RetailClientUseItemReport.Mode.CHECK
			&& reportModes.checkItemId(questId) > 0;
		int checkItemId = checkHandIn ? reportModes.checkItemId(questId) : useItemId;
		boolean cakeEvent = CAKE_EVENTS.contains(questId);
		if (cakeEvent && !metadata.questWorkItems().contains(new QuestItemRequirement(useItemId, 1))) {
			throw new IllegalArgumentException("cake event work item does not match its use item: " + questId);
		}

		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		// 领奖投影 = 客户端任务书末行行号（QE-051）。 / Reward projection = last client journal row (QE-051).
		Map<String, Integer> rewardRow = Map.of("var0", summaryRows.lastRowIndex(questId));
		List<QuestNode> nodes = List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)),
			new QuestNode("started", new NodeProjection(QuestStatus.START, zero)),
			new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)),
			new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));

		List<QuestTransition> transitions = new ArrayList<>();
		// 用物品接取：右键道具 → 接取确认窗。 / Using the item pops the accept window.
		transitions.add(new QuestTransition(new QuestEvent.UseItem(useItemId, 0), List.of(), List.of(),
			"unaccepted", List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), null, "unaccepted"));
		// 无目标对话：接受（1002）/ 拒绝（1003）/ 关窗（1008）。
		// Targetless dialog: accept (1002) / refuse (1003) / finish (1008).
		transitions.add(new QuestTransition(new QuestEvent.QuestDialog(QuestDialogAction.QUEST_ACCEPT_1.id()),
			List.of(new QuestCondition.StartEligible()), List.of(), "started",
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), null, "unaccepted"));
		transitions.add(new QuestTransition(new QuestEvent.QuestDialog(QuestDialogAction.QUEST_REFUSE_1.id()),
			List.of(), List.of(), "unaccepted", List.of(new AfterCommitAction.CloseDialog()), null,
			"unaccepted"));
		transitions.add(new QuestTransition(new QuestEvent.QuestDialog(QuestDialogAction.FINISH_DIALOG.id()),
			List.of(), List.of(), "unaccepted", List.of(new AfterCommitAction.CloseDialog()), null,
			"unaccepted"));
		// 报告/交付规范形（P0-2）：QUEST_SELECT 直翻 REWARD + 分档查表奖励窗（QE-028）——三种客户端
		// 交付模式统一到同一入口：CHECK = QUEST_SELECT 带交付物 HasItem 门控；蛋糕事件带工作物品门控
		// 并保持领奖投影落点；默认 = 无条件直翻。SELECT5 报告页、39/20002 检查对与 1009 中转删除；
		// 未集齐零对话路由（DialogService 关窗兜底）。
		// Canonical report/delivery (P0-2): QUEST_SELECT flips REWARD with the tiered reward window
		// (QE-028) — all three client delivery modes converge on the same entry: CHECK gates on the
		// hand-in HasItem, the cake event gates on its work item and keeps the reward projection,
		// default flips unconditionally. The SELECT5 page, the 39/20002 check pair and the 1009 hop
		// are gone; an incomplete hand-in has zero dialog routes (DialogService closes).
		int rewardWindowPage = QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1)
			.orElseThrow(() -> new IllegalArgumentException(
				"reward tiers exceed the six client reward windows: " + questId)).id();
		QuestEvent.TalkToNpc deliver = new QuestEvent.TalkToNpc(rewardNpc,
			QuestDialogAction.QUEST_SELECT.id());
		if (checkHandIn) {
			transitions.add(new QuestTransition(deliver,
				List.of(new QuestCondition.HasItem(checkItemId, 1)),
				List.of(new QuestAction.RemoveItem(checkItemId, 1)), "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(rewardWindowPage)), null, "started"));
		} else if (cakeEvent) {
			// 真端工作物品门控蛋糕交付；SetVariable 保持领奖投影 = 客户端任务书末行（QE-051）。 /
			// The retail work item gates the cake delivery; SetVariable keeps the reward projection
			// on the last client journal row (QE-051).
			transitions.add(new QuestTransition(deliver,
				List.of(new QuestCondition.HasItem(useItemId, 1)),
				List.of(new QuestAction.RemoveItem(useItemId, 1), new QuestAction.SetVariable("var0", 1)),
				"reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(rewardWindowPage)), null, "started"));
		} else {
			transitions.add(new QuestTransition(deliver, List.of(), List.of(), "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(rewardWindowPage)), null, "started"));
		}
		// 掉落箱对象：开启箱体掉落任务物品，需要 START 态的 ACTION_ITEM_USE 路由
		// （QuestInteractionObjectValidator 合同）；同时登记 TALK 路由使对象成为任务目标。
		// Drop boxes need a START-state ACTION_ITEM_USE route (validator contract) plus the
		// TALK registration route that marks the object as a quest target.
		Set<Integer> dropNpcs = new TreeSet<>();
		metadata.drops().forEach(drop -> dropNpcs.add(drop.npcId()));
		for (int dropNpc : dropNpcs) {
			transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(dropNpc), List.of(), List.of(),
				"started", List.of(), null, "started"));
			transitions.add(new QuestTransition(new QuestEvent.CanAct(dropNpc, "ACTION_ITEM_USE"), List.of(),
				List.of(), "started", List.of(), null, "started"));
		}
		transitions.addAll(previewFlow(rewardNpc));
		transitions.addAll(completeFlow(metadata, rewardNpc));
		transitions.addAll(journalRowRepair(rewardRow.get("var0")));
		if (cakeEvent) {
			// 活动关闭后升级即弃任，避免旧活动任务继续领奖。 /
			// Abandon the expired event quest on level-up before any further report.
			transitions.add(new QuestTransition(new QuestEvent.LevelUp(),
				List.of(new QuestCondition.EventActive(false)), List.of(new QuestAction.AbandonQuest()),
				"unaccepted", List.of(), null, "started"));
		}
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/** 领奖态预览窗：USE_OBJECT(-1) 与 1009 都重开奖励窗。 / Reward-state preview re-opens the reward window. */
	private static List<QuestTransition> previewFlow(int rewardNpc) {
		List<QuestTransition> flow = new ArrayList<>(2);
		for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
			QuestDialogAction.SELECT_QUEST_REWARD)) {
			flow.add(talk(rewardNpc, preview, "reward", "reward", null,
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))));
		}
		return List.copyOf(flow);
	}

	/**
	 * 完成流：与 npc-complete 展开同构——固定奖励 + 每个可选项一条 SELECTED_QUEST_REWARD 路由
	 * + NOREWARD 收尾，确认区间 8..23。
	 * The canonical completion flow over the 8..23 confirm range.
	 */
	private static List<QuestTransition> completeFlow(QuestMetadata metadata, int rewardNpc) {
		if (metadata.rewardGroups().size() > 1) {
			throw new IllegalArgumentException("multi-tier rewards not supported: "
				+ metadata.rewardGroups().size() + " groups");
		}
		List<QuestReward> group = metadata.rewardGroups().isEmpty()
			? List.of() : metadata.rewardGroups().get(0).rewards();
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
		List<QuestDialogAction> confirmActions = new ArrayList<>();
		for (int id = FIRST_CONFIRM_ACTION; id <= LAST_CONFIRM_ACTION; id++) {
			confirmActions.add(QuestDialogAction.fromId(id));
		}
		for (QuestDialogAction action : confirmActions) {
			int selectableIndex = action.id() - FIRST_CONFIRM_ACTION;
			List<QuestAction> actions = new ArrayList<>(fixedRewards);
			if (action != QuestDialogAction.SELECTED_QUEST_NOREWARD && selectableIndex < selectables.size()) {
				actions.add(grant(selectables.get(selectableIndex)));
			}
			actions.add(new QuestAction.CompleteQuest(0));
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, action.id()), List.of(),
				List.copyOf(actions), "complete",
				List.of(new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(10)),
				null, "reward"));
		}
		// 奖励窗口自动确认通道（108 / 110+i，双协议注册 + CloseDialog 收窗）与对话页确认通道并行。
		// The reward-window auto-confirm channel (108 / 110+i, dual-protocol registration with
		// CloseDialog) runs alongside the talk-page confirm channel.
		flow.addAll(RetailSimpleHuntDefinitionCompiler.rewardWindowAutoFlow(rewardNpc, fixedRewards,
			selectables, metadata.classRewards(), "reward", "complete"));
		return List.copyOf(flow);
	}

	/**
	 * 领奖行修复路由（QE-051 的本服兼容面）：多行任务才可能产生陈旧值。
	 * Journal-row repair for saves written by the older projection; only multi-row quests need it.
	 */
	private static List<QuestTransition> journalRowRepair(int lastRow) {
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

	private static QuestAction.GrantReward grant(QuestReward reward) {
		QuestRewardKind kind = QuestRewardKind.fromWire(reward.kind());
		QuestRewardKind actionKind = kind == QuestRewardKind.SELECTABLE_ITEM ? QuestRewardKind.ITEM : kind;
		QuestRewardAmountMode mode = switch (actionKind) {
			case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
			default -> QuestRewardAmountMode.EXACT;
		};
		return new QuestAction.GrantReward(actionKind.name(), reward.id(), reward.amount(), mode);
	}

	private static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			Integer priority, List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, priority, source);
	}
}
