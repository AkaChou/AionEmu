package com.aionemu.gameserver.questEngine.definition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reproducible client-page to compiled-IR dialog order audit. */
public final class QuestDialogOrderAudit {
	private static final Set<Integer> FIXED_EQUIPMENT_EXCHANGES = Set.of(
		1993, 1994, 2993, 2994, 80292, 80293, 80296, 80297);
	private static final List<String> OUTPUT_FIELDS = List.of(
		"quest_id", "source_file", "server_source_state", "npc_id", "trigger_action",
		"actual_path", "shown_page", "client_visible_action", "client_expected",
		"evidence_source", "audit_status", "fix_status", "unresolved_reason");

	private QuestDialogOrderAudit() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("usage: QuestDialogOrderAudit <client-details.csv> <output.csv>");
		}
		Path details = Path.of(args[0]).toAbsolutePath().normalize();
		Path output = Path.of(args[1]).toAbsolutePath().normalize();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(
			QuestDialogOrderAudit.class.getClassLoader());
		List<AuditRow> rows = audit(catalog, readClientPages(details));
		write(output, rows);
		long unresolved = rows.stream().filter(row -> row.auditStatus().equals("UNRESOLVED")).count();
		long unreached = rows.stream().filter(row -> row.auditStatus().equals("UNREACHED")).count();
		long verified = rows.stream().filter(row -> row.auditStatus().equals("VERIFIED")).count();
		System.out.printf("rows=%d verified=%d unresolved=%d unreached=%d output=%s%n",
			rows.size(), verified, unresolved, unreached, output);
	}

	static List<AuditRow> audit(QuestCatalog catalog, Map<Integer, ClientQuest> clientQuests) {
		List<AuditRow> result = new ArrayList<>();
		Set<Integer> taskHtmlPageIds = clientQuests.values().stream()
			.flatMap(client -> client.pages().keySet().stream())
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
		for (Map.Entry<Integer, ClientQuest> entry : clientQuests.entrySet()) {
			CompiledQuestDefinition compiled = catalog.findExecutable(entry.getKey()).orElse(null);
			if (compiled == null) {
				continue;
			}
			QuestDefinition definition = compiled.definition();
			ClientQuest client = entry.getValue();
			List<QuestTransition> dialogRoutes = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					|| transition.event() instanceof QuestEvent.QuestDialog).toList();
			Set<Integer> shownPages = new LinkedHashSet<>();
			for (QuestTransition trigger : dialogRoutes) {
				for (AfterCommitAction afterCommit : trigger.afterCommit()) {
					if (!(afterCommit instanceof AfterCommitAction.ShowQuestDialog shown)) {
						continue;
					}
					shownPages.add(shown.dialogId());
					ClientPage page = client.pages().get(shown.dialogId());
					if (page == null) {
						if (isKnownGenericPage(shown.dialogId(), taskHtmlPageIds)) {
							continue;
						}
						result.add(new AuditRow(definition.id(), client.sourceFile(),
							nullToEmpty(trigger.sourceNode()), ownerNpc(trigger.event()),
							Integer.toString(dialogAction(trigger.event())),
							trigger.sourceNode() + " + " + dialogOwner(trigger.event()) + " + "
								+ dialogAction(trigger.event()) + " -> " + trigger.targetNode()
								+ " + page " + shown.dialogId(), Integer.toString(shown.dialogId()), "",
							"shown server page must exist in the active client HTML action map",
							client.sourceFile(), "UNRESOLVED", "UNRESOLVED",
							"compiled IR emits a task page absent from the active client details"));
						continue;
					}
					for (ClientAction action : page.actions().values()) {
						boolean routeExists = dialogRoutes.stream().anyMatch(candidate ->
							sameDialogOwner(trigger.event(), candidate.event())
								&& dialogAction(candidate.event()) == action.actionId()
								&& startsFromNode(candidate, trigger.targetNode(), definition));
						result.add(row(definition.id(), client.sourceFile(), trigger, shown.dialogId(),
							action, routeExists));
					}
				}
			}
			for (ClientPage page : client.pages().values()) {
				if (!page.actions().isEmpty() && !shownPages.contains(page.pageId())) {
					result.add(new AuditRow(definition.id(), client.sourceFile(), "", "", "",
						"no compiled transition emits this active client page", Integer.toString(page.pageId()),
						joinActions(page.actions().keySet()), "active client page must be emitted before its buttons can be used",
						page.evidence(), "UNREACHED", "UNRESOLVED",
						"active page is absent from compiled IR responses; no current path identifies its NPC or state"));
				}
			}
		}
		result = new ArrayList<>(new LinkedHashSet<>(result));
		result.sort(Comparator.comparingInt(AuditRow::questId)
			.thenComparing(AuditRow::sourceFile)
			.thenComparing(AuditRow::shownPage)
			.thenComparing(AuditRow::clientVisibleAction)
			.thenComparing(AuditRow::actualPath));
		return List.copyOf(result);
	}

	private static boolean isKnownGenericPage(int pageId, Set<Integer> taskHtmlPageIds) {
		if (pageId == QuestDialogPage.QUEST_FAILED_1.id()) {
			return true;
		}
		try {
			QuestDialogPage.fromId(pageId);
			return !taskHtmlPageIds.contains(pageId);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static AuditRow row(int questId, String sourceFile, QuestTransition trigger,
			int shownPage, ClientAction clientAction, boolean routeExists) {
		String owner = dialogOwner(trigger.event());
		String triggerAction = Integer.toString(dialogAction(trigger.event()));
		String path = trigger.sourceNode() + " + " + owner + " + " + triggerAction + " -> "
			+ trigger.targetNode() + " + page " + shownPage;
		String expected = trigger.targetNode() + " + " + owner + " + " + clientAction.actionId()
			+ " must have a compiled route";
		boolean fixedExchange = FIXED_EQUIPMENT_EXCHANGES.contains(questId);
		String evidence = clientAction.evidence();
		if (fixedExchange) {
			evidence += " | origin/history daevanion handler for quest " + questId
				+ " | quest_data.xml reward groups";
		}
		return new AuditRow(questId, sourceFile, nullToEmpty(trigger.sourceNode()), ownerNpc(trigger.event()),
			triggerAction, path, Integer.toString(shownPage), Integer.toString(clientAction.actionId()),
			expected, evidence, routeExists ? "VERIFIED" : "UNRESOLVED",
			routeExists ? fixedExchange ? "FIXED" : "NOT_NEEDED" : "UNRESOLVED",
			routeExists ? "" : "visible client action has no route; client does not prove its response page or state side effect");
	}

	private static boolean startsFromNode(QuestTransition transition, String nodeLabel,
			QuestDefinition definition) {
		if (Objects.equals(transition.sourceNode(), nodeLabel)) {
			return true;
		}
		if (transition.sourceNode() != null) {
			return false;
		}
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(nodeLabel)).findFirst().orElseThrow();
		for (QuestCondition condition : transition.conditions()) {
			Boolean matches = conditionMatchesNode(condition, node);
			if (Boolean.FALSE.equals(matches)) {
				return false;
			}
		}
		return true;
	}

	/** Only quest status and packed variables constrain a generic route's source node. */
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
			int sum = variable.fields().stream()
				.mapToInt(field -> projection.variables().getOrDefault(field, 0)).sum();
			return sum == variable.value();
		}
		if (condition instanceof QuestCondition.VariableSumBelow variable) {
			int sum = variable.fields().stream()
				.mapToInt(field -> projection.variables().getOrDefault(field, 0)).sum();
			return sum < variable.value();
		}
		return null;
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

	private static String dialogOwner(QuestEvent event) {
		return event instanceof QuestEvent.TalkToNpc talk ? "NPC " + talk.npcId() : "QUEST_ACTION";
	}

	private static String ownerNpc(QuestEvent event) {
		return event instanceof QuestEvent.TalkToNpc talk ? Integer.toString(talk.npcId()) : "";
	}

	private static String joinActions(Collection<Integer> actions) {
		return actions.stream().sorted().map(String::valueOf)
			.collect(java.util.stream.Collectors.joining(" "));
	}

	static Map<Integer, ClientQuest> readClientPages(Path details) throws IOException {
		Map<Integer, MutableClientQuest> quests = new LinkedHashMap<>();
		try (BufferedReader reader = Files.newBufferedReader(details, StandardCharsets.UTF_8)) {
			List<String> header = parseCsvLine(stripBom(reader.readLine()));
			Map<String, Integer> columns = new HashMap<>();
			for (int index = 0; index < header.size(); index++) {
				columns.put(header.get(index), index);
			}
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> values = parseCsvLine(line);
				if (!"active".equals(value(values, columns, "source_variant"))
						|| !"exact".equals(value(values, columns, "page_mapping"))
						|| !"exact".equals(value(values, columns, "action_mapping"))) {
					continue;
				}
				int questId = Integer.parseInt(value(values, columns, "quest_id"));
				int pageId = Integer.parseInt(value(values, columns, "page_id"));
				int actionId = Integer.parseInt(value(values, columns, "action_id"));
				String sourceFile = value(values, columns, "source_file");
				String pageName = value(values, columns, "html_page_name");
				String buttonText = value(values, columns, "button_text_zh");
				MutableClientQuest quest = quests.computeIfAbsent(questId,
					ignored -> new MutableClientQuest(sourceFile));
				quest.sourceFiles.add(sourceFile);
				MutableClientPage page = quest.pages.computeIfAbsent(pageId,
					ignored -> new MutableClientPage(pageName));
				page.evidence.add(sourceFile + "#" + pageName);
				page.actions.putIfAbsent(actionId,
					new ClientAction(actionId, sourceFile + "#" + pageName + ": " + buttonText));
			}
		}
		Map<Integer, ClientQuest> result = new LinkedHashMap<>();
		quests.forEach((questId, quest) -> {
			Map<Integer, ClientPage> pages = new LinkedHashMap<>();
			quest.pages.forEach((pageId, page) -> pages.put(pageId,
				new ClientPage(pageId, page.pageName, Map.copyOf(page.actions), String.join(" | ", page.evidence))));
			result.put(questId, new ClientQuest(String.join(" | ", quest.sourceFiles), Map.copyOf(pages)));
		});
		return Map.copyOf(result);
	}

	private static String value(List<String> values, Map<String, Integer> columns, String name) {
		Integer index = columns.get(name);
		if (index == null || index >= values.size()) {
			throw new IllegalArgumentException("missing CSV field " + name);
		}
		return values.get(index);
	}

	private static List<String> parseCsvLine(String line) {
		if (line == null) {
			throw new IllegalArgumentException("empty CSV");
		}
		List<String> result = new ArrayList<>();
		StringBuilder value = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char current = line.charAt(index);
			if (quoted && current == '"' && index + 1 < line.length() && line.charAt(index + 1) == '"') {
				value.append('"');
				index++;
			} else if (current == '"') {
				quoted = !quoted;
			} else if (current == ',' && !quoted) {
				result.add(value.toString());
				value.setLength(0);
			} else {
				value.append(current);
			}
		}
		if (quoted) {
			throw new IllegalArgumentException("unterminated quoted CSV field");
		}
		result.add(value.toString());
		return result;
	}

	private static String stripBom(String value) {
		return value != null && !value.isEmpty() && value.charAt(0) == '\ufeff' ? value.substring(1) : value;
	}

	static void write(Path output, List<AuditRow> rows) throws IOException {
		Files.createDirectories(output.getParent());
		try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
			writer.write('\ufeff');
			writeCsvRow(writer, OUTPUT_FIELDS);
			for (AuditRow row : rows) {
				writeCsvRow(writer, List.of(Integer.toString(row.questId()), row.sourceFile(),
					row.serverSourceState(), row.npcId(), row.triggerAction(), row.actualPath(),
					row.shownPage(), row.clientVisibleAction(), row.clientExpected(), row.evidenceSource(),
					row.auditStatus(), row.fixStatus(), row.unresolvedReason()));
			}
		}
	}

	private static void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
		for (int index = 0; index < values.size(); index++) {
			if (index != 0) {
				writer.write(',');
			}
			String value = nullToEmpty(values.get(index));
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

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	record AuditRow(int questId, String sourceFile, String serverSourceState, String npcId,
		String triggerAction, String actualPath, String shownPage, String clientVisibleAction,
		String clientExpected, String evidenceSource, String auditStatus, String fixStatus,
		String unresolvedReason) {
	}

	record ClientQuest(String sourceFile, Map<Integer, ClientPage> pages) {
	}

	record ClientPage(int pageId, String pageName, Map<Integer, ClientAction> actions, String evidence) {
	}

	record ClientAction(int actionId, String evidence) {
	}

	private static final class MutableClientQuest {
		private final Set<String> sourceFiles = new LinkedHashSet<>();
		private final Map<Integer, MutableClientPage> pages = new LinkedHashMap<>();

		private MutableClientQuest(String sourceFile) {
			sourceFiles.add(sourceFile);
		}
	}

	private static final class MutableClientPage {
		private final String pageName;
		private final Map<Integer, ClientAction> actions = new LinkedHashMap<>();
		private final Set<String> evidence = new LinkedHashSet<>();

		private MutableClientPage(String pageName) {
			this.pageName = pageName;
		}
	}
}
