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
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DataDriven 无进度行（P5-3 wave A）的合成器：Talk 接取 → 交付 NPC 报告 → 领奖。
 * <p>
 * 形状 = 家族规范形（P0-2 DD 尾片收口，W5-g3 后不再消费任何逐任务信件页）：接取段走规范接取流
 * （NPC 形 = QUEST_SELECT 直发接取窗页 4；物品形 = 使用任务起始道具开窗，1002/1003/1008 无主
 * 提交/拒绝/关窗），交付段的 QUEST_SELECT 直接翻 REWARD 并按档位查表下发奖励窗（客户端成功页
 * 与 1009 中转删除）。完成流沿用采集族口径（领奖窗预览路由 + 确认段 8..23，容忍「零奖励组」）；
 * 领奖行投影 = 客户端任务书末行行号（QE-051），带进入世界修复边；无任务书行由上游门以
 * {@code RETAIL_TALK_JOURNAL_MISSING} 拦下（任务书行是该形状唯一的逐任务客户端证据）。
 * 领奖态在接取 NPC 上**零路由**（1919 合同锁定「各 NPC 路由互不越界」）。
 * <p>
 * Synthesizes DataDriven no-progress rows on the family canonical shape (no per-quest letter pages
 * since W5-g3): canonical accept segments, QUEST_SELECT flipping REWARD with the tiered window; the
 * only remaining per-quest client evidence is the journal row (guarded upstream).
 */
public final class RetailDataDrivenTalkCompiler {

	private RetailDataDrivenTalkCompiler() {
	}

	/**
	 * 组装无进度行定义（NPC 与元数据判定由调用方负责）；单 owner 形见集合重载。
	 * Builds the no-progress definition; the caller owns NPC and metadata pre-checks.
	 */
	public static QuestDefinition build(int questId, int acquiredNpc, int rewardNpc, QuestMetadata metadata,
			int lastRowIndex) {
		return build(questId, acquiredNpc, List.of(rewardNpc), metadata, lastRowIndex);
	}

	/**
	 * 组装无进度行定义（交付集多 owner 形）：真端 reward 名是**声明组**时按组员逐条发报告/完成流，
	 * 与遗留 XML 的多 {@code npc-report}/{@code npc-complete} 块同构（18737 形：三个阶段 NPC 各一份）。
	 * 奖励窗的对话页通道无 NPC 域，由完成流按集合形态整族发一次（{@code completeFlow} 集合重载）。
	 * <p>
	 * Builds the no-progress definition for a reward-owner set: a declared dialog-name group emits one
	 * report/completion flow per member, isomorphic to the legacy multi-block shape. The npc-less reward
	 * window channel is emitted once for the family by the collection overload of {@code completeFlow}.
	 */
	public static QuestDefinition build(int questId, int acquiredNpc, java.util.Collection<Integer> rewardNpcs,
			QuestMetadata metadata, int lastRowIndex) {
		List<QuestTransition> acquire;
		if (acquiredNpc < 0) {
			// 系统发放形（区域/等级/阵营哨兵）：无接取 NPC，发放由发放引擎完成；定义保留一条
			// SystemGrant 边（NONE → started），与 hunt 家族系统发放口径一致。
			// System-grant shape (area/level/faction sentinel): no acquire npc; the grant engine
			// accepts the quest and the definition keeps a single SystemGrant edge, same as hunt.
			acquire = List.of(new QuestTransition(new QuestEvent.SystemGrant(),
				List.of(new QuestCondition.StartEligible()), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		} else {
			// 接取规范形（P0-2 DD 尾片）：QUEST_SELECT 直发接取窗（页 4），select_none 极简信页链与
			// 1007 中转不再由服务端驱动（真端原生相位 A）；started 态 FINISH_DIALOG 出口随流提供。
			// Canonical accept (the DD tail slice): QUEST_SELECT pops the ask window (page 4) directly;
			// the minimal-letter page chain is no longer server-driven.
			acquire = RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(acquiredNpc, "started");
		}
		return assemble(questId, acquire, rewardNpcs, metadata, lastRowIndex);
	}

	/**
	 * 组装**物品接取**的无进度行（13952 形：真端 {@code category_acquire_ = ItemPlay} + 任务起始道具）。
	 * <p>
	 * 与 NPC 接取形的唯一差别 = 接取段：使用道具下发接取窗（页 4），接取/拒绝/关窗是无主对话
	 * （1002/1003/1008，真端接取窗的原生控件），接取目标仍是 {@code started}；报告/完成流与 NPC 形
	 * **逐字节相同**（同一个装配函数，单一真源）。 / Builds an item-acquired no-progress row (the 13952
	 * shape). Only the accept segment differs from the npc shape — the item pops the ask window (page 4)
	 * and its accept/refuse/finish controls are targetless dialogs; report and completion flows are
	 * shared byte-for-byte with the npc shape through one assembly routine.
	 */
	public static QuestDefinition buildItemAcquire(int questId, int itemId, int rewardNpc,
			QuestMetadata metadata, int lastRowIndex) {
		// 接取规范形（P0-2 DD 尾片）：使用道具下发接取窗（页 4），无主 1002/1003/1008 提交/拒绝/关窗。
		// Canonical item accept (the DD tail slice): UseItem pops page 4; 1002/1003/1008 commit/refuse/close.
		return assemble(questId,
			RetailSimpleHuntDefinitionCompiler.canonicalItemAcceptFlow(itemId, "started"),
			List.of(rewardNpc), metadata, lastRowIndex);
	}

	/**
	 * 装配（接取段之外的一切）：节点/布局 + 报告流 + 完成流 + 领奖行修复。接取段由调用方给形状，
	 * 保证 NPC 形与物品形的报告/完成流不会各写一份而漂移。
	 * <p>
	 * Assembly of everything but the accept segment, so the npc shape and the item shape cannot drift.
	 */
	private static QuestDefinition assemble(int questId, List<QuestTransition> acquire,
			java.util.Collection<Integer> rewardNpcs, QuestMetadata metadata, int lastRowIndex) {
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		Map<String, Integer> rewardRow = Map.of("var0", lastRowIndex);
		List<QuestNode> nodes = List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)),
			new QuestNode("started", new NodeProjection(QuestStatus.START, zero)),
			new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)),
			new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));
		List<QuestTransition> transitions = new ArrayList<>(acquire);

		// 交付规范形（P0-2 DD 尾片）：交付 NPC 的 QUEST_SELECT 直翻 REWARD 并按档位查表下发奖励窗
		// （客户端成功页与 1009 中转删除；REWARD 态的重开预览由完成流提供，QE-028 档位查表）。
		// 交付 NPC 专属，接取 NPC 的领奖态保持零路由（1919 合同）；多 owner 交付集逐 owner 各发一份。
		// Canonical delivery (the DD tail slice): the reward npc's QUEST_SELECT flips REWARD and shows
		// the tiered reward window directly (no success page, no 1009 hop; the completion flow keeps the
		// reward-state re-open previews). The acquire npc keeps zero reward-state routes (the 1919
		// contract); a multi-owner reward set emits one such flow per owner.
		int rewardWindowPage = RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, questId);
		for (int rewardNpc : rewardNpcs) {
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.QUEST_SELECT.id()),
				List.of(), List.of(), "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(rewardWindowPage)), null, "started"));
		}
		// 完成流用采集族口径（领奖窗预览路由 + 确认段 8..23，容忍「零奖励组」）。
		// The completion flow follows the collect family (reward-window previews + confirm range).
		transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.completeFlow(questId, rewardNpcs, metadata));
		transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.journalRowRepair(lastRowIndex));
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/**
	 * 组装 Talk 链行定义（P5-3 wave B，1876 链合同同构）：第 i 步 NPC 的 QUEST_SELECT 显示 select{i}
	 * 页，按钮 SETPRO{i} 推进中间步（PACKET_ONLY + 全局任务簿页）、末步 SET_SUCCEED 进领奖态；
	 * var0 = 阶梯计数（0 → 1 → … → 领奖行），领奖投影 = 客户端任务书末行（QE-051）。
	 * 前置步 NPC 的 SET_SUCCEED 直达领奖（1876 捷径同构）；REWARD 态陈旧阶梯值进入世界时纠正。
	 * 系统接取（chain / enterarea / enterworld / leveluplogin）无接取 NPC：发放边与 hunt 链同判据
	 * （TalkCollectChain 同片先例），逐任务客户端 cutscene 声明挂到对应步推进边。
	 * Builds the talk-chain definition; step i pairs with the client's select{i} page. System acquires
	 * (chain / enterarea / enterworld / leveluplogin) carry no acquire npc: grant edges share the hunt
	 * chain's predicate (the TalkCollectChain precedent), and per-step client cutscenes ride the
	 * matching advance edge.
	 */
	public static QuestDefinition buildChain(int questId, int acquiredNpc, int rewardNpc, QuestMetadata metadata,
			int entryPage, List<RetailClientTalkChainPages.Stage> stageLadders, List<Integer> stepNpcs,
			int lastRowIndex, String acquireCategory, String acquireParam, int worldAcquireId,
			Map<Integer, Integer> stepCutscenes) {
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(stageLadders, "stageLadders");
		Objects.requireNonNull(stepNpcs, "stepNpcs");
		String category = acquireCategory == null ? "" : acquireCategory.trim();
		boolean chainOrAreaAcquire = acquiredNpc < 0
			&& ("none".equalsIgnoreCase(category) || "enterarea".equalsIgnoreCase(category));
		boolean levelUpAcquire = acquiredNpc < 0 && "leveluplogin".equalsIgnoreCase(category);
		boolean worldAcquire = acquiredNpc < 0 && "enterworld".equalsIgnoreCase(category);
		int steps = stepNpcs.size();
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		Map<String, Integer> rewardRow = Map.of("var0", lastRowIndex);
		List<QuestNode> nodes = new ArrayList<>(steps + 4);
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)));
		nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, zero)));
		for (int stage = 1; stage < steps; stage++) {
			nodes.add(new QuestNode("s" + stage, new NodeProjection(QuestStatus.START, Map.of("var0", stage))));
		}
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));
		List<QuestTransition> transitions = new ArrayList<>();
		if (acquiredNpc >= 0) {
			// 接取规范形（P0-2 DD 尾片）：QUEST_SELECT 直发接取窗（页 4），select_none 极简信页链不再驱动。
			// Canonical accept (the DD tail slice): QUEST_SELECT pops the ask window (page 4) directly.
			transitions.addAll(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(acquiredNpc, "started"));
		} else if (worldAcquire) {
			// EnterWorld 接取（10010 形）：进世界事件 + WorldIs 直接进 START（与 hunt 链同判据）。
			// EnterWorld acquire (the 10010 shape): the world-enter event plus WorldIs lands START
			// (same predicate as the hunt chain).
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.WorldIs(worldAcquireId, true)), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
				null, "unaccepted"));
		} else {
			// 链式 / 区域 / 升级登录接取：自动发放。chain + enterarea = LevelUp + ZoneMissionEnd；
			// leveluplogin = LevelUp + EnterWorld（LogIn 半边）；StartEligible 承载等级/前置门。
			// Chain / area / level-up-login acquires auto-grant. Chain + area = LevelUp +
			// ZoneMissionEnd; leveluplogin = LevelUp + EnterWorld (the LogIn half); StartEligible
			// carries the level and prerequisite gates.
			List<QuestCondition> grantGate = new ArrayList<>(2);
			grantGate.add(new QuestCondition.StartEligible());
			if (!metadata.prerequisites().isEmpty()) {
				grantGate.add(new QuestCondition.QuestsFinished(metadata.prerequisites()));
			}
			QuestEvent[] grantEvents = levelUpAcquire
				? new QuestEvent[] {new QuestEvent.LevelUp(), new QuestEvent.EnterWorld()}
				: new QuestEvent[] {new QuestEvent.LevelUp(), new QuestEvent.ZoneMissionEnd()};
			for (QuestEvent grantEvent : grantEvents) {
				transitions.add(new QuestTransition(grantEvent, List.copyOf(grantGate), List.of(), "started",
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
					null, "unaccepted"));
			}
		}
		for (int index = 0; index < steps; index++) {
			int npc = stepNpcs.get(index);
			String source = index == 0 ? "started" : "s" + index;
			boolean finalStep = index == steps - 1;
			String target = finalStep ? "reward" : "s" + (index + 1);
			Integer movieId = stepCutscenes == null ? null : stepCutscenes.get(index);
			// 阶段页面梯：QUEST_SELECT 显示梯首页，导航按钮沿梯下行（动作 id = 下一页 id）。
			// The stage page ladder: QUEST_SELECT shows the head page; nav buttons walk down the
			// ladder (a nav action's dialog id is the next page's id).
			RetailClientTalkChainPages.Stage stage = stageLadders.get(index);
			List<Integer> ladder = stage.ladder();
			transitions.add(talk(npc, QuestDialogAction.QUEST_SELECT, source, source,
				List.of(new AfterCommitAction.ShowQuestDialog(ladder.get(0)))));
			for (int depth = 1; depth < ladder.size(); depth++) {
				transitions.add(new QuestTransition(
					new QuestEvent.TalkToNpc(npc, QuestDialogAction.fromId(ladder.get(depth)).id()),
					List.of(), List.of(), source,
					List.of(new AfterCommitAction.ShowQuestDialog(ladder.get(depth))), null, source));
			}
			if (finalStep) {
				// 末步推进动作（SET_SUCCEED 或 1009 变体）进领奖态。
				// The final advance action (SET_SUCCEED or the 1009 variant) moves to reward.
				List<AfterCommitAction> afterCommit = new ArrayList<>(3);
				afterCommit.add(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH));
				if (movieId != null) {
					afterCommit.add(new AfterCommitAction.PlayMovie(movieId, QuestMovieType.CUTSCENE));
				}
				afterCommit.add(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
				transitions.add(new QuestTransition(
					new QuestEvent.TalkToNpc(npc, stage.advanceActionId()),
					List.of(), List.of(), "reward",
					List.copyOf(afterCommit),
					null, source));
				if (steps == 1 && stage.advanceActionId() != QuestDialogAction.SET_SUCCEED.id()) {
					// 35055 单步捷径：步骤 NPC 的 SET_SUCCEED 落 s1（保留遗留别名边），与 SETPRO1
					// 主边（进领奖）并存；推进动作本身已是 SET_SUCCEED 时不发重复边，也不声明不可达别名节点。
					// The one-step shortcut: the step npc's SET_SUCCEED lands on s1 (the legacy
					// alias edge), coexisting with the SETPRO1 primary edge into reward; when the
					// advance action is already SET_SUCCEED, neither a duplicate edge nor an
					// unreachable alias node is emitted.
					nodes.add(new QuestNode("s1", new NodeProjection(QuestStatus.START, Map.of("var0", 1))));
					transitions.add(new QuestTransition(
						new QuestEvent.TalkToNpc(npc, QuestDialogAction.SET_SUCCEED.id()),
						List.of(), List.of(new QuestAction.SetVariable("var0", 1)), "s1",
						List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
						null, source));
				}
			} else {
				// 中间步推进动作（登记表从按钮图导出）推进阶梯（只下发状态包 + 全局任务簿页）；
				// 同时保留 SET_SUCCEED 直达领奖的捷径（1876 修复期形状：前置步 NPC 也能收尾）。
				// An intermediate advance (from the button graph) moves the ladder; the SET_SUCCEED
				// shortcut to the reward state stays available from earlier stage npcs (1876 shape).
				List<AfterCommitAction> afterCommit = new ArrayList<>(3);
				afterCommit.add(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
				if (movieId != null) {
					afterCommit.add(new AfterCommitAction.PlayMovie(movieId, QuestMovieType.CUTSCENE));
				}
				afterCommit.add(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
				transitions.add(new QuestTransition(
					new QuestEvent.TalkToNpc(npc, stage.advanceActionId()),
					List.of(), List.of(new QuestAction.SetVariable("var0", index + 1)), target,
					List.copyOf(afterCommit),
					null, source));
				transitions.add(new QuestTransition(
					new QuestEvent.TalkToNpc(npc, QuestDialogAction.SET_SUCCEED.id()),
					List.of(), List.of(), "reward",
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
						new AfterCommitAction.CloseDialog()),
					null, source));
			}
		}
		// 交付/预览规范形（P0-2 DD 尾片）：REWARD 态 QUEST_SELECT 重开分档奖励窗（QE-028 查表）。
		// Canonical preview (the DD tail slice): the reward-state QUEST_SELECT re-opens the tiered
		// reward window; the client success page is no longer server-driven.
		int rewardWindowPage = RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, questId);
		transitions.add(talk(rewardNpc, QuestDialogAction.QUEST_SELECT, "reward", "reward",
			List.of(new AfterCommitAction.ShowQuestDialog(rewardWindowPage))));
		transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.completeFlow(questId, rewardNpc, metadata));
		// 恰好一条进入世界自愈边：REWARD 态存档的旧阶梯值（末行-1）纠正到领奖行
		// （1876/11323 合同锁定单条边；1876 的 1→2 与 11323 的 4→5 同构，QE-054）。
		// Exactly one enter-world heal edge: the persisted pre-completion ladder value repairs to
		// the reward row (the 1876/11323 contracts pin a single edge, QE-054).
		if (lastRowIndex > 0) {
			int staleRow = lastRowIndex - 1;
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", staleRow)),
				List.of(new QuestAction.SetVariable("var0", lastRowIndex)), "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				null, null));
		}
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	private static QuestTransition talk(int npcId, QuestDialogAction action, String source, String target,
			List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, action.id()), List.of(), List.of(), target,
			afterCommit, null, source);
	}
}
