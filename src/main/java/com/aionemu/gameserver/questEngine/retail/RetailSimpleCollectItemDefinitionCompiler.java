package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 原生切换后的共享流程助手（真端派发语义的静态片段）。
 * <p>
 * P4 起 SimpleCollectItem 家族已由 {@link com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler}
 * 原生直驱，本类的**家族编译入口已删除**（`compile`/`precheck`/`build` 与 `RetailSimpleCollectItemTable`
 * 同批退场）：真端表行不再合成 IR 定义。保留下来的只有仍被未切换家族（DataDriven / Handin 流）复用的
 * 规范片段——{@code setproRoute} / {@code journalRowRepair} / {@code canonicalAcceptFlow} /
 * {@code deliveryWindowPage} / {@code canonicalDelivery} / {@code reportNpcExit} / {@code completeFlow} /
 * {@code talk}；待 P7 家族切换后随之一并删除（计划 §9 QE-112）。
 * <p>
 * Shared flow helpers after the native switch. The family compiler entry retired with
 * {@code RetailSimpleCollectItemTable} in P4; only the canonical fragments still reused by the
 * not-yet-switched families (DataDriven / hand-in flows) remain, and they retire with those families.
 */
public final class RetailSimpleCollectItemDefinitionCompiler {

	/** 奖励窗确认按钮区间的首/末档（真端 8..23）。 / First and last reward-window confirm action. */
	private static final int FIRST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	private RetailSimpleCollectItemDefinitionCompiler() {
	}

	/** 真端表自带的备选接取路由 {@code SETPRO1}（133/138 行有）。 / The retail {@code SETPRO1} accept variant. */
	static QuestTransition setproRoute(int acquiredNpc) {
		return setproRoute(acquiredNpc, List.of());
	}

	static QuestTransition setproRoute(int acquiredNpc, List<QuestAction> acceptActions) {
		return new QuestTransition(new QuestEvent.TalkToNpc(acquiredNpc, QuestDialogAction.SETPRO1.id()),
			List.of(new QuestCondition.StartEligible()), acceptActions, "started",
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), null, "unaccepted");
	}

	private static List<QuestReward> selectableRewards(QuestMetadata metadata) {
		List<QuestReward> group = metadata.rewardGroups().isEmpty()
			? List.of() : metadata.rewardGroups().get(0).rewards();
		return group.stream()
			.filter(reward -> QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM)
			.toList();
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
		return canonicalAcceptFlow(acquiredNpc, List.of());
	}

	static List<QuestTransition> canonicalAcceptFlow(int acquiredNpc, List<QuestAction> acceptActions) {
		return RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(acquiredNpc, "started", acceptActions);
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
