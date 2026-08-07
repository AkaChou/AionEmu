package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lowers strict XML authoring blocks to the canonical transition IR. */
final class QuestXmlBlockExpander {
	private QuestXmlBlockExpander() {
	}

	static List<QuestTransition> expand(int questId, QuestMetadata metadata, ProgressLayout progress,
			List<QuestNode> nodes, Element transitionsElement) {
		if (transitionsElement == null) {
			return List.of();
		}
		Context context = new Context(questId, metadata, progress, nodes);
		List<QuestTransition> transitions = new ArrayList<>();
		for (Element element : children(transitionsElement)) {
			switch (element.getTagName()) {
				case "transition" -> transitions.addAll(QuestDefinitionXmlCompiler.parseTransition(element));
				case "npc-start" -> transitions.addAll(expandNpcStart(context, element));
				case "counter" -> transitions.addAll(expandCounter(context, element));
				case "kill-chain" -> transitions.addAll(expandKillChain(context, element));
				case "npc-complete" -> transitions.addAll(expandNpcComplete(context, element));
				default -> fail("UNKNOWN_XML_BLOCK", context, element.getTagName(), "element",
					"unsupported transitions child");
			}
		}
		return List.copyOf(transitions);
	}

	private static List<QuestTransition> expandNpcStart(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		QuestNode sourceNode = requireNode(context, "npc-start", "source", source);
		QuestNode targetNode = requireNode(context, "npc-start", "target", target);
		if (sourceNode.projection().status() != QuestStatus.NONE) {
			fail("NPC_START_SOURCE_STATUS", context, "npc-start", "source",
				"node " + source + " must project NONE");
		}
		if (targetNode.projection().status() != QuestStatus.START) {
			fail("NPC_START_TARGET_STATUS", context, "npc-start", "target",
				"node " + target + " must project START");
		}
		int npcId = positiveInteger(context, block, "npc-start", "npc-id");
		List<String> selectionSources = block.hasAttribute("selection-sources")
			? tokens(context, block, "npc-start", "selection-sources", true) : List.of();
		for (String selectionSource : selectionSources) {
			requireNode(context, "npc-start", "selection-sources", selectionSource);
		}

		List<QuestAction> acceptActions = new ArrayList<>();
		Element actionsElement = child(block, "accept-actions");
		if (actionsElement != null) {
			for (Element action : children(actionsElement)) {
				try {
					acceptActions.add(QuestDefinitionXmlCompiler.parseAction(action));
				} catch (RuntimeException e) {
					fail("NPC_START_ACCEPT_ACTION_INVALID", context, "npc-start", "accept-actions",
						action.getTagName() + ": " + e.getMessage());
				}
			}
		}

		List<QuestTransition> result = new ArrayList<>();
		result.add(talk(npcId, 31, List.of(), List.of(), source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(1011))));
		result.add(talk(npcId, 1007, List.of(), List.of(), source, source, null,
			List.of(new AfterCommitAction.ShowQuestDialog(4))));
		List<QuestCondition> acceptConditions = List.of(new QuestCondition.StartEligible());
		result.add(talk(npcId, 1002, acceptConditions, acceptActions, source, target, null,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(1003))));
		result.add(talk(npcId, 20000, acceptConditions, acceptActions, source, target, null,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog())));
		for (int dialogId : List.of(1003, 1004, 20001)) {
			result.add(talk(npcId, dialogId, List.of(), List.of(), source, source, null,
				List.of(new AfterCommitAction.CloseDialog())));
		}
		for (String selectionSource : selectionSources) {
			result.add(talk(npcId, 1008, List.of(), List.of(), selectionSource, selectionSource, null,
				List.of(new AfterCommitAction.ShowQuestSelectionDialog(10))));
		}
		return result;
	}

	private static List<QuestTransition> expandCounter(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		String fieldName = attribute(block, "field");
		QuestNode sourceNode = requireNode(context, "counter", "source", source);
		QuestNode targetNode = requireNode(context, "counter", "target", target);
		BitField field = context.progress().field(fieldName);
		if (field == null) {
			fail("COUNTER_UNKNOWN_FIELD", context, "counter", "field", "unknown progress field " + fieldName);
		}
		int required = integer(context, block, "counter", "required");
		if (required < 1) {
			fail("COUNTER_INVALID_REQUIRED", context, "counter", "required", "must be at least 1");
		}
		if (required > field.maxValue()) {
			fail("COUNTER_FIELD_TOO_NARROW", context, "counter", "required",
				"value " + required + " exceeds " + fieldName + " max " + field.maxValue());
		}
		if (required - 1 < field.minValue()) {
			fail("COUNTER_FIELD_RANGE_INVALID", context, "counter", "required",
				"value before completion is below " + fieldName + " min " + field.minValue());
		}
		if (sourceNode.projection().variables().containsKey(fieldName)) {
			fail("COUNTER_SOURCE_PROJECTION_CONFLICT", context, "counter", "source",
				"node " + source + " fixes counter field " + fieldName);
		}
		Integer targetValue = targetNode.projection().variables().get(fieldName);
		if (targetValue != null && targetValue != required) {
			fail("COUNTER_TARGET_PROJECTION_CONFLICT", context, "counter", "target",
				"node " + target + " projects " + fieldName + "=" + targetValue
					+ " instead of " + required);
		}

		Element eventContainer = child(block, "event");
		List<QuestEvent> events;
		try {
			events = QuestDefinitionXmlCompiler.parseEvents(onlyChild(eventContainer));
		} catch (RuntimeException e) {
			return fail("COUNTER_EVENT_INVALID", context, "counter", "event", e.getMessage());
		}
		List<QuestCondition> sharedConditions = new ArrayList<>();
		Element conditionsElement = child(block, "conditions");
		if (conditionsElement != null) {
			for (Element condition : children(conditionsElement)) {
				try {
					sharedConditions.add(QuestDefinitionXmlCompiler.parseCondition(condition));
				} catch (RuntimeException e) {
					return fail("COUNTER_CONDITION_INVALID", context, "counter", "conditions", e.getMessage());
				}
			}
		}

		List<QuestTransition> result = new ArrayList<>();
		for (QuestEvent event : events) {
			List<QuestCondition> continuing = new ArrayList<>(sharedConditions);
			continuing.add(new QuestCondition.VariableBelow(fieldName, required - 1));
			result.add(new QuestTransition(event, continuing,
				List.of(new QuestAction.IncrementVariable(fieldName, 1)), source,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), 1, source));

			List<QuestCondition> completing = new ArrayList<>(sharedConditions);
			completing.add(new QuestCondition.QuestVariableIs(fieldName, required - 1));
			result.add(new QuestTransition(event, completing,
				List.of(new QuestAction.IncrementVariable(fieldName, 1)), target,
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), 0, source));
		}
		return result;
	}

	private static List<QuestTransition> expandKillChain(Context context, Element block) {
		List<String> nodes = tokens(context, block, "kill-chain", "nodes", true);
		if (nodes.size() < 3) {
			return fail("KILL_CHAIN_TOO_SHORT", context, "kill-chain", "nodes",
				"must contain at least three nodes");
		}
		Set<String> uniqueNodes = new LinkedHashSet<>();
		for (String node : nodes) {
			requireNode(context, "kill-chain", "nodes", node);
			if (!uniqueNodes.add(node)) {
				return fail("KILL_CHAIN_DUPLICATE_NODE", context, "kill-chain", "nodes",
					"duplicate node " + node);
			}
		}

		List<QuestEvent> events;
		try {
			events = QuestDefinitionXmlCompiler.parseEvents(onlyChild(child(block, "event")));
		} catch (RuntimeException e) {
			return fail("KILL_CHAIN_EVENT_INVALID", context, "kill-chain", "event", e.getMessage());
		}
		if (events.stream().anyMatch(event -> !(event instanceof QuestEvent.KillNpc)
				&& !(event instanceof QuestEvent.KillNpcSet))) {
			return fail("KILL_CHAIN_EVENT_TYPE", context, "kill-chain", "event",
				"must be kill-npc");
		}

		List<QuestCondition> conditions = new ArrayList<>();
		Element conditionsElement = child(block, "conditions");
		if (conditionsElement != null) {
			for (Element condition : children(conditionsElement)) {
				try {
					conditions.add(QuestDefinitionXmlCompiler.parseCondition(condition));
				} catch (RuntimeException e) {
					return fail("KILL_CHAIN_CONDITION_INVALID", context, "kill-chain", "conditions", e.getMessage());
				}
			}
		}

		List<QuestTransition> result = new ArrayList<>();
		List<AfterCommitAction> afterCommit = List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
		for (int index = 0; index < nodes.size() - 1; index++) {
			for (QuestEvent event : events) {
				result.add(new QuestTransition(event, conditions, List.of(), nodes.get(index + 1),
					afterCommit, null, nodes.get(index)));
			}
		}
		return result;
	}

	private static List<QuestTransition> expandNpcComplete(Context context, Element block) {
		String source = attribute(block, "source");
		String target = attribute(block, "target");
		QuestNode sourceNode = requireNode(context, "npc-complete", "source", source);
		QuestNode targetNode = requireNode(context, "npc-complete", "target", target);
		if (sourceNode.projection().status() != QuestStatus.REWARD) {
			fail("NPC_COMPLETE_SOURCE_STATUS", context, "npc-complete", "source",
				"node " + source + " must project REWARD");
		}
		if (targetNode.projection().status() != QuestStatus.COMPLETE) {
			fail("NPC_COMPLETE_TARGET_STATUS", context, "npc-complete", "target",
				"node " + target + " must project COMPLETE");
		}
		int npcId = positiveInteger(context, block, "npc-complete", "npc-id");
		int completeRewardIndex = integer(context, block, "npc-complete", "complete-reward-index");
		if (completeRewardIndex < 0) {
			fail("NPC_COMPLETE_INVALID_COMPLETE_REWARD_INDEX", context, "npc-complete",
				"complete-reward-index", "must be non-negative");
		}

		List<Integer> fixedRewardIndices = block.hasAttribute("fixed-reward-indices")
			? integerTokens(context, block, "npc-complete", "fixed-reward-indices", false) : List.of();
		if (new LinkedHashSet<>(fixedRewardIndices).size() != fixedRewardIndices.size()) {
			fail("NPC_COMPLETE_DUPLICATE_REWARD_INDEX", context, "npc-complete", "fixed-reward-indices",
				"contains duplicate indices");
		}
		List<QuestAction> fixedRewards = new ArrayList<>();
		for (int rewardIndex : fixedRewardIndices) {
			QuestReward reward = reward(context, "fixed-reward-indices", rewardIndex);
			if (rewardKind(context, "fixed-reward-indices", rewardIndex, reward) == QuestRewardKind.SELECTABLE_ITEM) {
				fail("NPC_COMPLETE_FIXED_REWARD_TYPE", context, "npc-complete", "fixed-reward-indices",
					"reward index " + rewardIndex + " is SELECTABLE_ITEM");
			}
			fixedRewards.add(rewardAction(context, "fixed-reward-indices", rewardIndex, reward));
		}

		DialogIds dialogs = new DialogIds(context, "npc-complete");
		List<Integer> previewDialogIds = dialogs.add(block, "preview-dialog-ids");
		List<CompletionRoute> routes = new ArrayList<>();
		if (block.hasAttribute("dialog-ids")) {
			for (int dialogId : dialogs.add(block, "dialog-ids")) {
				routes.add(new CompletionRoute(dialogId, null));
			}
		}
		for (Element choice : children(block, "choice")) {
			int dialogId = integer(context, choice, "npc-complete", "dialog-id");
			dialogs.addSingle(dialogId, "choice.dialog-id");
			int rewardIndex = integer(context, choice, "npc-complete", "reward-index");
			QuestReward reward = reward(context, "choice.reward-index", rewardIndex);
			if (rewardKind(context, "choice.reward-index", rewardIndex, reward) != QuestRewardKind.SELECTABLE_ITEM) {
				fail("NPC_COMPLETE_CHOICE_REWARD_TYPE", context, "npc-complete", "choice.reward-index",
					"reward index " + rewardIndex + " is not SELECTABLE_ITEM");
			}
			routes.add(new CompletionRoute(dialogId,
				rewardAction(context, "choice.reward-index", rewardIndex, reward)));
		}
		Element fallback = child(block, "fallback");
		if (fallback != null) {
			for (int dialogId : dialogs.add(fallback, "dialog-ids")) {
				routes.add(new CompletionRoute(dialogId, null));
			}
		}
		if (routes.isEmpty()) {
			fail("NPC_COMPLETE_NO_COMPLETION_ROUTE", context, "npc-complete", "dialog-ids",
				"declare dialog-ids, choice, or fallback");
		}
		Finish finish;
		try {
			finish = Finish.valueOf(attribute(block, "finish"));
		} catch (IllegalArgumentException e) {
			return fail("NPC_COMPLETE_INVALID_FINISH", context, "npc-complete", "finish",
				"must be SELECTION_DIALOG, CLOSE_DIALOG, or NONE");
		}

		List<QuestTransition> result = new ArrayList<>();
		for (int dialogId : previewDialogIds) {
			result.add(talk(npcId, dialogId, List.of(), List.of(), source, source, null,
				List.of(new AfterCommitAction.ShowQuestDialog(5))));
		}
		for (CompletionRoute route : routes) {
			List<QuestAction> actions = new ArrayList<>(fixedRewards);
			if (route.choiceReward() != null) {
				actions.add(route.choiceReward());
			}
			actions.add(new QuestAction.CompleteQuest(completeRewardIndex));
			List<AfterCommitAction> afterCommit = new ArrayList<>();
			afterCommit.add(new AfterCommitAction.RefreshPlayerStats());
			afterCommit.add(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION));
			switch (finish) {
				case SELECTION_DIALOG -> afterCommit.add(new AfterCommitAction.ShowQuestSelectionDialog(10));
				case CLOSE_DIALOG -> afterCommit.add(new AfterCommitAction.CloseDialog());
				case NONE -> {
				}
			}
			result.add(talk(npcId, route.dialogId(), List.of(), actions, source, target, null, afterCommit));
		}
		return result;
	}

	private static QuestReward reward(Context context, String attribute, int rewardIndex) {
		if (rewardIndex < 0 || rewardIndex >= context.metadata().rewards().size()) {
			return fail("NPC_COMPLETE_REWARD_INDEX_OUT_OF_RANGE", context, "npc-complete", attribute,
				"reward index " + rewardIndex + " is not present in metadata");
		}
		return context.metadata().rewards().get(rewardIndex);
	}

	private static QuestRewardKind rewardKind(Context context, String attribute, int rewardIndex,
			QuestReward reward) {
		try {
			return QuestRewardKind.fromWire(reward.kind());
		} catch (IllegalArgumentException e) {
			return fail("NPC_COMPLETE_REWARD_TYPE_INVALID", context, "npc-complete", attribute,
				"reward index " + rewardIndex + " has unsupported kind " + reward.kind());
		}
	}

	private static QuestAction rewardAction(Context context, String attribute, int rewardIndex,
			QuestReward reward) {
		QuestRewardKind kind = rewardKind(context, attribute, rewardIndex, reward);
		QuestRewardKind actionKind = kind == QuestRewardKind.SELECTABLE_ITEM ? QuestRewardKind.ITEM : kind;
		QuestRewardAmountMode amountMode = switch (actionKind) {
			case GOLD, KINAH, EXP, AP, GP -> QuestRewardAmountMode.QUEST_BASE;
			default -> QuestRewardAmountMode.EXACT;
		};
		return new QuestAction.GrantReward(actionKind.name(), reward.id(), reward.amount(), amountMode);
	}

	private static QuestTransition talk(int npcId, int dialogId, List<QuestCondition> conditions,
			List<QuestAction> actions, String source, String target, Integer priority,
			List<AfterCommitAction> afterCommit) {
		return new QuestTransition(new QuestEvent.TalkToNpc(npcId, dialogId), conditions, actions, target,
			afterCommit, priority, source);
	}

	private static QuestNode requireNode(Context context, String block, String attribute, String label) {
		QuestNode node = context.nodes().get(label);
		if (node == null) {
			return fail("XML_BLOCK_BAD_NODE_REFERENCE", context, block, attribute, "unknown node " + label);
		}
		return node;
	}

	private static int positiveInteger(Context context, Element element, String block, String attribute) {
		int value = integer(context, element, block, attribute);
		if (value <= 0) {
			return fail("XML_BLOCK_INVALID_POSITIVE_INTEGER", context, block, attribute, "must be positive");
		}
		return value;
	}

	private static int integer(Context context, Element element, String block, String attribute) {
		try {
			return Integer.parseInt(attribute(element, attribute));
		} catch (NumberFormatException e) {
			return fail("XML_BLOCK_INVALID_INTEGER", context, block, attribute, "must be an integer");
		}
	}

	private static List<Integer> integerTokens(Context context, Element element, String block,
			String attribute, boolean required) {
		List<String> values = tokens(context, element, block, attribute, required);
		List<Integer> result = new ArrayList<>(values.size());
		for (String value : values) {
			try {
				result.add(Integer.parseInt(value));
			} catch (NumberFormatException e) {
				return fail("XML_BLOCK_INVALID_INTEGER_SET", context, block, attribute,
					"contains non-integer " + value);
			}
		}
		return List.copyOf(result);
	}

	private static List<String> tokens(Context context, Element element, String block,
			String attribute, boolean required) {
		String raw = attribute(element, attribute).trim();
		if (raw.isEmpty()) {
			if (required) {
				return fail("XML_BLOCK_EMPTY_ATTRIBUTE", context, block, attribute, "must not be empty");
			}
			return List.of();
		}
		return List.of(raw.split("\\s+"));
	}

	private static String attribute(Element element, String name) {
		return element.getAttribute(name);
	}

	private static Element child(Element parent, String name) {
		if (parent == null) {
			return null;
		}
		for (Element element : children(parent)) {
			if (name.equals(element.getTagName())) {
				return element;
			}
		}
		return null;
	}

	private static List<Element> children(Element parent) {
		return children(parent, null);
	}

	private static List<Element> children(Element parent, String name) {
		List<Element> result = new ArrayList<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element element && (name == null || name.equals(element.getTagName()))) {
				result.add(element);
			}
		}
		return result;
	}

	private static Element onlyChild(Element parent) {
		if (parent == null) {
			throw new IllegalArgumentException("missing event");
		}
		List<Element> children = children(parent);
		if (children.size() != 1) {
			throw new IllegalArgumentException("event must contain exactly one child");
		}
		return children.getFirst();
	}

	private static <T> T fail(String code, Context context, String block, String attribute, String detail) {
		throw new QuestCompilationException(code, "quest " + context.questId() + " " + block
			+ " attribute '" + attribute + "': " + detail);
	}

	private record Context(int questId, QuestMetadata metadata, ProgressLayout progress,
			Map<String, QuestNode> nodes) {
		private Context(int questId, QuestMetadata metadata, ProgressLayout progress, List<QuestNode> nodes) {
			this(questId, metadata, progress, index(nodes));
		}

		private static Map<String, QuestNode> index(List<QuestNode> nodes) {
			Map<String, QuestNode> result = new LinkedHashMap<>();
			for (QuestNode node : nodes) {
				result.putIfAbsent(node.label(), node);
			}
			return Map.copyOf(result);
		}
	}

	private record CompletionRoute(int dialogId, QuestAction choiceReward) {
	}

	private enum Finish {
		SELECTION_DIALOG,
		CLOSE_DIALOG,
		NONE
	}

	private static final class DialogIds {
		private final Context context;
		private final String block;
		private final Set<Integer> seen = new LinkedHashSet<>();

		private DialogIds(Context context, String block) {
			this.context = context;
			this.block = block;
		}

		private List<Integer> add(Element element, String attribute) {
			String raw = QuestXmlBlockExpander.attribute(element, attribute).trim();
			if (raw.isEmpty()) {
				return fail("NPC_COMPLETE_EMPTY_DIALOG_SET", context, block, attribute, "must not be empty");
			}
			List<Integer> result = new ArrayList<>();
			for (String token : raw.split("[\\s,]+")) {
				int delimiter = token.indexOf("..");
				if (delimiter < 0) {
					int dialogId = parse(token, attribute);
					addSingle(dialogId, attribute);
					result.add(dialogId);
					continue;
				}
				if (delimiter == 0 || delimiter + 2 == token.length()
						|| token.indexOf("..", delimiter + 2) >= 0) {
					return fail("NPC_COMPLETE_INVALID_DIALOG_SET", context, block, attribute, token);
				}
				int first = parse(token.substring(0, delimiter), attribute);
				int last = parse(token.substring(delimiter + 2), attribute);
				if (first > last || (long) last - first >= 256) {
					return fail("NPC_COMPLETE_INVALID_DIALOG_RANGE", context, block, attribute, token);
				}
				for (int dialogId = first; ; dialogId++) {
					addSingle(dialogId, attribute);
					result.add(dialogId);
					if (dialogId == last) {
						break;
					}
				}
			}
			if (result.size() > 256) {
				return fail("NPC_COMPLETE_TOO_MANY_DIALOG_IDS", context, block, attribute,
					"must contain at most 256 ids");
			}
			return List.copyOf(result);
		}

		private int parse(String token, String attribute) {
			try {
				return Integer.parseInt(token);
			} catch (NumberFormatException e) {
				return fail("NPC_COMPLETE_INVALID_DIALOG_SET", context, block, attribute, token);
			}
		}

		private void addSingle(int dialogId, String attribute) {
			if (!seen.add(dialogId)) {
				fail("NPC_COMPLETE_DUPLICATE_DIALOG_ID", context, block, attribute,
					"dialog id " + dialogId + " is declared more than once");
			}
		}
	}
}
