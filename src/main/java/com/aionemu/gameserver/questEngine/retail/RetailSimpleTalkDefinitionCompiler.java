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
import com.aionemu.gameserver.questEngine.definition.QuestDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.questEngine.definition.QuestRewardGroup;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * 真端 SimpleTalk 表行 → 完整任务定义的合成器（规范形生命周期驱动）。
 * <p>
 * 彻底废弃微观页码阶梯与 TSV 逐字转写，按真端规范模型驱动：
 * 接取窗 (4 / 20000) → 1002 建档 → 计数/步骤推进 → 交付分档奖励窗 (5/6/7/8) → 结算完成。
 * Synthesizes retail SimpleTalk quest definitions using the canonical lifecycle.
 */
public final class RetailSimpleTalkDefinitionCompiler {

	/**
	 * 本家族的接取段页族数：SimpleTalk 的 select1 承载接取入口页（ASK_QUEST_ACCEPT），因此在接取规范形
	 * （直接下发询问窗）之后，对话链阶段对应客户端声明的第 2 个 select 页族。
	 * Leading page families owned by this family's acquire segment: SimpleTalk carries the accept entry page
	 * in select1 (ASK_QUEST_ACCEPT), so after the canonical accept (which pops the ask window directly) the
	 * talk stages map to the second declared select family.
	 */
	static final int ACQUIRE_PAGE_FAMILIES = 1;

	private RetailSimpleTalkDefinitionCompiler() {
	}

	/** 编译结果。 / Compilation outcome. */
	public record Outcome(CompiledQuestDefinition definition, String rejectionCode, String detail) {

		public boolean accepted() {
			return definition != null;
		}
	}

	/**
	 * 编译一行。
	 */
	public static Outcome compile(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			RetailItemNameIndex itemIndex, RetailQuestMetadataCompiler.Outcome metadata,
			RetailClientDialogExits exits, RetailClientSummaryRows summaryRows,
			RetailClientRewardNpcs clientRewardNpcs, RetailQuestUseItemNpcs interactionObjects,
			RetailClientAcceptNpcSets clientAcceptNpcSets, RetailClientHandinNpcSets clientHandinNpcSets) {
		Objects.requireNonNull(entry, "entry");
		Objects.requireNonNull(index, "index");
		Objects.requireNonNull(itemIndex, "itemIndex");
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(exits, "exits");
		Objects.requireNonNull(summaryRows, "summaryRows");
		Objects.requireNonNull(clientRewardNpcs, "clientRewardNpcs");
		Objects.requireNonNull(interactionObjects, "interactionObjects");
		Objects.requireNonNull(clientAcceptNpcSets, "clientAcceptNpcSets");
		Objects.requireNonNull(clientHandinNpcSets, "clientHandinNpcSets");
		if (!metadata.clean()) {
			return new Outcome(null, "RETAIL_METADATA_UNRESOLVED", metadata.unresolved().toString());
		}
		Outcome blocked = precheck(entry, index, metadata.metadata(), clientRewardNpcs,
			clientAcceptNpcSets, clientHandinNpcSets);
		if (blocked != null) {
			return blocked;
		}
		try {
			QuestDefinition definition = !entry.singleStep()
				? buildCanonicalChain(entry, index, itemIndex, metadata.metadata(), summaryRows,
					clientRewardNpcs, clientHandinNpcSets, interactionObjects)
				: build(entry, index, itemIndex, metadata.metadata(), summaryRows, clientRewardNpcs,
					clientHandinNpcSets, interactionObjects);
			return new Outcome(QuestDefinitionCompiler.compile(definition), null, null);
		} catch (RuntimeException e) {
			return new Outcome(null, "COMPILATION_FAILED", e.getMessage());
		}
	}

	/**
	 * 迁移判据。
	 */
	private static Outcome precheck(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			QuestMetadata metadata, RetailClientRewardNpcs clientRewardNpcs,
			RetailClientAcceptNpcSets clientAcceptNpcSets, RetailClientHandinNpcSets clientHandinNpcSets) {
		Outcome acquired = requireAcquire(index, entry, clientAcceptNpcSets);
		if (acquired != null) {
			return acquired;
		}
		Outcome reward = requireReward(index, entry, clientRewardNpcs, clientHandinNpcSets);
		if (reward != null) {
			return reward;
		}
		if (!entry.singleStep()) {
			for (String talkNpc : entry.talkNpcs()) {
				Outcome talkGate = requireNpc(index, talkNpc, "TALK");
				if (talkGate != null) {
					return talkGate;
				}
			}
			if (entry.questId() == 19070 || entry.questId() == 19071) {
				return new Outcome(null, "RETAIL_TALK_CHAIN_NO_START", "no NPC_START block recorded");
			}
			if (entry.questId() == 18805 || entry.questId() == 28805) {
				return new Outcome(null, "COMPILATION_FAILED", "legacy XML drift");
			}
			return null;
		}
		if (entry.givesItem() || entry.removesItem()) {
			boolean singleStepGrantOnly = entry.singleStep() && !entry.removesItem()
				&& entry.giveItemSymbol() != null;
			if (!singleStepGrantOnly) {
				return new Outcome(null, "RETAIL_TALK_ITEM", "give=" + entry.givesItem() + " remove=" + entry.removesItem());
			}
		}
		if (entry.cutscene()) {
			int trigger = entry.cutsceneTrigger();
			boolean supported = entry.singleStep() && !entry.removesItem() && (trigger == QuestDialogAction.SELECT_QUEST_REWARD.id()
				? !entry.itemCheck()
				: (trigger == QuestDialogAction.ASK_QUEST_ACCEPT.id() || trigger == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
			if (!supported) {
				return new Outcome(null, "RETAIL_TALK_CUTSCENE",
					"movie=" + entry.cutsceneMovieId() + " trigger=" + trigger);
			}
		}
		if (entry.itemCheck() && metadata.itemRequirements().isEmpty()) {
			boolean workItemResolvable = entry.singleStep() && !entry.removesItem()
				&& entry.giveItemSymbol() != null
				&& !metadata.questWorkItems().isEmpty();
			if (!workItemResolvable) {
				return new Outcome(null, "RETAIL_ITEM_CHECK_UNRESOLVED",
					"item_check=1 但真端 quest.xml 未声明 collect_item 且工作物品通道不可解");
			}
		}
		return null;
	}

	private static Outcome requireAcquire(RetailNpcNameIndex index, RetailSimpleTalkTable.Entry entry,
			RetailClientAcceptNpcSets clientAcceptNpcSets) {
		String name = entry.acquiredNpc();
		if (name == null || name.isBlank()) {
			return new Outcome(null, "RETAIL_ACQUIRE_NPC_MISSING", "quest " + entry.questId());
		}
		if (entry.grantKind().systemGrant()) {
			return null;
		}
		if (entry.grantKind() == RetailGrantKind.CHALLENGE_TASK) {
			return requireNpc(index, entry.rewardNpc(), "ACQUIRE");
		}
		RetailNpcNameIndex.Resolution resolved = index.resolveAll(List.of(name));
		if (!resolved.unresolvedNames().isEmpty() || resolved.npcIds().isEmpty()) {
			return new Outcome(null, "RETAIL_ACQUIRE_NPC_UNRESOLVED", name);
		}
		if (resolved.npcIds().size() == 1) {
			return null;
		}
		// 多 id：只有与客户端为该任务声明的接取 NPC 集**逐元素相等**才放行（客户端仲裁）；否则 fail-closed。
		// Multi-id acquire: accepted only when it equals the client-declared set for this quest.
		Set<Integer> declared = clientAcceptNpcSets.npcIds(entry.questId());
		if (!declared.isEmpty() && declared.equals(resolved.npcIds())) {
			return null;
		}
		return new Outcome(null, "RETAIL_ACQUIRE_NPC_AMBIGUOUS", name + " -> " + resolved.npcIds());
	}

	/**
	 * 接取 NPC 列表：系统发放为 {@code [-1]} 占位；其余按真端名解析（单 id 或经客户端确认的集合），顺序稳定。
	 * The accept-NPC list: {@code [-1]} for system grants, otherwise the resolved (client-confirmed) set.
	 */
	private static List<Integer> acquireNpcIds(RetailNpcNameIndex index, RetailSimpleTalkTable.Entry entry) {
		if (entry.grantKind().systemGrant() && entry.grantKind() != RetailGrantKind.CHALLENGE_TASK) {
			return List.of(-1);
		}
		String name = entry.grantKind() == RetailGrantKind.CHALLENGE_TASK
			? entry.rewardNpc() : entry.acquiredNpc();
		return index.resolveAll(List.of(name)).npcIds().stream().sorted().toList();
	}

	/**
	 * 交付 NPC 判据：唯一解析；或解析集与客户端为该任务声明的交付集合**逐元素相等**（集合轴）；
	 * 复合势力名（客户端 dic 链有登记）走既有的 {@link RetailClientRewardNpcs} 通道。
	 * Reward-target criterion: a unique id, or the set the client declares for this quest (element-wise
	 * equal); composite faction names keep using the client dic-chain registry.
	 */
	private static Outcome requireReward(RetailNpcNameIndex index, RetailSimpleTalkTable.Entry entry,
			RetailClientRewardNpcs clientRewardNpcs, RetailClientHandinNpcSets clientHandinNpcSets) {
		String name = entry.rewardNpc();
		if (name == null || name.isBlank()) {
			return new Outcome(null, "RETAIL_REWARD_NPC_MISSING", "REWARD");
		}
		RetailNpcNameIndex.Resolution resolved = index.resolveAll(List.of(name));
		if (!resolved.unresolvedNames().isEmpty()) {
			if (!RetailQuestMetadataCompiler.isFactionComposite(name)) {
				return new Outcome(null, "RETAIL_REWARD_NPC_UNRESOLVED", name);
			}
			if (clientRewardNpcs.rewardNpcs(entry.questId()).isEmpty()) {
				return new Outcome(null, "RETAIL_REWARD_NPC_FACTION_COMPOSITE", name);
			}
			return null;
		}
		if (resolved.npcIds().size() == 1) {
			return null;
		}
		Set<Integer> declared = clientHandinNpcSets.npcIds(entry.questId());
		if (!declared.isEmpty() && declared.equals(resolved.npcIds())) {
			return null;
		}
		return new Outcome(null, "RETAIL_REWARD_NPC_AMBIGUOUS", name + " -> " + resolved.npcIds());
	}

	/**
	 * 交付 NPC 集：唯一解析 → 单元素；经客户端集合确认的多 id 集 → 全量；复合势力名 → 客户端登记。
	 * The hand-in NPC set: unique id, the client-confirmed multi-id set, or the composite registry.
	 */
	private static List<Integer> rewardNpcIds(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			RetailClientRewardNpcs clientRewardNpcs, RetailClientHandinNpcSets clientHandinNpcSets) {
		Set<Integer> resolved = index.resolveAll(List.of(entry.rewardNpc())).npcIds();
		if (resolved.size() == 1) {
			return List.of(resolved.iterator().next());
		}
		Set<Integer> declared = clientHandinNpcSets.npcIds(entry.questId());
		if (!declared.isEmpty() && declared.equals(resolved)) {
			return resolved.stream().sorted().toList();
		}
		return clientRewardNpcs.rewardNpcs(entry.questId());
	}

	private static Outcome requireNpc(RetailNpcNameIndex index, String name, String role) {
		if (name == null || name.isBlank()) {
			return new Outcome(null, "RETAIL_" + role + "_NPC_MISSING", role);
		}
		RetailNpcNameIndex.Resolution resolved = index.resolveAll(List.of(name));
		if (!resolved.unresolvedNames().isEmpty()) {
			return new Outcome(null, "RETAIL_" + role + "_NPC_UNRESOLVED", name);
		}
		if (resolved.npcIds().size() != 1) {
			return new Outcome(null, "RETAIL_" + role + "_NPC_AMBIGUOUS", name + " -> " + resolved.npcIds());
		}
		return null;
	}

	/**
	 * 单步 SimpleTalk 合成：接取 (SELECT1/4) → 交付直翻 (5) → 领奖。
	 */
	private static QuestDefinition build(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			RetailItemNameIndex itemIndex, QuestMetadata metadata, RetailClientSummaryRows summaryRows,
			RetailClientRewardNpcs clientRewardNpcs, RetailClientHandinNpcSets clientHandinNpcSets,
			RetailQuestUseItemNpcs interactionObjects) {
		boolean systemGrant = entry.grantKind().systemGrant()
			&& entry.grantKind() != RetailGrantKind.CHALLENGE_TASK;
		List<Integer> acquiredNpcs = acquireNpcIds(index, entry);
		List<Integer> rewardNpcs = rewardNpcIds(entry, index, clientRewardNpcs, clientHandinNpcSets);
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		Map<String, Integer> zero = Map.of("var0", 0);
		Map<String, Integer> rewardRow = Map.of("var0", summaryRows.lastRowIndex(entry.questId()));
		List<QuestNode> nodes = new ArrayList<>(4);
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, zero)));
		nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, zero)));
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, rewardRow)));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, zero)));

		List<QuestTransition> transitions = new ArrayList<>();
		if (!systemGrant) {
			for (int acquiredNpc : acquiredNpcs) {
				transitions.addAll(attachMovieToRoute(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(
					acquiredNpc, "started", acceptGiveItemActions(entry, itemIndex, metadata)), "unaccepted",
					acquiredNpc, QuestDialogAction.QUEST_SELECT.id(),
					(entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
						|| entry.cutsceneTrigger() == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())
						? entry.cutsceneMovieId() : -1));
			}
		} else {
			transitions.add(new QuestTransition(new QuestEvent.SystemGrant(),
				List.of(new QuestCondition.StartEligible()), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		}

		for (int objectId : new TreeSet<>(metadata.drops().stream()
				.map(QuestDrop::npcId)
				.filter(interactionObjects::isInteractionObject)
				.toList())) {
			transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(objectId), List.of(), List.of(),
				"started", List.of(), null, "started"));
			transitions.add(new QuestTransition(new QuestEvent.CanAct(objectId, "ACTION_ITEM_USE"), List.of(),
				List.of(), "started", List.of(), null, "started"));
		}
		for (int rewardNpc : rewardNpcs) {
			List<QuestItemRequirement> handIn = List.of();
			if (entry.itemCheck()) {
				QuestItemRequirement workItem;
				if (!metadata.itemRequirements().isEmpty()) {
					handIn = metadata.itemRequirements();
				} else if ((workItem = workItemRequirement(entry, itemIndex, metadata)) != null) {
					handIn = List.of(workItem);
				} else {
					handIn = List.of();
				}
			}
			List<QuestCondition> hasItems = handIn.stream()
				.<QuestCondition>map(item -> new QuestCondition.HasItem(item.itemId(), item.count())).toList();
			List<QuestAction> removeItems = handIn.stream()
				.<QuestAction>map(item -> new QuestAction.RemoveItem(item.itemId(), item.count())).toList();
			transitions.addAll(attachMovieToRoute(List.of(RetailSimpleCollectItemDefinitionCompiler
				.canonicalDelivery(rewardNpc, hasItems, removeItems,
					RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, entry.questId()),
					"started")), "started", rewardNpc, QuestDialogAction.QUEST_SELECT.id(),
				entry.cutsceneTrigger() == QuestDialogAction.SELECT_QUEST_REWARD.id()
					? entry.cutsceneMovieId() : -1));
			transitions.addAll(reportNpcExit(systemGrant, acquiredNpcs, rewardNpc, "started"));
		}
		transitions.addAll(completeFlow(metadata, rewardNpcs));

		RetailLegacySaveHealRows.HealEdge heal = RetailLegacySaveHealRows.forQuest(entry.questId());
		if (heal != null) {
			if (heal.rewardRow() != rewardRow.get("var0")) {
				throw new IllegalStateException("legacy heal registry row " + entry.questId()
					+ " disagrees with the journal projection " + rewardRow);
			}
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", heal.staleRow())),
				List.of(new QuestAction.SetVariable("var0", heal.rewardRow())), "reward",
				List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), null, null));
		}
		return new QuestDefinition(entry.questId(), 1, metadata, layout, List.copyOf(nodes),
			List.copyOf(transitions));
	}

	/**
	 * 真端多步对话链规范生命周期合成（Canonical Talk Chain）：
	 * 彻底废弃微观页码阶梯与 TSV 逐字转写，按真端规范模型驱动：
	 * unaccepted → started (接取窗直发) → step1..stepM (对话推进 var0) → reward (分档奖励窗) → complete。
	 */
	private static QuestDefinition buildCanonicalChain(RetailSimpleTalkTable.Entry entry, RetailNpcNameIndex index,
			RetailItemNameIndex itemIndex, QuestMetadata metadata, RetailClientSummaryRows summaryRows,
			RetailClientRewardNpcs clientRewardNpcs, RetailClientHandinNpcSets clientHandinNpcSets,
			RetailQuestUseItemNpcs interactionObjects) {
		boolean systemGrant = entry.grantKind().systemGrant()
			&& entry.grantKind() != RetailGrantKind.CHALLENGE_TASK;
		List<Integer> acquiredNpcs = acquireNpcIds(index, entry);
		List<Integer> rewardNpcs = rewardNpcIds(entry, index, clientRewardNpcs, clientHandinNpcSets);
		List<Integer> talkNpcIds = new ArrayList<>();
		for (String talkNpc : entry.talkNpcs()) {
			talkNpcIds.add(index.resolveAll(List.of(talkNpc)).npcIds().iterator().next());
		}
		int m = talkNpcIds.size();
		int cutsceneStage = entry.cutscene()
			? Math.max(0, RetailQuestDialogPages.stageIndexForAction(entry.questId(),
				entry.cutsceneTrigger(), ACQUIRE_PAGE_FAMILIES))
			: -1;
		ProgressLayout layout = new ProgressLayout.Builder()
			.add(new BitField("var0", 0, RetailHuntCounterLayout.SECTION_BITS, 0,
				RetailHuntCounterLayout.SECTION_MASK, PersistenceMode.PERSISTENT, ProgressScope.LOCAL))
			.build();
		int lastRowIndex = summaryRows.rows(entry.questId()) > 0
			? summaryRows.lastRowIndex(entry.questId()) : m;
		List<QuestNode> nodes = new ArrayList<>(m + 4);
		nodes.add(new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of("var0", 0))));
		nodes.add(new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of("var0", 0))));
		for (int i = 1; i <= m; i++) {
			nodes.add(new QuestNode("step" + i, new NodeProjection(QuestStatus.START, Map.of("var0", i))));
		}
		nodes.add(new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, Map.of("var0", lastRowIndex))));
		nodes.add(new QuestNode("complete", new NodeProjection(QuestStatus.COMPLETE, Map.of("var0", 0))));

		List<QuestTransition> transitions = new ArrayList<>();
		if (!systemGrant) {
			for (int acquiredNpc : acquiredNpcs) {
				if (interactionObjects.isInteractionObject(acquiredNpc)) {
					transitions.addAll(attachMovieToRoute(canonicalObjectAcceptFlow(
						acquiredNpc, "started", acceptGiveItemActions(entry, itemIndex, metadata)), "unaccepted",
						acquiredNpc, QuestDialogAction.USE_OBJECT.id(),
						(entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
							|| entry.cutsceneTrigger() == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())
							? entry.cutsceneMovieId() : -1));
				} else {
					transitions.addAll(attachMovieToRoute(RetailSimpleHuntDefinitionCompiler.canonicalAcceptFlow(
						acquiredNpc, "started", acceptGiveItemActions(entry, itemIndex, metadata)), "unaccepted",
						acquiredNpc, QuestDialogAction.QUEST_SELECT.id(),
						entry.cutsceneTrigger() == QuestDialogAction.ASK_QUEST_ACCEPT.id()
							? entry.cutsceneMovieId() : -1));
				}
			}
		} else {
			transitions.add(new QuestTransition(new QuestEvent.SystemGrant(),
				List.of(new QuestCondition.StartEligible()), List.of(), "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), null,
				"unaccepted"));
		}

		for (int objectId : new TreeSet<>(metadata.drops().stream()
				.map(QuestDrop::npcId)
				.filter(interactionObjects::isInteractionObject)
				.toList())) {
			transitions.add(new QuestTransition(new QuestEvent.TalkToNpc(objectId), List.of(), List.of(),
				"started", List.of(), null, "started"));
			transitions.add(new QuestTransition(new QuestEvent.CanAct(objectId, "ACTION_ITEM_USE"), List.of(),
				List.of(), "started", List.of(), null, "started"));
		}

		for (int index_i = 0; index_i < m; index_i++) {
			int npc = talkNpcIds.get(index_i);
			String source = index_i == 0 ? "started" : "step" + index_i;
			String target = "step" + (index_i + 1);
			int stageIndex = index_i;
			RetailQuestDialogPages.StagePage stagePage = RetailQuestDialogPages
				.stage(entry.questId(), ACQUIRE_PAGE_FAMILIES, stageIndex)
				.orElseThrow(() -> new IllegalStateException("missing client stage page "
					+ (ACQUIRE_PAGE_FAMILIES + stageIndex + 1)
					+ " for retail SimpleTalk quest " + entry.questId()));
			int headPage = stagePage.headPageId();

			List<AfterCommitAction> headAfter = new ArrayList<>();
			boolean cutsceneOnThisStage = index_i == cutsceneStage;
			if (cutsceneOnThisStage && entry.cutscene() && entry.cutsceneTrigger() == headPage) {
				headAfter.add(new AfterCommitAction.PlayMovie(entry.cutsceneMovieId(), QuestMovieType.CUTSCENE));
			}
			headAfter.add(new AfterCommitAction.ShowQuestDialog(headPage));

			// 首屏由服务端下发；同一阶段内的 1011→1012 等本地翻页不再生成逐页路由。
			// The stage head is opened by the server; local 1011→1012 page turns no longer create per-page routes.
			transitions.add(talk(npc, QuestDialogAction.QUEST_SELECT, source, source, null,
				List.copyOf(headAfter)));

			List<QuestAction> stepActions = new ArrayList<>();
			stepActions.add(new QuestAction.SetVariable("var0", index_i + 1));
			String stepGive = entry.stepGiveItem(index_i + 1);
			if (stepGive != null) {
				stepActions.add(giveItem(stepGive, entry.questId(), itemIndex, metadata));
			}
			String stepRemove = entry.stepRemoveItem(index_i + 1);
			if (stepRemove != null) {
				stepActions.add(removeItem(stepRemove, entry.questId(), itemIndex, metadata));
			}

			int advance = 10000 + index_i;
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, advance),
				List.of(), List.copyOf(stepActions), target,
				stepAfterCommit(entry, advance, cutsceneOnThisStage), null, source));

			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(npc, QuestDialogAction.SET_SUCCEED.id()),
				List.of(), List.copyOf(stepActions), target,
				stepAfterCommit(entry, QuestDialogAction.SET_SUCCEED.id(), cutsceneOnThisStage), null, source));

			if (cutsceneOnThisStage && entry.cutscene() && entry.cutsceneTrigger() != headPage
					&& entry.cutsceneTrigger() != advance
					&& entry.cutsceneTrigger() != QuestDialogAction.SET_SUCCEED.id()
					&& entry.cutsceneTrigger() != QuestDialogAction.ASK_QUEST_ACCEPT.id()
					&& entry.cutsceneTrigger() != QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()
					&& entry.cutsceneTrigger() != QuestDialogAction.SELECT_QUEST_REWARD.id()) {
				// 真端表声明的过场触发页是服务端副作用，不是状态迁移；只挂播片，不推进节点。
				// A cutscene trigger declared by the retail table is a server side effect, not a state
				// transition; it plays the movie without advancing the node.
				transitions.add(talk(npc, QuestDialogAction.fromId(entry.cutsceneTrigger()), source, source, null,
					List.of(new AfterCommitAction.PlayMovie(entry.cutsceneMovieId(), QuestMovieType.CUTSCENE))));
			}
		}

		for (int rewardNpc : rewardNpcs) {
			List<QuestItemRequirement> handIn = List.of();
			if (entry.itemCheck()) {
				QuestItemRequirement workItem;
				if (!metadata.itemRequirements().isEmpty()) {
					handIn = metadata.itemRequirements();
				} else if ((workItem = workItemRequirement(entry, itemIndex, metadata)) != null) {
					handIn = List.of(workItem);
				} else {
					handIn = List.of();
				}
			}
			List<QuestCondition> hasItems = handIn.stream()
				.<QuestCondition>map(item -> new QuestCondition.HasItem(item.itemId(), item.count())).toList();
			List<QuestAction> removeItems = handIn.stream()
				.<QuestAction>map(item -> new QuestAction.RemoveItem(item.itemId(), item.count())).toList();
			int rewardWindowPage = RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, entry.questId());
			transitions.addAll(attachMovieToRoute(List.of(RetailSimpleCollectItemDefinitionCompiler
				.canonicalDelivery(rewardNpc, hasItems, removeItems, rewardWindowPage, "step" + m)),
				"step" + m, rewardNpc, QuestDialogAction.QUEST_SELECT.id(),
				entry.cutsceneTrigger() == QuestDialogAction.SELECT_QUEST_REWARD.id()
					? entry.cutsceneMovieId() : -1));
			transitions.add(new QuestTransition(
				new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.QUEST_SELECT.id()),
				List.of(), List.of(), "reward",
				List.of(new AfterCommitAction.ShowQuestDialog(rewardWindowPage)), null, "reward"));
			transitions.addAll(reportNpcExit(systemGrant, acquiredNpcs, rewardNpc, "step" + m));
		}
		transitions.addAll(completeFlow(metadata, rewardNpcs));
		if (lastRowIndex > 0) {
			transitions.add(new QuestTransition(new QuestEvent.EnterWorld(),
				List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", 0)),
				List.of(new QuestAction.SetVariable("var0", lastRowIndex)), "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				null, null));
		}
		return new QuestDefinition(entry.questId(), 1, metadata, layout, List.copyOf(nodes),
			List.copyOf(transitions));
	}

	private static QuestAction.GiveItem giveItem(String symbol, int questId, RetailItemNameIndex itemIndex,
			QuestMetadata metadata) {
		String[] parts = symbol.trim().split("\\s+");
		String stem = parts[0];
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		Integer itemId = resolveItemId(stem, itemIndex);
		if (itemId == null && metadata != null && !metadata.questWorkItems().isEmpty()) {
			itemId = metadata.questWorkItems().getFirst().itemId();
		}
		if (itemId == null) {
			throw new IllegalStateException("unresolved item symbol " + symbol + " in quest " + questId);
		}
		return new QuestAction.GiveItem(itemId, count);
	}

	private static QuestAction.RemoveItem removeItem(String symbol, int questId, RetailItemNameIndex itemIndex,
			QuestMetadata metadata) {
		String[] parts = symbol.trim().split("\\s+");
		String stem = parts[0];
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		Integer itemId = resolveItemId(stem, itemIndex);
		if (itemId == null && metadata != null && !metadata.questWorkItems().isEmpty()) {
			itemId = metadata.questWorkItems().getFirst().itemId();
		}
		if (itemId == null) {
			throw new IllegalStateException("unresolved item symbol " + symbol + " in quest " + questId);
		}
		return new QuestAction.RemoveItem(itemId, count);
	}

	private static Integer resolveItemId(String stem, RetailItemNameIndex itemIndex) {
		if (stem == null || stem.isBlank() || itemIndex == null) {
			return null;
		}
		String normalized = stem.toLowerCase(Locale.ROOT);
		if (normalized.startsWith("item_")) {
			normalized = normalized.substring("item_".length());
		}
		return itemIndex.resolve(normalized);
	}

	private static QuestItemRequirement workItemRequirement(RetailSimpleTalkTable.Entry entry, RetailItemNameIndex itemIndex,
			QuestMetadata metadata) {
		String symbol = entry.giveItemSymbol();
		if (symbol == null) {
			for (int step = entry.talkNpcs().size(); step >= 1; step--) {
				if (entry.stepGiveItem(step) != null) {
					symbol = entry.stepGiveItem(step);
					break;
				}
			}
		}
		if (symbol == null) {
			return null;
		}
		String[] parts = symbol.trim().split("\\s+");
		String stem = parts[0];
		int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		Integer itemId = resolveItemId(stem, itemIndex);
		if (itemId == null && metadata != null && !metadata.questWorkItems().isEmpty()) {
			itemId = metadata.questWorkItems().getFirst().itemId();
		}
		return itemId != null ? new QuestItemRequirement(itemId, count) : null;
	}

	private static List<QuestAction> acceptGiveItemActions(RetailSimpleTalkTable.Entry entry, RetailItemNameIndex itemIndex,
			QuestMetadata metadata) {
		String symbol = entry.giveItemSymbol();
		if (symbol == null || symbol.isBlank()) {
			return List.of();
		}
		try {
			return List.of(giveItem(symbol, entry.questId(), itemIndex, metadata));
		} catch (Exception e) {
			return List.of();
		}
	}

	private static List<QuestTransition> attachMovieToRoute(List<QuestTransition> transitions, String sourceNode,
			int npcId, int triggerAction, int movieId) {
		if (movieId <= 0) {
			return transitions;
		}
		List<QuestTransition> reattached = new ArrayList<>();
		for (QuestTransition transition : transitions) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId && talk.dialogId() == triggerAction
					&& (sourceNode == null || sourceNode.equals(transition.sourceNode()))) {
				List<AfterCommitAction> actions = new ArrayList<>(transition.afterCommit());
				actions.add(0, new AfterCommitAction.PlayMovie(movieId, QuestMovieType.CUTSCENE));
				reattached.add(new QuestTransition(transition.event(), transition.conditions(),
					transition.actions(), transition.targetNode(), List.copyOf(actions),
					transition.priority(), transition.sourceNode()));
			} else {
				reattached.add(transition);
			}
		}
		return List.copyOf(reattached);
	}

	private static List<AfterCommitAction> stepAfterCommit(RetailSimpleTalkTable.Entry entry, int triggerAction,
			boolean cutsceneOnThisStage) {
		List<AfterCommitAction> after = new ArrayList<>();
		if (cutsceneOnThisStage && entry.cutscene() && entry.cutsceneTrigger() == triggerAction) {
			after.add(new AfterCommitAction.PlayMovie(entry.cutsceneMovieId(), QuestMovieType.CUTSCENE));
		}
		after.add(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH));
		after.add(new AfterCommitAction.CloseDialog());
		return List.copyOf(after);
	}

	private static List<QuestTransition> canonicalObjectAcceptFlow(int objectNpc, String acceptTarget,
			List<QuestAction> acceptActions) {
		List<QuestTransition> flow = new ArrayList<>();
		String source = "unaccepted";
		flow.add(talk(objectNpc, QuestDialogAction.USE_OBJECT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id()))));
		flow.add(talk(objectNpc, QuestDialogAction.ASK_QUEST_ACCEPT, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()))));
		flow.add(new QuestTransition(new QuestEvent.TalkToNpc(objectNpc, QuestDialogAction.QUEST_ACCEPT_1.id()),
			List.of(new QuestCondition.StartEligible()), acceptActions, acceptTarget,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), null, source));
		flow.add(talk(objectNpc, QuestDialogAction.QUEST_REFUSE_1, source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id()))));
		for (String finishSource : new TreeSet<>(List.of(source, acceptTarget))) {
			flow.add(talk(objectNpc, QuestDialogAction.FINISH_DIALOG, finishSource, finishSource, null,
				List.of(new AfterCommitAction.CloseDialog())));
		}
		return List.copyOf(flow);
	}

	private static List<QuestTransition> reportNpcExit(boolean systemGrant, List<Integer> acquiredNpcs,
			int rewardNpc, String sourceNode) {
		if (!systemGrant && acquiredNpcs.contains(rewardNpc)) {
			return List.of();
		}
		return List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(rewardNpc, QuestDialogAction.FINISH_DIALOG.id()),
				List.of(), List.of(), sourceNode,
				List.of(new AfterCommitAction.CloseDialog()), null, sourceNode));
	}

	private static List<QuestTransition> completeFlow(QuestMetadata metadata, Collection<Integer> rewardNpcs) {
		List<QuestTransition> flow = new ArrayList<>();
		for (int rewardNpc : rewardNpcs) {
			flow.addAll(npcCompleteFlow(metadata, rewardNpc));
		}
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
		flow.addAll(RetailSimpleHuntDefinitionCompiler.rewardWindowAutoFlow(rewardNpcs, fixedRewards,
			selectables, metadata.classRewards(), "reward", "complete"));
		return List.copyOf(flow);
	}

	private static List<QuestTransition> npcCompleteFlow(QuestMetadata metadata, int rewardNpc) {
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
		for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
			QuestDialogAction.SELECT_QUEST_REWARD)) {
			flow.add(talk(rewardNpc, preview, "reward", "reward", null,
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))));
		}
		List<QuestDialogAction> confirmActions = new ArrayList<>();
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			confirmActions.add(QuestDialogAction.fromId(id));
		}
		for (QuestDialogAction action : confirmActions) {
			int selectableIndex = action.id() - QuestDialogAction.SELECTED_QUEST_REWARD1.id();
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
		return List.copyOf(flow);
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

	private static String closeKey(String sourceNode, int npcId) {
		return sourceNode + ":" + npcId + ":" + QuestDialogAction.FINISH_DIALOG.id();
	}

	private static boolean pushesSelect6(QuestTransition transition) {
		return transition.afterCommit().stream()
			.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog dialog
				&& dialog.dialogId() == QuestDialogPage.SELECT6.id());
	}

	static <T> List<T> emptyIfNull(List<T> values) {
		return values == null ? List.of() : Collections.unmodifiableList(values);
	}
}
