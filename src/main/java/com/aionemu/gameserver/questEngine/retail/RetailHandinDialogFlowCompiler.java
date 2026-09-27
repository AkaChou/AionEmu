package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.ProgressScope;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 交付型对话流的合成器（客户端词汇表 = {@code select_none / select1 / check_user_item_ok /
 * check_user_item_fail / select_success}，见 {@link RetailClientHandinPages}）。
 * <p>
 * 页面顺序（客户端任务书 HTML 的权威顺序）：
 * <ol>
 * <li>{@code select_none}：接取窗，按钮 20000 接受 / 20001 拒绝；</li>
 * <li>{@code select1}：任务书正文，按钮 39 或 20002 交付检查（两个动作都发，覆盖两种客户端变体）；</li>
 * <li>{@code check_user_item_ok}：交付成功页（按钮 1009 或 1008 → 领奖窗 / 关窗）；</li>
 * <li>{@code check_user_item_fail}：交付失败页（按钮 1008 → 关窗）；</li>
 * <li>{@code select_success}：成功收尾页（按钮 1009 → 领奖窗）。</li>
 * </ol>
 * ok 页本地关闭的续接规则（与交接审计合同逐字镜像）：{@code check_user_item_ok} 页可见按钮只有
 * FINISH_DIALOG(1008)（客户端本地关闭，不回传任务动作）且同一 NPC 在 reward 节点有 USE_OBJECT
 * 续接入口时，交付成功分支直接开领奖窗——显示 ok 页会形成死端；否则照常显示 ok 页。
 * 采集物来自真端 quest.xml 元数据（接取时自动发放）；领奖行投影沿用 QE-051（客户端任务书末行行号）。
 * <p>
 * Synthesizer for the client hand-in dialog vocabulary; every button the client exposes gets a route
 * and every page the client declares is emitted, which is what the client contract gate asserts.
 */
public final class RetailHandinDialogFlowCompiler {

	private RetailHandinDialogFlowCompiler() {
	}

	/**
	 * 组装交付型定义（不含元数据校验；调用方负责 NPC 与交付物判定）。
	 * Builds the hand-in definition; the caller owns NPC and work-item pre-checks.
	 */
	public static QuestDefinition build(int questId, java.util.Set<Integer> acquiredNpcs,
			java.util.Set<Integer> rewardNpcs, QuestMetadata metadata, RetailClientHandinPages.Pages pages,
			RetailClientSummaryRows summaryRows, int lastRowIndex, RetailQuestUseItemNpcs interactionObjects,
			boolean selectNoneLadder) {
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		Map<String, Integer> rewardRow = Map.of("var0", lastRowIndex);
		// 完成流先建：成功分支是否直接开领奖窗，取决于同一 NPC 在 reward 节点有没有 USE_OBJECT 续接入口
		//（与交接审计合同逐字镜像：ok 页本地关闭 + 有续接入口 → 直接开续接页，否则保留 ok 页）。
		// The completion flow is built first: the hand-over success page mirrors the handover-audit
		// contract exactly — local-close ok page plus a same-NPC continuation entry opens the reward
		// window directly; otherwise the ok page stays.
		// 交付 NPC 变体家族（50052 形）：家族形完整交付流（对话页确认梯逐实例、奖励窗口整族一份）。
		// Reward npc variant family (the 50052 shape): the family completion flow (talk-page ladder
		// per instance, one reward window for the family).
		List<QuestTransition> completion = new ArrayList<>(
			RetailSimpleCollectItemDefinitionCompiler.completeFlow(questId, rewardNpcs, metadata));
		// 续接入口判定按变体家族口径：任一接取 NPC 在交付流里拥有自己的 USE_OBJECT 入口即成立
		//（接取 NPC 同时是交付实例时必然如此）。
		// Continuation check in family caliber: any acquire npc owning its own USE_OBJECT entry in
		// the completion flow qualifies (always true when the acquire npc is itself a hand-in instance).
		boolean localCloseWithContinuation = pages.okLocalClose()
			&& acquiredNpcs.stream().anyMatch(acquire -> hasSameNpcContinuation(completion, acquire));
		List<QuestNode> nodes = List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)),
			new QuestNode("started", new NodeProjection(QuestStatus.START, zero)),
			new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)),
			new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));
		List<QuestTransition> transitions = new ArrayList<>();
		// 接取 NPC 变体家族（18742 形）：每个变体各建一套接取流（接取入口页/任务书正文页/交付检查）。
		// Acquire variant family (the 18742 shape): each variant gets its own accept flow (entry
		// page / letter page / hand-in checks).
		for (int acquireVariant : acquiredNpcs) {
			// 接取流用族规范形（QUEST_SELECT 入口页来自客户端首屏，其余按钮沿用规范展开）——客户端逐个按钮都有
			// 路由，且与既有 XML/测试锁定的形状一致；客户端首屏带 select_none 续页时一并发阶梯出口。
			// The accept flow is the canonical expansion carrying the client's own entry page; the
			// select_none continuation rung rides along when the client declares that page.
			transitions.addAll(RetailSimpleHuntDefinitionCompiler.acceptFlow(acquireVariant, "started",
				pages.entryPage(), selectNoneLadder));
			transitions.add(letterRoute(acquireVariant, pages.letterPage()));
			transitions.addAll(handinChecks(acquireVariant, metadata.itemRequirements(), pages,
				localCloseWithContinuation));
		}
		// started 态的 FINISH_DIALOG 出口由接取流规范展开提供（同一路由重复登记会判 AMBIGUOUS_TRANSITION）。
		// The started-state FINISH_DIALOG exit comes from the canonical accept flow.
		// 领奖态的报告页：交付 NPC 的 QUEST_SELECT 开客户端成功页（select_success），与旧 XML 的
		// npc-report（page=DEFAULT_SUCCESS）同构；然后才是 npc-complete 的领奖窗。
		// The reward-state report page: the hand-in NPC's QUEST_SELECT opens the client success page.
		for (int rewardVariant : rewardNpcs) {
			transitions.add(talk(rewardVariant, QuestDialogAction.QUEST_SELECT, "reward", "reward",
				List.of(new AfterCommitAction.ShowQuestDialog(pages.successPage()))));
		}
		// 完成流用采集族口径（容忍「零奖励组」：真端 %Quest_* 引用未登记时该行本就没有奖励，旧 XML 亦无）。
		// The completion flow follows the collect family, which tolerates zero reward groups.
		transitions.addAll(completion);
		// 成功页/失败页上的 1008 关窗出口（客户端按钮变体之一）：接取∪交付集合每个不同 NPC 各登记一次。
		// The close exits for the reward-state pages, once per distinct npc in acquires ∪ rewards.
		java.util.TreeSet<Integer> closeNpcs = new java.util.TreeSet<>();
		closeNpcs.addAll(acquiredNpcs);
		closeNpcs.addAll(rewardNpcs);
		for (int closeNpc : closeNpcs) {
			transitions.add(talk(closeNpc, QuestDialogAction.FINISH_DIALOG, "reward", "reward",
				List.of(new AfterCommitAction.CloseDialog())));
		}
		// 交付成功页可能带领奖窗按钮：非交付实例的接取 NPC 也要有领奖窗出口（交付实例自身由完成流提供）。
		// The success page may carry a reward-window button: acquire npcs that are not hand-in
		// instances need the reward-window exit too (hand-in instances get it from their flow).
		for (int acquireVariant : acquiredNpcs) {
			if (!rewardNpcs.contains(acquireVariant)) {
				transitions.add(talk(acquireVariant, QuestDialogAction.SELECT_QUEST_REWARD, "reward", "reward",
					List.of(new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))));
			}
		}
		transitions.addAll(dropBoxes(metadata, interactionObjects));
		transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.journalRowRepair(lastRowIndex));
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/**
	 * 交互物掉落箱：START 态的 {@code ACTION_ITEM_USE} 路由（启动期合同）。
	 * 只给真端 {@code ai=quest_use_item} 的掉落 NPC 发，普通怪物掉落不发；不登记对话路由——
	 * 采集/掉落 NPC 不是对话对象（启动校验器对自回路无副作用的路线只要求掉落元数据在场）。
	 * Drop boxes: a START-state ACTION_ITEM_USE route, only for npcs whose retail AI is quest_use_item.
	 * No dialog routes: drop sources are not dialogue owners, and the startup validator accepts the
	 * side-effect-free self-loop with catalog drop metadata alone.
	 */
	private static List<QuestTransition> dropBoxes(QuestMetadata metadata,
			RetailQuestUseItemNpcs interactionObjects) {
		java.util.TreeSet<Integer> boxNpcs = new java.util.TreeSet<>();
		for (var drop : metadata.drops()) {
			if (interactionObjects.isInteractionObject(drop.npcId())) {
				boxNpcs.add(drop.npcId());
			}
		}
		List<QuestTransition> flow = new ArrayList<>(boxNpcs.size());
		for (int boxNpc : boxNpcs) {
			flow.add(new QuestTransition(new QuestEvent.CanAct(boxNpc, "ACTION_ITEM_USE"), List.of(),
				List.of(), "started", List.of(), null, "started"));
		}
		return List.copyOf(flow);
	}

	/**
	 * 交付成功分支的续接入口判定（与交接审计 {@code sameNpcEntryPages} 同口径）：同一 NPC 在
	 * reward 节点的 USE_OBJECT 打开对话入口（领奖窗）是否存在。
	 * Same-NPC continuation check mirroring the handover-audit contract: a USE_OBJECT dialogue
	 * entry (the reward window) owned by the same NPC in the reward node.
	 */
	private static boolean hasSameNpcContinuation(List<QuestTransition> completion, int npcId) {
		return completion.stream().anyMatch(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.npcId() == npcId && talk.dialogId() != null
			&& talk.dialogId() == QuestDialogAction.USE_OBJECT.id()
			&& candidate.afterCommit().stream().anyMatch(AfterCommitAction.ShowQuestDialog.class::isInstance));
	}


	/** 任务书正文页：交付检查按钮所在页。 / The letter page hosting the hand-in check button. */
	private static QuestTransition letterRoute(int npcId, int letterPage) {
		return talk(npcId, QuestDialogAction.QUEST_SELECT, "started", "started",
			List.of(new AfterCommitAction.ShowQuestDialog(letterPage)));
	}

	/**
	 * 交付检查：39（CHECK_USER_HAS_QUEST_ITEM）与 20002（SIMPLE 变体）各一对——客户端两种变体都存在，
	 * 成功页 = {@code check_user_item_ok}；ok 页本地关闭且有续接入口时直接开领奖窗（见类注释）。
	 * The hand-in checks: both client variants (39 and 20002), success page = check_user_item_ok,
	 * or the reward window directly when the ok page is a local close with a continuation entry.
	 */
	private static List<QuestTransition> handinChecks(int npcId, List<QuestItemRequirement> items,
			RetailClientHandinPages.Pages pages, boolean localCloseWithContinuation) {
		List<QuestCondition> hasItems = items.stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> removeItems = items.stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		int successPage = localCloseWithContinuation
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: pages.okPage();
		List<AfterCommitAction> success = List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH));
		List<QuestTransition> flow = new ArrayList<>(6);
		for (QuestDialogAction check : List.of(QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE)) {
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(npcId, check.id()), hasItems, removeItems,
				"reward", withPage(success, successPage), 0, "started"));
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(npcId, check.id()), List.of(), List.of(),
				"started", List.of(new AfterCommitAction.ShowQuestDialog(pages.failPage())), 1, "started"));
		}
		return List.copyOf(flow);
	}

	private static List<AfterCommitAction> withPage(List<AfterCommitAction> base, int pageId) {
		List<AfterCommitAction> actions = new ArrayList<>(base);
		actions.add(new AfterCommitAction.ShowQuestDialog(pageId));
		return List.copyOf(actions);
	}

	private static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, null, source);
	}
}
