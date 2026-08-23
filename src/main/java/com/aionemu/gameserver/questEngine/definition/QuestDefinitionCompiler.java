package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * XML 与 Java DSL 前端共用的结构化编译器。
 * Structural compiler shared by XML and Java DSL front ends.
 */
public final class QuestDefinitionCompiler {
	private QuestDefinitionCompiler() {
	}

	public static CompiledQuestDefinition compile(QuestDefinition definition) {
		definition = restoreStartEligibilityContract(Objects.requireNonNull(definition, "definition"));
		definition = restoreRepeatStartDialogContract(definition);
		definition = restoreRewardPreviewContract(definition);
		if (definition.nodes().isEmpty()) {
			fail("NO_NODES", "executable definition has no nodes");
		}
		if (definition.transitions().isEmpty()) {
			fail("NO_TRANSITIONS", "executable definition has no transitions");
		}
		if (definition.metadata().maxCountLimitedQuest() > 1) {
			fail("LIMITED_QUEST_START_UNSUPPORTED",
				"limited quest quota acquisition is not part of the shared transaction");
		}

		Map<String, QuestNode> nodes = new HashMap<>();
		Set<String> projections = new HashSet<>();
		for (QuestNode node : definition.nodes()) {
			if (nodes.putIfAbsent(node.label(), node) != null) {
				fail("DUPLICATE_NODE", "duplicate node: " + node.label());
			}
			for (String variable : node.projection().variables().keySet()) {
				if (definition.progressLayout().field(variable) == null) {
					fail("UNKNOWN_PROGRESS_FIELD", "node references unknown field: " + variable);
				}
			}
			int packedProjection = definition.progressLayout().pack(node.projection().variables());
			String projectionKey = node.projection().status().name() + ":" + packedProjection;
			if (!projections.add(projectionKey)) {
				fail("DUPLICATE_NODE_PROJECTION", "nodes share the same QuestStatus + quest_vars projection: "
						+ projectionKey);
			}
		}

		Map<String, Set<String>> outgoing = new HashMap<>();
		for (QuestTransition transition : definition.transitions()) {
			if (transition.sourceNode() != null && !nodes.containsKey(transition.sourceNode())) {
				fail("BAD_NODE_REFERENCE", "transition has unknown source node: " + transition.sourceNode());
			}
			if (!nodes.containsKey(transition.targetNode())) {
				fail("BAD_NODE_REFERENCE", "transition points to unknown node: " + transition.targetNode());
			}
			for (QuestCondition condition : transition.conditions()) {
				if (condition instanceof QuestCondition.PvpVictimLevelDelta
						&& !(transition.event() instanceof QuestEvent.KillInWorld)) {
					fail("PVP_CONDITION_EVENT_MISMATCH",
						"pvp-victim-level-delta is only valid on kill-in-world events");
				}
				if (condition instanceof QuestCondition.PvpRecipientInZone
						&& !(transition.event() instanceof QuestEvent.KillRanked
						|| transition.event() instanceof QuestEvent.KillInWorld)) {
					fail("PVP_CONDITION_EVENT_MISMATCH",
						"pvp-recipient-in-zone is only valid on PvP kill events");
				}
				if (condition instanceof QuestCondition.QuestVariableIs variable) {
					validateProgressField(definition, variable.field(), "condition");
					definition.progressLayout().pack(Map.of(variable.field(), variable.value()));
				}
				if (condition instanceof QuestCondition.VariableAtLeast variable) {
					validateProgressField(definition, variable.field(), "condition");
				}
				if (condition instanceof QuestCondition.VariableBelow variable) {
					validateProgressField(definition, variable.field(), "condition");
				}
				if (condition instanceof QuestCondition.VariableSumIs variable) {
					validateProgressFields(definition, variable.fields(), "condition");
				}
				if (condition instanceof QuestCondition.VariableSumBelow variable) {
					validateProgressFields(definition, variable.fields(), "condition");
				}
			}
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.GrantSelectedReward selected
						&& selected.rewardIndex() >= definition.metadata().rewards().size()) {
					fail("SELECTED_REWARD_INDEX_OUT_OF_RANGE",
						"selected reward index " + selected.rewardIndex() + " is not present in quest metadata");
				}
				if (action instanceof QuestAction.SetVariable variable) {
					if (definition.progressLayout().field(variable.field()) == null) {
						fail("UNKNOWN_PROGRESS_FIELD", "action references unknown field: " + variable.field());
					}
					definition.progressLayout().pack(Map.of(variable.field(), variable.value()));
				}
				if (action instanceof QuestAction.GrantReward reward
						&& reward.amountMode() == QuestRewardAmountMode.QUEST_BASE
						&& reward.rewardKind() != QuestRewardKind.GOLD
						&& reward.rewardKind() != QuestRewardKind.KINAH
						&& reward.rewardKind() != QuestRewardKind.AP
						&& reward.rewardKind() != QuestRewardKind.GP
						&& reward.rewardKind() != QuestRewardKind.EXP) {
					fail("QUEST_BASE_REWARD_KIND_UNSUPPORTED",
						"QUEST_BASE amount mode is unsupported for " + reward.rewardKind());
				}
			}
			QuestStatus effectiveStatus = nodes.get(transition.targetNode()).projection().status();
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.SetStatus setStatus) {
					effectiveStatus = setStatus.status();
				}
			}
			long completions = transition.actions().stream()
				.filter(QuestAction.CompleteQuest.class::isInstance).count();
			long archDaevaPromotions = transition.actions().stream()
				.filter(QuestAction.PromoteArchDaeva.class::isInstance).count();
			boolean blockOnly = transition.actions().size() == 1
				&& transition.actions().get(0) instanceof QuestAction.BlockDefaultItemUse;
			if (blockOnly && !(transition.event() instanceof QuestEvent.UseItem)) {
				fail("BLOCK_DEFAULT_ITEM_USE_EVENT_MISMATCH",
					"block-default-item-use is only valid on use-item events");
			}
			boolean preservesCompletedProjection = transition.sourceNode() != null
				&& transition.sourceNode().equals(transition.targetNode())
				&& nodes.get(transition.sourceNode()).projection().status() == QuestStatus.COMPLETE
				&& effectiveStatus == QuestStatus.COMPLETE
				&& (transition.actions().isEmpty() || blockOnly);
			if (effectiveStatus == QuestStatus.COMPLETE && completions != 1 && !preservesCompletedProjection) {
				fail("COMPLETE_QUEST_ACTION_REQUIRED",
					"a COMPLETE projection requires exactly one complete-quest action");
			}
			if (effectiveStatus != QuestStatus.COMPLETE && completions != 0) {
				fail("COMPLETE_QUEST_STATUS_MISMATCH",
					"complete-quest action requires a COMPLETE projection");
			}
			if (archDaevaPromotions > 1 || (archDaevaPromotions == 1
					&& (effectiveStatus != QuestStatus.COMPLETE || completions != 1))) {
				fail("ARCHDAEVA_PROMOTION_COMPLETION_REQUIRED",
					"promote-archdaeva requires exactly one complete-quest action and a COMPLETE projection");
			}
			boolean abandons = transition.actions().stream()
				.anyMatch(QuestAction.AbandonQuest.class::isInstance);
			if (abandons && effectiveStatus != QuestStatus.NONE) {
				fail("ABANDON_QUEST_STATUS_MISMATCH",
					"abandon-quest action requires a NONE projection");
			}
			List<AfterCommitAction.SyncQuestState> stateSyncs = transition.afterCommit().stream()
				.filter(AfterCommitAction.SyncQuestState.class::isInstance)
				.map(AfterCommitAction.SyncQuestState.class::cast).toList();
			if (stateSyncs.size() > 1) {
				fail("DUPLICATE_QUEST_STATE_SYNC", "a transition may synchronize quest state only once");
			}
			long dialogCloses = transition.afterCommit().stream()
				.filter(AfterCommitAction.CloseDialog.class::isInstance).count();
			if (dialogCloses > 1) {
				fail("DUPLICATE_DIALOG_CLOSE", "a transition may close the dialog only once");
			}
			boolean completionSync = stateSyncs.size() == 1
				&& stateSyncs.get(0).mode() == QuestStateSyncMode.COMPLETION;
			if (effectiveStatus == QuestStatus.COMPLETE && !completionSync && !preservesCompletedProjection) {
				fail("COMPLETE_QUEST_SYNC_REQUIRED",
					"a COMPLETE projection requires one COMPLETION quest-state sync");
			}
			if (effectiveStatus != QuestStatus.COMPLETE && completionSync) {
				fail("COMPLETE_QUEST_SYNC_STATUS_MISMATCH",
					"COMPLETION quest-state sync requires a COMPLETE projection");
			}
			for (String source : sourceLabels(definition, transition)) {
				outgoing.computeIfAbsent(source, ignored -> new HashSet<>()).add(transition.targetNode());
			}
		}
		validateCraftLifecycle(definition, nodes);
		Set<String> reached = new HashSet<>();
		java.util.ArrayDeque<String> pending = new java.util.ArrayDeque<>();
		pending.add(definition.nodes().get(0).label());
		while (!pending.isEmpty()) {
			String current = pending.removeFirst();
			if (!reached.add(current)) {
				continue;
			}
			for (String target : outgoing.getOrDefault(current, Set.of())) {
				pending.addLast(target);
			}
		}
		if (reached.size() != nodes.size()) {
			String unreachable = nodes.keySet().stream().filter(label -> !reached.contains(label)).sorted().findFirst()
				.orElse("unknown");
			fail("UNREACHABLE_NODE", "node is not reachable from the first node: " + unreachable);
		}
		validateTransitionConflicts(definition.transitions(), nodes);
		return new CompiledQuestDefinition(definition);
	}

	/**
	 * 在每条接取路由上恢复旧版 {@code QuestService.startQuest} 资格门槛。
	 * Restores the legacy {@code QuestService.startQuest} eligibility gate on every acquisition route.
	 */
	private static QuestDefinition restoreStartEligibilityContract(QuestDefinition definition) {
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		List<QuestTransition> normalized = new ArrayList<>(definition.transitions().size());
		boolean changed = false;
		for (QuestTransition transition : definition.transitions()) {
			QuestStatus targetStatus = statuses.get(transition.targetNode());
			boolean startsFromNone = transition.sourceNode() == null
				? transition.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.StatusIs status
					&& status.status() == QuestStatus.NONE)
				: statuses.get(transition.sourceNode()) == QuestStatus.NONE;
			boolean hasEligibility = transition.conditions().stream()
				.anyMatch(QuestCondition.StartEligible.class::isInstance);
			if (!startsFromNone || targetStatus == null || targetStatus == QuestStatus.NONE || hasEligibility) {
				normalized.add(transition);
				continue;
			}
			List<QuestCondition> conditions = new ArrayList<>(transition.conditions());
			conditions.add(new QuestCondition.StartEligible());
			normalized.add(new QuestTransition(transition.event(), conditions, transition.actions(),
				transition.targetNode(), transition.afterCommit(), transition.priority(), transition.sourceNode()));
			changed = true;
		}
		if (!changed) {
			return definition;
		}
		return new QuestDefinition(definition.id(), definition.version(), definition.metadata(),
			definition.progressLayout(), definition.nodes(), normalized);
	}

	/**
	 * 重复任务再次开始前恢复完整的客户端接取对话链。
	 * Restores the complete client-side acquisition dialog chain before a repeat quest starts again.
	 */
	private static QuestDefinition restoreRepeatStartDialogContract(QuestDefinition definition) {
		if (definition.metadata().repeatPolicy().maxRepeatCount() <= 1) {
			return definition;
		}
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		List<String> completeNodes = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.COMPLETE)
			.map(QuestNode::label).toList();
		if (completeNodes.isEmpty()) {
			return definition;
		}
		Set<Integer> startNpcs = definition.transitions().stream()
			.filter(transition -> statuses.get(transition.sourceNode()) == QuestStatus.NONE)
			.filter(transition -> statuses.get(transition.targetNode()) == QuestStatus.START)
			.filter(transition -> transition.conditions().stream()
				.anyMatch(QuestCondition.StartEligible.class::isInstance))
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.filter(talk -> talk.dialogId() != null && (talk.dialogId() == 1002 || talk.dialogId() == 20000))
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
		if (startNpcs.isEmpty()) {
			return definition;
		}

		List<QuestTransition> normalized = new ArrayList<>();
		for (QuestTransition transition : definition.transitions()) {
			normalized.add(transition);
			if (statuses.get(transition.sourceNode()) != QuestStatus.NONE
					|| statuses.get(transition.targetNode()) != QuestStatus.NONE
					|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)
					|| !startNpcs.contains(talk.npcId()) || talk.dialogId() == null
					|| !transition.actions().isEmpty()
					|| !isDialogOnlyResponse(transition.afterCommit())) {
				continue;
			}
			for (String complete : completeNodes) {
				if (hasDialogRoute(definition, complete, talk)) {
					continue;
				}
				List<QuestCondition> conditions = new ArrayList<>(transition.conditions());
				if (conditions.stream().noneMatch(QuestCondition.StartEligible.class::isInstance)) {
					conditions.add(new QuestCondition.StartEligible());
				}
				normalized.add(new QuestTransition(transition.event(), conditions, transition.actions(), complete,
					transition.afterCommit(), transition.priority(), complete));
			}
		}
		if (normalized.size() == definition.transitions().size()) {
			return definition;
		}
		return new QuestDefinition(definition.id(), definition.version(), definition.metadata(),
			definition.progressLayout(), definition.nodes(), normalized);
	}

	private static boolean isDialogOnlyResponse(List<AfterCommitAction> actions) {
		return actions.stream().allMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog
			|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
			|| action instanceof AfterCommitAction.ShowDialogWindow
			|| action instanceof AfterCommitAction.CloseDialog);
	}

	private static boolean hasDialogRoute(QuestDefinition definition, String source, QuestEvent.TalkToNpc event) {
		return definition.transitions().stream().anyMatch(transition -> source.equals(transition.sourceNode())
			&& QuestEvent.matches(transition.event(), event));
	}

	/**
	 * 为明确写明了奖励确认路由但省略了初始 {@code -1}/{@code 1009} 预览的定义
	 * 恢复旧版交还握手。旧任务引擎对每个 REWARD 状态终点 NPC 统一处理；
	 * 在这里降级可让 XML 与 Java DSL 所有者行为一致。
	 * Restores the legacy turn-in handshake for definitions that spell out reward
	 * confirmation routes but omit the initial {@code -1}/{@code 1009} preview.
	 * The old quest engine handled this uniformly for every REWARD-state end NPC;
	 * keeping the lowering here makes XML and Java DSL owners behave identically.
	 */
	private static QuestDefinition restoreRewardPreviewContract(QuestDefinition definition) {
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		Map<RewardPreviewKey, Set<Integer>> completionRewardIndexes = new LinkedHashMap<>();
		for (QuestTransition transition : definition.transitions()) {
			Set<String> sources = rewardSourceLabels(definition, statuses, transition);
			if (sources.isEmpty() || !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
				continue;
			}
			Integer dialogId = talk.dialogId();
			for (String source : sources) {
				RewardPreviewKey key = new RewardPreviewKey(source, talk.npcId());
				if (dialogId != null && dialogId >= 8 && dialogId <= 23
						&& transition.targetNode() != null
						&& statuses.get(transition.targetNode()) == QuestStatus.COMPLETE) {
					for (QuestAction action : transition.actions()) {
						if (action instanceof QuestAction.CompleteQuest(int rewardIndex)) {
							completionRewardIndexes.computeIfAbsent(key, ignored -> new HashSet<>())
								.add(rewardIndex);
						}
					}
				}
			}
		}

		List<QuestTransition> normalized = new ArrayList<>(definition.transitions());
		for (Map.Entry<RewardPreviewKey, Set<Integer>> entry : completionRewardIndexes.entrySet()) {
			RewardPreviewKey key = entry.getKey();
			Set<Integer> indexes = entry.getValue();
			if (indexes.size() != 1) {
				continue;
			}
			int rewardIndex = indexes.iterator().next();
			if (rewardIndex > Integer.MAX_VALUE - 5) {
				continue;
			}
			int rewardPage = 5 + rewardIndex;
			for (int dialogId : List.of(-1, 1009)) {
				if (hasRewardPreview(definition, statuses, key, dialogId)) {
					continue;
				}
				normalized.add(new QuestTransition(new QuestEvent.TalkToNpc(key.npcId(), dialogId),
					List.of(), List.of(), key.source(),
					List.of(new AfterCommitAction.ShowQuestDialog(rewardPage)), null, key.source()));
			}
		}
		if (normalized.size() == definition.transitions().size()) {
			return definition;
		}
		return new QuestDefinition(definition.id(), definition.version(), definition.metadata(),
			definition.progressLayout(), definition.nodes(), normalized);
	}

	private static Set<String> rewardSourceLabels(QuestDefinition definition, Map<String, QuestStatus> statuses,
		QuestTransition transition) {
		if (transition.sourceNode() != null) {
			return statuses.get(transition.sourceNode()) == QuestStatus.REWARD
				? Set.of(transition.sourceNode()) : Set.of();
		}
		boolean rewardBound = transition.conditions().stream()
			.anyMatch(condition -> condition instanceof QuestCondition.StatusIs status
				&& status.status() == QuestStatus.REWARD);
		if (!rewardBound) {
			return Set.of();
		}
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.REWARD)
			.map(QuestNode::label)
			.sorted()
			.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
	}

	private static boolean hasRewardPreview(QuestDefinition definition, Map<String, QuestStatus> statuses,
		RewardPreviewKey key, int dialogId) {
		return definition.transitions().stream().anyMatch(transition ->
			rewardSourceLabels(definition, statuses, transition).contains(key.source())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == key.npcId()
				&& Integer.valueOf(dialogId).equals(talk.dialogId()));
	}

	private record RewardPreviewKey(String source, int npcId) {
	}

	private static void validateProgressField(QuestDefinition definition, String field, String context) {
		if (definition.progressLayout().field(field) == null) {
			fail("UNKNOWN_PROGRESS_FIELD", context + " references unknown field: " + field);
		}
	}

	private static void validateProgressFields(QuestDefinition definition, List<String> fields, String context) {
		for (String field : fields) {
			validateProgressField(definition, field, context);
		}
	}

	private static void validateCraftLifecycle(QuestDefinition definition, Map<String, QuestNode> nodes) {
		Set<Integer> questOwnedRecipes = new HashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			for (QuestAction action : transition.actions()) {
				if (action instanceof QuestAction.LearnRecipe learn
						&& learn.ownership() == QuestRecipeOwnership.QUEST_OWNED) {
					questOwnedRecipes.add(learn.recipeId());
				}
			}
		}
		for (int recipeId : questOwnedRecipes) {
			boolean abandonCleanup = definition.metadata().cannotGiveup();
			boolean terminalCleanup = false;
			for (QuestTransition transition : definition.transitions()) {
				boolean forgetsRecipe = transition.actions().stream()
					.anyMatch(action -> action instanceof QuestAction.ForgetRecipe forget
						&& forget.recipeId() == recipeId);
				if (!forgetsRecipe) {
					continue;
				}
				boolean abandonsQuest = transition.actions().stream()
					.anyMatch(QuestAction.AbandonQuest.class::isInstance);
				if (transition.event() instanceof QuestEvent.Abandon || abandonsQuest) {
					abandonCleanup = true;
				} else {
					QuestStatus status = nodes.get(transition.targetNode()).projection().status();
					terminalCleanup |= status == QuestStatus.COMPLETE || status == QuestStatus.NONE;
				}
			}
			if (!abandonCleanup || !terminalCleanup) {
				fail("CRAFT_LIFECYCLE_INCOMPLETE", "quest-owned recipe " + recipeId
					+ " requires explicit abandon and terminal cleanup");
			}
		}
	}

	/**
	 * 返回一条转换覆盖的图源。没有显式源节点的转换在运行时是刻意的通配
	 * （规划器会用当前快照匹配其条件）。当单一状态条件约束该通配时可以
	 * 接受这种形式；有界节点集足以做可达性与冲突分析。
	 * Returns the graph sources covered by a transition.  A transition without
	 * an explicit source is a deliberate wildcard at runtime (the planner
	 * matches its conditions against the current snapshot).  It is safe to
	 * accept that form when a single status condition bounds the wildcard; the
	 * bounded node set is enough for reachability and conflict analysis.
	 */
	private static Set<String> sourceLabels(QuestDefinition definition, QuestTransition transition) {
		if (transition.sourceNode() != null) {
			return Set.of(transition.sourceNode());
		}
		Set<QuestStatus> statuses = new HashSet<>();
		for (QuestCondition condition : transition.conditions()) {
			if (condition instanceof QuestCondition.StatusIs status) {
				statuses.add(status.status());
			}
		}
		if (statuses.size() == 1) {
			QuestStatus status = statuses.iterator().next();
			Set<String> matches = definition.nodes().stream()
				.filter(node -> node.projection().status() == status)
				.map(QuestNode::label)
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
			if (!matches.isEmpty()) {
				return matches;
			}
		}
		if (definition.nodes().size() == 1) {
			return Set.of(definition.nodes().get(0).label());
		}
		fail("AMBIGUOUS_SOURCE", "transition source cannot be inferred; declare source explicitly or add one status-is condition");
		return Set.of();
	}

	private static void validateTransitionConflicts(List<QuestTransition> transitions,
			Map<String, QuestNode> nodes) {
		// 按事件具体类型分桶：QuestEvent.overlaps 仅同型（及 KillNpc/KillNpcSet 跨型）可真，
		// record 的 equals 对不同类型恒为 false，因此跨桶对不可能冲突，无需比较。
		// 6200 个任务编译时 O(T²) 全对扫描是最大热点（JFR），分桶后只比桶内与 Kill 跨桶对。
		// Bucket by concrete event type: QuestEvent.overlaps can only be true for same-type pairs
		// (plus the KillNpc/KillNpcSet cross pair); record equals is always false across types, so
		// cross-bucket pairs cannot conflict. The full O(T²) scan was the top compile hotspot (JFR).
		Map<Class<? extends QuestEvent>, List<QuestTransition>> buckets = new LinkedHashMap<>();
		for (QuestTransition transition : transitions) {
			buckets.computeIfAbsent(transition.event().getClass(), ignored -> new ArrayList<>())
				.add(transition);
		}
		List<List<QuestTransition>> killBuckets = List.of(
			buckets.getOrDefault(QuestEvent.KillNpc.class, List.of()),
			buckets.getOrDefault(QuestEvent.KillNpcSet.class, List.of()));
		for (List<QuestTransition> bucket : buckets.values()) {
			validateBucketPairwise(bucket, nodes);
		}
		// Kill 单/集合跨型配对：overlaps 是唯一可能跨类型为真的情形。
		// Kill single/set cross-bucket pairs: the only case overlaps may be true across types.
		for (int left = 0; left < killBuckets.size(); left++) {
			for (int right = left + 1; right < killBuckets.size(); right++) {
				for (QuestTransition a : killBuckets.get(left)) {
					for (QuestTransition b : killBuckets.get(right)) {
						checkConflict(a, b, nodes);
					}
				}
			}
		}
	}

	/**
	 * 对同一事件类型的 transition 两两执行冲突校验。
	 * Runs pairwise conflict checks within one event-type bucket.
	 */
	private static void validateBucketPairwise(List<QuestTransition> bucket, Map<String, QuestNode> nodes) {
		for (int left = 0; left < bucket.size(); left++) {
			QuestTransition a = bucket.get(left);
			for (int right = left + 1; right < bucket.size(); right++) {
				checkConflict(a, bucket.get(right), nodes);
			}
		}
	}

	private static void checkConflict(QuestTransition a, QuestTransition b, Map<String, QuestNode> nodes) {
		if (!QuestEvent.overlaps(a.event(), b.event())) {
			return;
		}
		if (QuestEvent.hasDeterministicPrecedence(a.event(), b.event())) {
			return;
		}
		if (sourceNodesAreMutuallyExclusive(a, b, nodes)) {
			return;
		}
		if (mutuallyExclusive(a.conditions(), b.conditions())) {
			return;
		}
		if (!a.hasExplicitPriority() || !b.hasExplicitPriority() || a.priority().equals(b.priority())) {
			fail("AMBIGUOUS_TRANSITION", "same event has overlapping transitions without unique priorities: "
				+ a.event().type());
		}
	}

	private static boolean sourceNodesAreMutuallyExclusive(QuestTransition left, QuestTransition right,
			Map<String, QuestNode> nodes) {
		List<QuestNode> leftSources = sourceCandidates(left, nodes);
		List<QuestNode> rightSources = sourceCandidates(right, nodes);
		for (QuestNode leftSource : leftSources) {
			for (QuestNode rightSource : rightSources) {
				if (!sameProjection(leftSource.projection(), rightSource.projection())) {
					continue;
				}
				if (conditionsCannotMatchNode(left.conditions(), leftSource)
						|| conditionsCannotMatchNode(right.conditions(), rightSource)) {
					continue;
				}
				return false;
			}
		}
		return true;
	}

	private static List<QuestNode> sourceCandidates(QuestTransition transition, Map<String, QuestNode> nodes) {
		if (transition.sourceNode() != null) {
			return List.of(nodes.get(transition.sourceNode()));
		}
		Set<QuestStatus> statuses = new HashSet<>();
		for (QuestCondition condition : transition.conditions()) {
			if (condition instanceof QuestCondition.StatusIs status) {
				statuses.add(status.status());
			}
		}
		if (statuses.size() == 1) {
			QuestStatus status = statuses.iterator().next();
			return nodes.values().stream().filter(node -> node.projection().status() == status).toList();
		}
		return List.copyOf(nodes.values());
	}

	private static boolean sameProjection(NodeProjection left, NodeProjection right) {
		if (left.status() != right.status()) {
			return false;
		}
		Set<String> fields = new HashSet<>(left.variables().keySet());
		fields.addAll(right.variables().keySet());
		return fields.stream().allMatch(field -> left.variables().getOrDefault(field, 0)
			.equals(right.variables().getOrDefault(field, 0)));
	}

	private static boolean conditionsCannotMatchNode(List<QuestCondition> conditions, QuestNode node) {
		for (QuestCondition condition : conditions) {
			Boolean matches = conditionMatchesNode(condition, node);
			if (Boolean.FALSE.equals(matches)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 当条件依赖实时事实而非任务投影时返回 null。
	 * Returns null when a condition depends on live facts rather than the quest projection.
	 */
	private static Boolean conditionMatchesNode(QuestCondition condition, QuestNode node) {
		NodeProjection projection = node.projection();
		if (condition instanceof QuestCondition.StatusIs status) {
			return projection.status() == status.status();
		}
		if (condition instanceof QuestCondition.QuestVariableIs variable) {
			return projection.variables().getOrDefault(variable.field(), 0) == variable.value();
		}
		if (condition instanceof QuestCondition.VariableAtLeast variable) {
			return projection.variables().getOrDefault(variable.field(), 0) >= variable.value();
		}
		if (condition instanceof QuestCondition.VariableBelow variable) {
			return projection.variables().getOrDefault(variable.field(), 0) < variable.value();
		}
		if (condition instanceof QuestCondition.VariableSumIs variable) {
			int sum = variable.fields().stream().mapToInt(field -> projection.variables().getOrDefault(field, 0)).sum();
			return sum == variable.value();
		}
		if (condition instanceof QuestCondition.VariableSumBelow variable) {
			int sum = variable.fields().stream().mapToInt(field -> projection.variables().getOrDefault(field, 0)).sum();
			return sum < variable.value();
		}
		return null;
	}

	private static boolean mutuallyExclusive(List<QuestCondition> left, List<QuestCondition> right) {
		return QuestCondition.listsAreMutuallyExclusive(left, right);
	}

	private static void fail(String code, String message) {
		throw new QuestCompilationException(code, message);
	}
}
