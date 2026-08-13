package com.aionemu.gameserver.questEngine.definition;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client page-order vs compiled-IR dialog order audit.
 * <p>
 * Reuses {@link QuestDialogOrderAudit#readClientPages} to load the active client page index, then walks every
 * external dialog entry with a BFS over IR routes exactly like {@link QuestDialogOrderAudit#audit}: each time a
 * route responds with a client page, the previous page of the same NPC route is compared against it. If the client
 * declares the new page before the previous one, the server would jump backwards in the client's reading order.
 */
public final class QuestDialogSequenceAudit {
	/**
	 * Client page ids whose intentional loops make backward page-order jumps normal: the refuse/accept pair
	 * restarts acquisition, the failed page falls back to reward windows, the reward window returns to the
	 * accept window when a repeat quest starts again, and SELECT1 is the menu page — every route back to it
	 * is a deliberate reconsider loop (e.g. "不，我再考虑一下" on equipment exchange pages).
	 */
	private static final Set<Integer> CYCLE_PAGES = Set.of(
		QuestDialogPage.SELECT1.id(), // 1011
		QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(), // 4
		QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id(), // 5
		QuestDialogPage.QUEST_REFUSE_1.id(), // 1004
		QuestDialogPage.QUEST_FAILED_1.id(), // 1009
		QuestDialogPage.QUEST_ACCEPT_1.id()); // 1003

	private static final List<String> OUTPUT_FIELDS = List.of(
		"quest_id", "source_file", "npc_id", "path",
		"prev_client_page", "prev_page_order", "next_client_page", "next_page_order", "pattern", "evidence");

	private QuestDialogSequenceAudit() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			throw new IllegalArgumentException(
				"usage: QuestDialogSequenceAudit <client-pages.csv> <client-details.csv> <output.csv>");
		}
		Path pages = Path.of(args[0]).toAbsolutePath().normalize();
		Path details = Path.of(args[1]).toAbsolutePath().normalize();
		Path output = Path.of(args[2]).toAbsolutePath().normalize();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(
			QuestDialogSequenceAudit.class.getClassLoader());
		List<Row> rows = audit(catalog, QuestDialogOrderAudit.readClientPages(pages, details));
		write(output, rows);
		Map<String, Long> counts = rows.stream().collect(java.util.stream.Collectors.groupingBy(
			Row::pattern, java.util.TreeMap::new, java.util.stream.Collectors.counting()));
		System.out.printf("rows=%d statuses=%s output=%s%n", rows.size(), counts, output);
	}

	static List<Row> audit(QuestCatalog catalog, Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests) {
		List<Row> result = new ArrayList<>();
		for (Map.Entry<Integer, QuestDialogOrderAudit.ClientQuest> entry : clientQuests.entrySet()) {
			CompiledQuestDefinition compiled = catalog.findExecutable(entry.getKey()).orElse(null);
			if (compiled == null) {
				continue;
			}
			QuestDefinition definition = compiled.definition();
			QuestDialogOrderAudit.ClientQuest client = entry.getValue();
			List<QuestTransition> dialogRoutes = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					|| transition.event() instanceof QuestEvent.QuestDialog).toList();
			Set<Integer> clientVisibleActions = client.pages().values().stream()
				.flatMap(page -> page.actions().keySet().stream())
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
			List<QuestTransition> pendingEntries = dialogRoutes.stream()
				.filter(transition -> isExternalDialogEntry(transition, client, clientVisibleActions)).toList();
			// BFS elements carry the page that led to them, so parallel branches never chain into a false sequence.
			List<Step> pending = new ArrayList<>(pendingEntries.size());
			for (QuestTransition entryTransition : pendingEntries) {
				pending.add(new Step(entryTransition, null));
			}
			Set<QuestTransition> visited = new LinkedHashSet<>();
			for (int index = 0; index < pending.size(); index++) {
				Step step = pending.get(index);
				QuestTransition trigger = step.transition();
				if (!visited.add(trigger)) {
					continue;
				}
				for (AfterCommitAction afterCommit : trigger.afterCommit()) {
					if (!(afterCommit instanceof AfterCommitAction.ShowQuestDialog shown)) {
						continue;
					}
					QuestDialogOrderAudit.ClientPage page = client.pages().get(shown.dialogId());
					if (page == null) {
						continue;
					}
					QuestDialogOrderAudit.ClientPage prev = step.previous();
					if (prev != null) {
						String path = trigger.sourceNode() + " + NPC " + ownerNpc(trigger.event()) + " + "
							+ dialogAction(trigger.event()) + " -> " + trigger.targetNode()
							+ " + page " + shown.dialogId();
						String pattern;
						if (prev.pageOrder() > page.pageOrder()) {
							pattern = CYCLE_PAGES.contains(prev.pageId()) || CYCLE_PAGES.contains(page.pageId())
								? "CYCLE_NORMAL" : "ORDER_VIOLATION";
						} else {
							pattern = "FORWARD";
						}
						result.add(new Row(definition.id(), client.sourceFile(),
							Integer.toString(ownerNpc(trigger.event())), path,
							prev.pageName(), prev.pageOrder(), page.pageName(), page.pageOrder(), pattern,
							page.evidence() + " | previous: " + prev.evidence()));
					}
					for (QuestDialogOrderAudit.ClientAction action : page.actions().values()) {
						List<QuestTransition> candidates = dialogRoutes.stream().filter(candidate ->
							sameDialogOwner(trigger.event(), candidate.event())
								&& dialogAction(candidate.event()) == action.actionId()
								&& startsFromNode(candidate, trigger.targetNode(), definition)).toList();
						for (QuestTransition candidate : candidates) {
							pending.add(new Step(candidate, page));
						}
					}
				}
			}
		}
		result.sort(Comparator.comparingInt(Row::questId)
			.thenComparing(Row::sourceFile)
			.thenComparing(Row::pattern)
			.thenComparingInt(Row::prevPageOrder)
			.thenComparingInt(Row::nextPageOrder)
			.thenComparing(Row::path));
		return List.copyOf(result);
	}

	private static boolean isExternalDialogEntry(QuestTransition transition,
			QuestDialogOrderAudit.ClientQuest client, Set<Integer> clientVisibleActions) {
		if (transition.event() instanceof QuestEvent.QuestDialog) {
			return true;
		}
		QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
		if (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
				|| talk.dialogId() == QuestDialogAction.USE_OBJECT.id()) {
			return true;
		}
		return !clientVisibleActions.contains(talk.dialogId()) && transition.afterCommit().stream()
			.filter(AfterCommitAction.ShowQuestDialog.class::isInstance)
			.map(AfterCommitAction.ShowQuestDialog.class::cast)
			.anyMatch(shown -> client.pages().containsKey(shown.dialogId()));
	}

	private static boolean startsFromNode(QuestTransition transition, String nodeLabel,
			QuestDefinition definition) {
		if (transition.sourceNode() == null) {
			return true;
		}
		if (transition.sourceNode().equals(nodeLabel)) {
			return true;
		}
		QuestNode actual = node(definition, nodeLabel);
		QuestNode declared = node(definition, transition.sourceNode());
		return definition.metadata().repeatPolicy().maxRepeatCount() > 1
			&& actual.projection().status() == com.aionemu.gameserver.questEngine.model.QuestStatus.COMPLETE
			&& declared.projection().status() == com.aionemu.gameserver.questEngine.model.QuestStatus.NONE
			&& transition.conditions().stream().anyMatch(QuestCondition.StartEligible.class::isInstance);
	}

	private static boolean sameDialogOwner(QuestEvent left, QuestEvent right) {
		if (left instanceof QuestEvent.TalkToNpc leftTalk && right instanceof QuestEvent.TalkToNpc rightTalk) {
			return leftTalk.npcId() == rightTalk.npcId();
		}
		return left instanceof QuestEvent.QuestDialog && right instanceof QuestEvent.QuestDialog;
	}

	private static int dialogAction(QuestEvent event) {
		return switch (event) {
			case QuestEvent.TalkToNpc talk -> talk.dialogId();
			case QuestEvent.QuestDialog dialog -> dialog.dialogId();
			default -> throw new IllegalArgumentException("not a dialog event: " + event);
		};
	}

	private static int ownerNpc(QuestEvent event) {
		return event instanceof QuestEvent.TalkToNpc talk ? talk.npcId() : 0;
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow(() -> new IllegalArgumentException("unknown quest node " + label));
	}

	static void write(Path output, List<Row> rows) throws IOException {
		Files.createDirectories(output.getParent());
		try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
			writer.write('﻿');
			writeCsvRow(writer, OUTPUT_FIELDS);
			for (Row row : rows) {
				writeCsvRow(writer, List.of(Integer.toString(row.questId()), row.sourceFile(), row.npcId(), row.path(),
					row.prevClientPage(), Integer.toString(row.prevPageOrder()), row.nextClientPage(),
					Integer.toString(row.nextPageOrder()), row.pattern(), row.evidence()));
			}
		}
	}

	private static void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
		for (int index = 0; index < values.size(); index++) {
			if (index != 0) {
				writer.write(',');
			}
			String value = values.get(index);
			if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
				writer.write('"');
				writer.write(value.replace("\"", "\"\""));
				writer.write('"');
			} else {
				writer.write(value);
			}
		}
		writer.newLine();
	}

	record Row(int questId, String sourceFile, String npcId, String path, String prevClientPage,
			int prevPageOrder, String nextClientPage, int nextPageOrder, String pattern, String evidence) {
	}

	private record Step(QuestTransition transition, QuestDialogOrderAudit.ClientPage previous) {
	}
}
