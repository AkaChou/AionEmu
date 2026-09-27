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

/**
 * 真端 SimpleItemPlay 表行 → 完整任务定义的合成器（P1 第一波：简单"接取发物 → 用物品演出 → 交付"形）。
 * <p>
 * 规范形状（13704 展开口径；P0-2 接取/交付规范形）：QUEST_SELECT 直发接取窗（页 4，select1
 * 页与 1007 中转随页链删除），接取提交发放任务道具 + 用物品推进（{@code UseItem} → var0 置 1
 * 进 REWARD）+ REWARD 态 QUEST_SELECT/USE_OBJECT 重开分档奖励窗（QE-028，SELECT5 报告页删除）、
 * 1009 回收道具 + npc-complete（确认段 8..23）。道具 id = 表符号去 {@code ITEM_} 前缀转小写后的
 * 物品 {@code name_desc}（与 SimpleUseItem 同规则）。
 * <p>
 * 稳定拒绝码（对话链/换道具/前置/过场等待后续波次，真端表有列但本波合成器未展开）：
 * {@code RETAIL_TALK_CHAIN}（talk_npc1/2 对话链）、{@code RETAIL_ITEM_SWAP}（give_item2/remove_item2 换道具）、
 * {@code RETAIL_CON_QUEST}（con_quest 前置）、{@code RETAIL_CUTSCENE}（cutsceneid1 过场）、
 * {@code RETAIL_ADVANCE_UNEXPRESSED}（行无 use_item_name，推进事件不在族表）、
 * {@code RETAIL_ACQUIRE_NPC_*}/{@code RETAIL_REWARD_NPC_*}/{@code RETAIL_USE_ITEM_UNRESOLVED}（名字/符号解析失败）。
 * <p>
 * Synthesizes the canonical SimpleItemPlay shape (accept grants the item, using it advances to the
 * reward state, hand-in on the reward NPC). Chain/swap/prerequisite/cutscene columns carry stable
 * rejection codes until later waves.
 */
public final class RetailSimpleItemPlayDefinitionCompiler {

	private static final int FIRST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	private RetailSimpleItemPlayDefinitionCompiler() {
	}

	/** 编译结果（拒绝时 {@code rejectionCode} 稳定可登记）。 / Compilation outcome with a stable rejection code. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 编译一行。 / Compiles one row.
	 */
	public static Outcome compile(RetailSimpleItemPlayTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex, RetailQuestMetadataCompiler.Outcome metadata) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(itemIndex, "itemIndex");
		Objects.requireNonNull(npcIndex, "npcIndex");
		Objects.requireNonNull(metadata, "metadata");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		Outcome blocked = precheck(entry, itemIndex, npcIndex);
		if (blocked != null) {
			return blocked;
		}
		try {
			QuestDefinition definition = build(entry, itemIndex, npcIndex, metadata.metadata());
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 迁移判据（稳定拒绝码，按序首中即拒）。 / Migration pre-check; the first hit wins.
	 */
	private static Outcome precheck(RetailSimpleItemPlayTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex) {
		int questId = entry.questId();
		if (entry.acquiredNpcName() == null || resolveNpc(entry.acquiredNpcName(), npcIndex).size() != 1) {
			return npcReject("RETAIL_ACQUIRE_NPC", questId, entry.acquiredNpcName(), npcIndex);
		}
		if (entry.rewardNpcName() == null || resolveNpc(entry.rewardNpcName(), npcIndex).size() != 1) {
			return npcReject("RETAIL_REWARD_NPC", questId, entry.rewardNpcName(), npcIndex);
		}
		if (entry.useItemName() == null) {
			return new Outcome(null, "RETAIL_ADVANCE_UNEXPRESSED",
				"row has no use_item_name; the advance event is not in the family table");
		}
		if (resolveItemId(entry.useItemName(), itemIndex) == null) {
			return new Outcome(null, "RETAIL_USE_ITEM_UNRESOLVED", entry.useItemName());
		}
		if (entry.talkNpc1() != null || entry.talkNpc2() != null) {
			return new Outcome(null, "RETAIL_TALK_CHAIN",
				"talk_npc1=" + entry.talkNpc1() + " talk_npc2=" + entry.talkNpc2());
		}
		if (entry.conQuest() != null) {
			return new Outcome(null, "RETAIL_CON_QUEST", "con_quest=" + entry.conQuest());
		}
		if (entry.cutsceneId() != null) {
			return new Outcome(null, "RETAIL_CUTSCENE", "cutsceneid1=" + entry.cutsceneId());
		}
		if (entry.giveItem2() != null || entry.removeItem2() != null || entry.removeItem1() != null
				|| entry.giveItem1() != null) {
			return new Outcome(null, "RETAIL_ITEM_SWAP",
				"give_item1/2=" + entry.giveItem1() + "/" + entry.giveItem2()
					+ " remove_item1/2=" + entry.removeItem1() + "/" + entry.removeItem2());
		}
		return null;
	}

	private static Outcome npcReject(String prefix, int questId, String raw, RetailNpcNameIndex npcIndex) {
		Set<Integer> ids = raw == null ? Set.of() : resolveNpc(raw, npcIndex);
		if (ids.size() > 1) {
			return new Outcome(null, prefix + "_AMBIGUOUS", questId + ": " + raw + " -> " + ids);
		}
		String kind = raw != null && raw.length() > 2 && raw.startsWith("_") && raw.endsWith("_")
			? "_SENTINEL" : "_UNRESOLVED";
		return new Outcome(null, prefix + kind, questId + ": " + raw);
	}

	private static Set<Integer> resolveNpc(String name, RetailNpcNameIndex npcIndex) {
		return npcIndex.resolveAll(List.of(name == null ? "" : name)).npcIds();
	}

	private static Integer resolveItemId(String symbol, RetailItemNameIndex itemIndex) {
		if (symbol == null || symbol.isBlank()) {
			return null;
		}
		// 表值 = 符号 + 可选数量（如 "ITEM_QUEST_13704A 1"），符号取首段。
		// Table values are "SYMBOL [count]"; the symbol is the first token.
		String stem = symbol.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
		Integer direct = itemIndex.resolve(stem);
		if (direct != null) {
			return direct;
		}
		if (stem.startsWith("item_")) {
			return itemIndex.resolve(stem.substring("item_".length()));
		}
		return null;
	}

	/** 表值尾段数量（缺省 1）。 / Trailing count of a table value (default 1). */
	private static int itemCount(String symbol) {
		if (symbol == null) {
			return 1;
		}
		String[] tokens = symbol.trim().split("\\s+");
		return tokens.length > 1 ? Integer.parseInt(tokens[1]) : 1;
	}

	private static QuestDefinition build(RetailSimpleItemPlayTable.Entry entry, RetailItemNameIndex itemIndex,
			RetailNpcNameIndex npcIndex, QuestMetadata metadata) {
		int questId = entry.questId();
		int acquiredNpc = resolveNpc(entry.acquiredNpcName(), npcIndex).iterator().next();
		int rewardNpc = resolveNpc(entry.rewardNpcName(), npcIndex).iterator().next();
		int itemId = resolveItemId(entry.useItemName(), itemIndex);
		Integer giveItemId = entry.giveItem() != null ? resolveItemId(entry.giveItem(), itemIndex) : null;
		int giveCount = itemCount(entry.giveItem());
		int removeCount = itemCount(entry.useItemName());

		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, 1, 0, 1, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		Map<String, Integer> one = Map.of("var0", 1);
		List<QuestNode> nodes = List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)),
			new QuestNode("started", new NodeProjection(QuestStatus.START, zero)),
			new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, one)),
			new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));

		List<QuestTransition> transitions = new ArrayList<>();
		// 接取规范形（P0-2）：QUEST_SELECT 直发接取窗（页 4），select1 页与 1007 中转随页链删除；
		// 接取提交（1002/20000）发放演出道具。 / Canonical accept (P0-2): QUEST_SELECT pops the ask
		// window (page 4) directly; the select1 page and 1007 hop are gone; the accept commit grants
		// the play item.
		List<QuestAction> acceptActions = giveItemId == null ? List.of()
			: List.of(new QuestAction.GiveItem(giveItemId, giveCount));
		transitions.addAll(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(
			acquiredNpc, "started", acceptActions));
		// 用物品演出：started→reward（var0 置 1）。 / Using the item advances started→reward (var0 = 1).
		transitions.add(new QuestTransition(new QuestEvent.UseItem(itemId, 0),
			List.of(new QuestCondition.QuestVariableIs("var0", 0)),
			List.of(new QuestAction.SetVariable("var0", 1)), "reward",
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			null, "started"));
		// 交付/预览规范形（P0-2）：REWARD 态 QUEST_SELECT/USE_OBJECT 重开分档奖励窗（QE-028 查表，
		// SELECT5 报告页删除）；1009 保留为预览通道并回收演出道具（回收时机不变）。
		// Canonical delivery/preview (P0-2): the reward-state QUEST_SELECT/USE_OBJECT re-opens the
		// tiered reward window (QE-028; the SELECT5 page is gone); 1009 stays as the preview channel
		// and removes the play item (the removal timing is unchanged).
		int rewardWindowPage = QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1)
			.orElseThrow(() -> new IllegalArgumentException(
				"reward tiers exceed the six client reward windows: " + questId)).id();
		transitions.add(talk(rewardNpc, QuestDialogAction.QUEST_SELECT, "reward", "reward", null,
			List.of(new AfterCommitAction.ShowQuestDialog(rewardWindowPage))));
		transitions.add(talk(rewardNpc, QuestDialogAction.USE_OBJECT, "reward", "reward", null,
			List.of(new AfterCommitAction.ShowQuestDialog(rewardWindowPage))));
		transitions.add(new QuestTransition(
			new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.SELECT_QUEST_REWARD.id()), List.of(),
			List.of(new QuestAction.RemoveItem(itemId, removeCount)), "reward",
			List.of(new AfterCommitAction.ShowQuestDialog(rewardWindowPage)), null, "reward"));
		transitions.addAll(completeFlow(metadata, rewardNpc));
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/**
	 * 完成流：与 npc-complete 展开同构——固定奖励，确认段 8..23。
	 * Completion flow isomorphic to the npc-complete expansion over the 8..23 confirm range.
	 */
	private static List<QuestTransition> completeFlow(QuestMetadata metadata, int rewardNpc) {
		List<QuestReward> group = metadata.rewardGroups().isEmpty()
			? List.of() : metadata.rewardGroups().get(0).rewards();
		List<QuestAction> fixedRewards = new ArrayList<>();
		for (QuestReward reward : group) {
			if (QuestRewardKind.fromWire(reward.kind()) != QuestRewardKind.SELECTABLE_ITEM) {
				fixedRewards.add(grant(reward));
			}
		}
		List<QuestTransition> flow = new ArrayList<>();
		for (int id = FIRST_CONFIRM_ACTION; id <= LAST_CONFIRM_ACTION; id++) {
			List<QuestAction> actions = new ArrayList<>(fixedRewards);
			actions.add(new QuestAction.CompleteQuest(0));
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, id), List.of(),
				List.copyOf(actions), "complete",
				List.of(new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(10)),
				null, "reward"));
		}
		// 奖励窗口自动确认通道：与对话页确认段同形（固定奖励挂 108，双协议注册 + CloseDialog 收窗）。
		// The reward-window auto-confirm channel mirrors the talk-page confirm range (fixed rewards
		// bind 108, dual-protocol registration with CloseDialog).
		flow.addAll(RetailSimpleHuntDefinitionCompiler.rewardWindowAutoFlow(rewardNpc, fixedRewards,
			List.of(), Map.of(), "reward", "complete"));
		return flow;
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
