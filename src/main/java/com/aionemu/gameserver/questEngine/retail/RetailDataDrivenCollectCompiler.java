package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.NodeProjection;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.ProgressScope;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 真端 DataDriven 表行 → 完整任务定义（P5-2：Talk 接取 + CollectItem 进度）。
 * <p>
 * CollectItem 行的 {@code value0_progress_} 只有报告 NPC 名（没有物品/对象参数），采集物由真端
 * quest.xml 元数据的交付物（{@code <items>}，接取时自动发放）承载。因此合成形状 = **采集族规范形**：
 * 四节点 + {@code npc-start} 接取流 + 交付检查对（39/20002，整组 has-item/remove-item）+ 完成流
 * （{@code npc-complete} 展开，含可选项 choice 绑定）+ 关窗出口 + 领奖行修复。
 * <p>
 * 领奖投影沿用 QE-051 口径（客户端任务书末行行号 = {@code rows - 1}），与 SimpleCollectItem /
 * SimpleTalk 同一来源；旧 XML 的单行投影是历史错误面，由冻结 IR 指纹与本批裁定表登记。
 * <p>
 * Synthesizes DataDriven rows with Talk acquire + CollectItem progress as the SimpleCollectItem
 * canonical shape (work items from the retail metadata, hand-in checks, npc-complete expansion).
 */
public final class RetailDataDrivenCollectCompiler {

	private RetailDataDrivenCollectCompiler() {
	}

	/** 编译结果（复用采集族的语义）。 / Compilation outcome (collect-family semantics reused). */
	public record Outcome(QuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/** 供 DataDriven 编译器委托调用的构建入口。 / Build entry used by the DataDriven compiler. */
	public static QuestDefinition buildSimple(int questId, java.util.Set<Integer> acquiredNpcs,
			java.util.Set<Integer> rewardNpcs, QuestMetadata metadata, RetailClientSummaryRows summaryRows,
			RetailClientDialogExits exits, RetailClientHandinPages handinPages,
			RetailQuestUseItemNpcs interactionObjects) {
		// 交付型客户端词汇表（select_none/select1/check_ok/check_fail/select_success）：页面只能来自
		// 客户端任务书，按钮必须逐个有路由，因此走专用合成器（P5-2 真实缺口：249/256 行是这种流程）。
		// The client hand-in vocabulary needs its own shape: every button the client exposes must be
		// routed and the pages come from the client letter, not from the family defaults.
		var pages = handinPages.find(questId);
		if (pages.isPresent()) {
			// 专用词汇合成器已变体化（18742/50052 形：接取与交付 NPC 均可为同名多刷点家族）。
			// The dedicated-vocabulary compiler is variant-capable (the 18742/50052 shapes: acquires
			// and rewards may each be a same-name multi-spawn family).
			return RetailHandinDialogFlowCompiler.build(questId, acquiredNpcs, rewardNpcs, metadata,
				pages.get(), summaryRows, summaryRows.lastRowIndex(questId), interactionObjects,
				exits.requires(questId, RetailClientDialogExits.SELECT_NONE_1));
		}
		return buildCanonical(questId, acquiredNpcs, rewardNpcs, metadata, summaryRows, exits);
	}

	/** 家族规范形（select1/ask_quest_accept/select2 词汇表）。 / The family canonical shape. */
	private static QuestDefinition buildCanonical(int questId, java.util.Set<Integer> acquiredNpcs,
			java.util.Set<Integer> rewardNpcs, QuestMetadata metadata, RetailClientSummaryRows summaryRows,
			RetailClientDialogExits exits) {
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		// 领奖投影 = 客户端任务书末行行号（QE-051）；真端模板表没有行数，行数来自客户端 quest_summary 登记。
		// Reward projection = the last client journal row index (QE-051).
		Map<String, Integer> rewardRow = Map.of("var0", summaryRows.lastRowIndex(questId));
		List<QuestNode> nodes = List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)),
			new QuestNode("started", new NodeProjection(QuestStatus.START, zero)),
			new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)),
			new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));
		List<QuestTransition> transitions = new ArrayList<>();
		// 接取 NPC 变体家族（18742 形）：每个变体各建一套接取流（规范形 + SETPRO）。
		// Acquire variant family (the 18742 shape): each variant gets its own accept flow
		// (canonical + SETPRO).
		for (int acquireVariant : acquiredNpcs) {
			// 接取规范形（P0-2 DD 尾片）：页 4 直发；SELECT1 入口页、1007 中转与 SELECT1_1 续页梯随页链删除。
			// Canonical accept (the DD tail slice): page 4 directly; the SELECT1 entry page, the 1007
			// hop and the SELECT1_1 continuation ladder are gone.
			transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.canonicalAcceptFlow(acquireVariant));
			// 真端表自带的备选接取路由（客户端 select1 页的 SETPRO 按钮），与采集族同口径。
			// The retail SETPRO1 alternative accept route, same caliber as the collect family.
			transitions.add(RetailSimpleCollectItemDefinitionCompiler.setproRoute(acquireVariant));
		}
		// 交付规范形（P0-2 DD 尾片）：QUEST_SELECT 带整组 HasItem 门控直翻 REWARD 并下发档位奖励窗
		// （QE-028 查表）；报告页（SELECT5）与 39/20002 检查对随页链删除。奖励 NPC 变体家族（50052 形）
		// 的每个实例都是合法交付 NPC（任一实例都可完成，检查对话的按 NPC 分域随之消失）。
		// Canonical delivery (the DD tail slice): QUEST_SELECT gated by the whole hand-in set flips
		// REWARD and shows the tiered window (QE-028 lookup); the report page and the 39/20002 check
		// pairs are gone. Every instance of a reward npc variant family is a legal turn-in npc.
		int rewardWindowPage = RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, questId);
		List<QuestCondition> hasItems = metadata.itemRequirements().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
			.toList();
		List<QuestAction> removeItems = metadata.itemRequirements().stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();
		for (int rewardVariant : rewardNpcs) {
			transitions.add(RetailSimpleCollectItemDefinitionCompiler.canonicalDelivery(rewardVariant,
				hasItems, removeItems, rewardWindowPage, "started"));
		}
		for (int rewardVariant : rewardNpcs) {
			transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.completeFlow(questId, rewardVariant,
				metadata));
		}
		for (int rewardVariant : rewardNpcs) {
			transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.reportNpcExit(
				acquiredNpcs.contains(rewardVariant), rewardVariant, "started"));
		}
		transitions.addAll(RetailSimpleCollectItemDefinitionCompiler.journalRowRepair(rewardRow.get("var0")));
		return new QuestDefinition(questId, 1, metadata, layout, nodes, List.copyOf(transitions));
	}
}
