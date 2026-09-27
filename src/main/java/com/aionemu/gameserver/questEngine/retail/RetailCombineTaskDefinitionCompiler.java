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
import com.aionemu.gameserver.questEngine.definition.QuestRecipeOwnership;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 真端 CombineTask 表行 → 完整任务定义（无 shell）的合成器。
 * <p>
 * 全族同一形状（M4-a 勘察：574/574 四节点 {@code var0=0}，无行号投影）：<br>
 * 1. 接取（P0-2 规范形）：QUEST_SELECT 直发接取窗（页 4；select1 页/1007 中转已随页链删除）——
 * 发工料分量（{@code give_componentN}）+ 学配方；<br>
 * 2. 报告：交付产物（{@code has-item}）成功 → REWARD 并回收剩余分量；未交付 → 回退页 {@code SELECT3_2}；<br>
 * 3. 完成：奖励确认动作 8..23 → 扣产物、忘配方、{@code complete-quest}；<br>
 * 4. 放弃：{@code abandon} → 忘配方。
 * <p>
 * 真端表给不出配方 id（只有符号名），由 {@link RetailRecipeIndex} 按 {@code (skillid, productid)} 反查；
 * 技能/技能点/产物/分量同时与真端 {@code quest.xml} 元数据交叉校验，任何一侧不一致都按稳定码拒绝。
 * <p>
 * Synthesizes the single retail CombineTask shape; divergent rows get stable rejection codes so they stay
 * on the XML fallback.
 */
public final class RetailCombineTaskDefinitionCompiler {

	/** 交付失败的客户端回退页（与历史 XML 形状一致）。 / Client fallback page when the product is missing. */
	private static final QuestDialogPage FAIL_PAGE = QuestDialogPage.SELECT3_2;
	/** 产物确认动作区间：{@code SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD}。 / Reward confirmation range. */
	private static final int FIRST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
	private static final int LAST_CONFIRM_ACTION = QuestDialogAction.SELECTED_QUEST_NOREWARD.id();

	private RetailCombineTaskDefinitionCompiler() {
	}

	/** 编译结果（拒绝时 {@code rejectionCode} 稳定可登记）。 / Compilation outcome with a stable rejection code. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 编译一行：{@code metadata} 是真端 quest.xml 侧元数据（技能/技能点/产物/分量），
	 * {@code recipeIndex} 把 {@code (skillid, productid)} 反查成 recipe id。
	 * Compiles one retail row against the retail metadata and the local recipe index.
	 */
	public static Outcome compile(RetailCombineTaskTable.Entry entry, RetailNpcNameIndex npcIndex,
			RetailItemNameIndex itemIndex, RetailRecipeIndex recipeIndex,
			RetailQuestMetadataCompiler.Outcome metadata) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(itemIndex, "itemIndex");
		Objects.requireNonNull(recipeIndex, "recipeIndex");
		Objects.requireNonNull(metadata, "metadata");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		Outcome blocked = precheck(entry, npcIndex, itemIndex, recipeIndex, metadata.metadata());
		if (blocked != null) {
			return blocked;
		}
		try {
			QuestDefinition definition = build(entry, npcIndex, itemIndex, recipeIndex, metadata.metadata());
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 迁移判据（稳定拒绝码，供保留清单消费）：
	 * <ul>
	 * <li>{@code RETAIL_ACQUIRE_NPC_*}：接取 NPC 名字解析不到 / 多解 / 是类别哨兵；</li>
	 * <li>{@code RETAIL_COMBINE_SKILL_*}：合成技能未知或与真端 quest.xml 不一致；</li>
	 * <li>{@code RETAIL_COMBINE_PRODUCT_*} / {@code RETAIL_COMBINE_COMPONENT_*}：产物/分量名解析不到、形态不是单产物；</li>
	 * <li>{@code RETAIL_COMBINE_RECIPE_*}：{@code (skillid, productid)} 在配方表里缺行或多解；</li>
	 * <li>{@code RETAIL_COMBINE_METADATA_*}：真端表与 quest.xml 的产物/分量/技能点不一致。</li>
	 * </ul>
	 * Migration pre-check with stable rejection codes.
	 */
	private static Outcome precheck(RetailCombineTaskTable.Entry entry, RetailNpcNameIndex npcIndex,
			RetailItemNameIndex itemIndex, RetailRecipeIndex recipeIndex, QuestMetadata metadata) {
		if (entry.taskNpcs().isEmpty()) {
			return new Outcome(null, "RETAIL_ACQUIRE_NPC_UNRESOLVED", "task_npc 为空");
		}
		for (String npc : entry.taskNpcs()) {
			Outcome npcOutcome = requireNpc(npcIndex, npc);
			if (npcOutcome != null) {
				return npcOutcome;
			}
		}
		Integer skillId = RetailQuestMetadataCompiler.combineSkillId(entry.combineSkill());
		if (skillId == null) {
			return new Outcome(null, "RETAIL_COMBINE_SKILL_UNRESOLVED", String.valueOf(entry.combineSkill()));
		}
		if (!Objects.equals(skillId, metadata.combineSkill())) {
			return new Outcome(null, "RETAIL_COMBINE_SKILL_MISMATCH",
				"table=" + skillId + " quest.xml=" + metadata.combineSkill());
		}
		Integer skillPoint = metadata.combineSkillPoint() == null ? 0 : metadata.combineSkillPoint();
		if (skillPoint != entry.skillPoint()) {
			return new Outcome(null, "RETAIL_COMBINE_SKILL_POINT_MISMATCH",
				"table=" + entry.skillPoint() + " quest.xml=" + skillPoint);
		}
		if (entry.products().size() != 1) {
			return new Outcome(null, "RETAIL_COMBINE_PRODUCT_SHAPE", entry.products().toString());
		}
		RetailCombineTaskTable.Slot product = entry.products().get(0);
		Integer productId = itemIndex.resolve(product.name());
		if (productId == null) {
			return new Outcome(null, "RETAIL_COMBINE_PRODUCT_UNRESOLVED", product.name());
		}
		if (entry.components().isEmpty()) {
			return new Outcome(null, "RETAIL_COMBINE_COMPONENT_UNRESOLVED", "give_component* 为空");
		}
		List<QuestItemRequirement> components = new ArrayList<>(entry.components().size());
		for (RetailCombineTaskTable.Slot component : entry.components()) {
			Integer componentId = itemIndex.resolve(component.name());
			if (componentId == null) {
				return new Outcome(null, "RETAIL_COMBINE_COMPONENT_UNRESOLVED", component.name());
			}
			components.add(new QuestItemRequirement(componentId, component.count()));
		}
		Set<Integer> recipes = recipeIndex.resolveAll(skillId, productId);
		if (recipes.isEmpty()) {
			return new Outcome(null, "RETAIL_COMBINE_RECIPE_UNRESOLVED",
				"skill=" + skillId + " product=" + productId + " name=" + entry.recipeName());
		}
		if (recipes.size() > 1) {
			return new Outcome(null, "RETAIL_COMBINE_RECIPE_AMBIGUOUS", recipes.toString());
		}
		List<QuestItemRequirement> declaredProducts = metadata.itemRequirements();
		if (declaredProducts.size() != 1 || declaredProducts.get(0).itemId() != productId
			|| declaredProducts.get(0).count() != product.count()) {
			return new Outcome(null, "RETAIL_COMBINE_METADATA_PRODUCT_MISMATCH",
				"quest.xml=" + declaredProducts + " table=" + productId + "x" + product.count());
		}
		if (!metadata.questWorkItems().equals(components)) {
			return new Outcome(null, "RETAIL_COMBINE_METADATA_COMPONENT_MISMATCH",
				"quest.xml=" + metadata.questWorkItems() + " table=" + components);
		}
		return null;
	}

	/** 单个 NPC 名判定：唯一 → 放行；空集/多解按名字形态归类。 / Single-NPC gate for the accept NPCs. */
	private static Outcome requireNpc(RetailNpcNameIndex index, String name) {
		Set<Integer> ids = index.resolveAll(List.of(name == null ? "" : name)).npcIds();
		if (ids.size() == 1) {
			return null;
		}
		String raw = name == null ? "" : name.trim();
		if (ids.size() > 1) {
			return new Outcome(null, "RETAIL_ACQUIRE_NPC_AMBIGUOUS", raw + " -> " + ids);
		}
		String kind = raw.length() > 2 && raw.startsWith("_") && raw.endsWith("_")
			? "_NPC_SENTINEL" : "_NPC_UNRESOLVED";
		return new Outcome(null, "RETAIL_ACQUIRE" + kind, raw);
	}

	private static QuestDefinition build(RetailCombineTaskTable.Entry entry, RetailNpcNameIndex npcIndex,
			RetailItemNameIndex itemIndex, RetailRecipeIndex recipeIndex, QuestMetadata metadata) {
		List<Integer> npcIds = new ArrayList<>(entry.taskNpcs().size());
		for (String npc : entry.taskNpcs()) {
			npcIds.add(npcIndex.resolveAll(List.of(npc)).npcIds().iterator().next());
		}
		int skillId = RetailQuestMetadataCompiler.combineSkillId(entry.combineSkill());
		RetailCombineTaskTable.Slot product = entry.products().get(0);
		int productId = itemIndex.resolve(product.name());
		int recipeId = recipeIndex.resolveUnique(skillId, productId).orElseThrow();
		List<QuestAction> acceptActions = new ArrayList<>(entry.components().size() + 1);
		for (RetailCombineTaskTable.Slot component : entry.components()) {
			acceptActions.add(new QuestAction.GiveItem(itemIndex.resolve(component.name()), component.count()));
		}
		acceptActions.add(new QuestAction.LearnRecipe(recipeId, QuestRecipeOwnership.QUEST_OWNED));

		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		List<QuestNode> nodes = List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)),
			new QuestNode("started", new NodeProjection(QuestStatus.START, zero)),
			new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, zero)),
			new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));

		List<QuestTransition> transitions = new ArrayList<>();
		Set<Integer> taskNpcs = new LinkedHashSet<>(npcIds);
		for (int npcId : taskNpcs) {
			// 接取规范形（P0-2）：QUEST_SELECT 直发接取窗（页 4），select1 页与 1007 中转随页链删除；
			// 交付段（1009 双 prio + SELECT3_2 合成回退页）与完成/放弃流保持族形（无页链，已合规）。
			// Canonical accept (P0-2): QUEST_SELECT pops the ask window (page 4) directly; the
			// select1 page and 1007 hop are gone. The report/complete/abandon flows keep the family
			// shape (no page chain; already canonical).
			transitions.addAll(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(
				npcId, "started", acceptActions));
			transitions.addAll(reportFlow(npcId, productId, product.count(), entry.components(), itemIndex));
		}
		transitions.addAll(completeFlow(taskNpcs, productId, recipeId));
		transitions.add(new QuestTransition(new QuestEvent.Abandon(), List.of(),
			List.of(new QuestAction.ForgetRecipe(recipeId)), "unaccepted", List.of(), null, "started"));
		return new QuestDefinition(entry.questId(), 1, metadata, layout, nodes, List.copyOf(transitions));
	}

	/** 报告流：交付产物成功 → REWARD（回收剩余分量）；缺产物 → 回退页。 / Turn-in flow plus its fallback. */
	private static List<QuestTransition> reportFlow(int npcId, int productId, int productCount,
			List<RetailCombineTaskTable.Slot> components, RetailItemNameIndex itemIndex) {
		List<QuestAction> removeComponents = new ArrayList<>(components.size());
		for (RetailCombineTaskTable.Slot component : components) {
			removeComponents.add(new QuestAction.RemoveItem(itemIndex.resolve(component.name()),
				QuestAction.RemoveItem.ALL));
		}
		List<QuestTransition> flow = new ArrayList<>(2);
		flow.add(new QuestTransition(
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			List.of(new QuestCondition.HasItem(productId, productCount)), List.copyOf(removeComponents),
			"reward",
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), 0, "started"));
		flow.add(new QuestTransition(
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			List.of(), List.of(), "started",
			List.of(new AfterCommitAction.ShowQuestDialog(FAIL_PAGE.id())), 10, "started"));
		return List.copyOf(flow);
	}

	/** 完成流：奖励确认 8..23 —— 扣产物、忘配方、完成任务。 / Completion range with product removal. */
	private static List<QuestTransition> completeFlow(Set<Integer> npcIds, int productId, int recipeId) {
		List<AfterCommitAction> afterCommit = List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
		List<QuestAction> actions = List.of(new QuestAction.RemoveItem(productId, QuestAction.RemoveItem.ALL),
			new QuestAction.ForgetRecipe(recipeId), new QuestAction.CompleteQuest(0));
		List<QuestTransition> flow = new ArrayList<>(
			npcIds.size() * (LAST_CONFIRM_ACTION - FIRST_CONFIRM_ACTION + 2) + 1);
		for (int npcId : npcIds) {
			for (int id = FIRST_CONFIRM_ACTION; id <= LAST_CONFIRM_ACTION; id++) {
				flow.add(new QuestTransition(new QuestEvent.TalkToNpc(npcId, id), List.of(), actions, "complete",
					afterCommit, null, "reward"));
			}
		}
		// 奖励窗口自动确认通道（108，双协议注册 + CloseDialog 收窗）：窗口 UI 全局发出，
		// 可能不带 NPC 上下文。
		// The reward-window auto-confirm channel (108, dual-protocol registration with CloseDialog):
		// the window UI is global and may lack NPC context.
		List<AfterCommitAction> closeWindow = List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.CloseDialog());
		flow.add(new QuestTransition(new QuestEvent.QuestDialog(
			QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()), List.of(), actions, "complete",
			closeWindow, null, "reward"));
		for (int npcId : npcIds) {
			flow.add(new QuestTransition(new QuestEvent.TalkToNpc(npcId,
				QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()), List.of(), actions, "complete",
				closeWindow, null, "reward"));
		}
		return List.copyOf(flow);
	}
}
