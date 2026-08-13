package com.aionemu.gameserver.questEngine.definition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
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
		"candidate_count", "candidate_index", "candidate_source_node", "candidate_target_node",
		"candidate_target_status", "candidate_target_variables", "candidate_conditions",
		"candidate_priority", "candidate_transaction_actions", "candidate_response",
		"candidate_after_commit_sequence",
		"evidence_source", "audit_status", "fix_status", "unresolved_reason");

	private QuestDialogOrderAudit() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			throw new IllegalArgumentException(
				"usage: QuestDialogOrderAudit <client-pages.csv> <client-details.csv> <output.csv>");
		}
		Path pages = Path.of(args[0]).toAbsolutePath().normalize();
		Path details = Path.of(args[1]).toAbsolutePath().normalize();
		Path output = Path.of(args[2]).toAbsolutePath().normalize();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(
			QuestDialogOrderAudit.class.getClassLoader());
		List<AuditRow> rows = audit(catalog, readClientPages(pages, details));
		write(output, rows);
		Map<String, Long> counts = rows.stream().collect(java.util.stream.Collectors.groupingBy(
			AuditRow::auditStatus, java.util.TreeMap::new, java.util.stream.Collectors.counting()));
		System.out.printf("rows=%d statuses=%s output=%s%n", rows.size(), counts, output);
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
			Set<Integer> clientVisibleActions = client.pages().values().stream()
				.flatMap(page -> page.actions().keySet().stream())
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
			List<QuestTransition> pending = new ArrayList<>(dialogRoutes.stream()
				.filter(transition -> isExternalDialogEntry(transition, client, clientVisibleActions)).toList());
			Set<QuestTransition> visited = new LinkedHashSet<>();
			for (int index = 0; index < pending.size(); index++) {
				QuestTransition trigger = pending.get(index);
				if (!visited.add(trigger)) {
					continue;
				}
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
							"shown server page must exist in the active client page index",
							-1, null,
							client.sourceFile(), "EVIDENCE_REQUIRED", "EVIDENCE_REQUIRED",
							"compiled IR emits a task page absent from the active client page index"));
						continue;
					}
					if (page.actions().isEmpty() && page.unmappedActions().isEmpty()) {
						result.add(new AuditRow(definition.id(), client.sourceFile(),
							nullToEmpty(trigger.sourceNode()), ownerNpc(trigger.event()),
							Integer.toString(dialogAction(trigger.event())),
							trigger.sourceNode() + " + " + dialogOwner(trigger.event()) + " + "
								+ dialogAction(trigger.event()) + " -> " + trigger.targetNode()
								+ " + terminal page " + shown.dialogId(), Integer.toString(shown.dialogId()), "",
							"server response reaches an active client page with no visible action",
							-1, null,
							page.evidence(), "TERMINAL_PAGE_REACHED", "NOT_NEEDED", ""));
						continue;
					}
					for (Map.Entry<String, String> unmapped : page.unmappedActions().entrySet()) {
						result.add(new AuditRow(definition.id(), client.sourceFile(),
							nullToEmpty(trigger.sourceNode()), ownerNpc(trigger.event()),
							Integer.toString(dialogAction(trigger.event())),
							trigger.sourceNode() + " + " + dialogOwner(trigger.event()) + " + "
								+ dialogAction(trigger.event()) + " -> " + trigger.targetNode()
								+ " + page " + shown.dialogId(), Integer.toString(shown.dialogId()), unmapped.getKey(),
							"visible client action must map to a HyperLinks.xml protocol id",
							-1, null,
							unmapped.getValue(), "EVIDENCE_REQUIRED", "EVIDENCE_REQUIRED",
							"visible client action has no exact HyperLinks.xml mapping"));
					}
					for (ClientAction action : page.actions().values()) {
						List<QuestTransition> candidates = dialogRoutes.stream().filter(candidate ->
							sameDialogOwner(trigger.event(), candidate.event())
								&& dialogAction(candidate.event()) == action.actionId()
								&& startsFromNode(candidate, trigger.targetNode(), definition)).toList();
						result.addAll(rows(definition, client.sourceFile(), trigger, shown.dialogId(),
							action, candidates));
						pending.addAll(candidates);
					}
				}
			}
			for (ClientPage page : client.pages().values()) {
				if ((!page.actions().isEmpty() || !page.unmappedActions().isEmpty())
						&& !shownPages.contains(page.pageId())) {
					result.add(new AuditRow(definition.id(), client.sourceFile(), "", "", "",
						"no compiled transition emits this active client page", Integer.toString(page.pageId()),
						joinActions(page), "active client page must be emitted before its buttons can be used",
						-1, null,
						page.evidence(), "CLIENT_PAGE_UNREACHED", "EVIDENCE_REQUIRED",
						"active page is absent from compiled IR responses; no current path identifies its NPC or state"));
				}
			}
		}
		result = new ArrayList<>(new LinkedHashSet<>(result));
		result.sort(Comparator.comparingInt(AuditRow::questId)
			.thenComparing(AuditRow::sourceFile)
			.thenComparing(AuditRow::shownPage)
			.thenComparing(AuditRow::clientVisibleAction)
			.thenComparing(AuditRow::actualPath)
			.thenComparingInt(row -> row.candidate() == null ? 0 : row.candidate().index()));
		return List.copyOf(result);
	}

	private static boolean isExternalDialogEntry(QuestTransition transition, ClientQuest client,
			Set<Integer> clientVisibleActions) {
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

	private static List<AuditRow> rows(QuestDefinition definition, String sourceFile,
			QuestTransition trigger, int shownPage, ClientAction clientAction,
			List<QuestTransition> candidates) {
		String owner = dialogOwner(trigger.event());
		String triggerAction = Integer.toString(dialogAction(trigger.event()));
		String path = trigger.sourceNode() + " + " + owner + " + " + triggerAction + " -> "
			+ trigger.targetNode() + " + page " + shownPage;
		String expected = trigger.targetNode() + " + " + owner + " + " + clientAction.actionId()
			+ " must have a compiled route";
		boolean fixedExchange = FIXED_EQUIPMENT_EXCHANGES.contains(definition.id());
		String evidence = clientAction.evidence();
		if (fixedExchange) {
			evidence += " | origin/history daevanion handler for quest " + definition.id()
				+ " | quest_data.xml reward groups";
		}
		if (candidates.isEmpty()) {
			return List.of(new AuditRow(definition.id(), sourceFile, nullToEmpty(trigger.sourceNode()),
				ownerNpc(trigger.event()), triggerAction, path, Integer.toString(shownPage),
				Integer.toString(clientAction.actionId()), expected, 0, null, evidence,
				"EVIDENCE_REQUIRED", "EVIDENCE_REQUIRED",
				"visible client action has no route; client does not prove its response page or state side effect"));
		}
		List<QuestTransition> sorted = candidates.stream()
			.sorted(Comparator.comparing(candidate -> candidateSortKey(candidate, definition)))
			.toList();
		List<AuditRow> rows = new ArrayList<>(sorted.size());
		for (int index = 0; index < sorted.size(); index++) {
			CandidateContract contract = candidateContract(index + 1, sorted.get(index), definition);
			rows.add(new AuditRow(definition.id(), sourceFile, nullToEmpty(trigger.sourceNode()),
				ownerNpc(trigger.event()), triggerAction, path, Integer.toString(shownPage),
				Integer.toString(clientAction.actionId()), expected, sorted.size(), contract, evidence,
				"PAGE_ACTION_MATCHED", fixedExchange ? "FIXED" : "NOT_NEEDED", ""));
		}
		return List.copyOf(rows);
	}

	private static CandidateContract candidateContract(int index, QuestTransition candidate,
			QuestDefinition definition) {
		NodeProjection target = node(definition, candidate.targetNode()).projection();
		return new CandidateContract(index, nullToEmpty(candidate.sourceNode()), candidate.targetNode(),
			target.status().name(), serializeMap(target.variables()), serializeSequence(candidate.conditions()),
			candidate.priority() == null ? "" : candidate.priority().toString(),
			serializeSequence(candidate.actions()), serializeResponse(candidate.afterCommit()),
			serializeSequence(candidate.afterCommit()));
	}

	private static String candidateSortKey(QuestTransition candidate, QuestDefinition definition) {
		CandidateContract contract = candidateContract(0, candidate, definition);
		return String.join("\u0000", contract.sourceNode(), contract.targetNode(), contract.targetStatus(),
			contract.targetVariables(), contract.conditions(), contract.priority(), contract.transactionActions(),
			contract.response(), contract.afterCommitSequence());
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow(() -> new IllegalArgumentException("unknown quest node " + label));
	}

	private static String serializeResponse(List<AfterCommitAction> actions) {
		List<String> responses = new ArrayList<>();
		for (int index = 0; index < actions.size(); index++) {
			AfterCommitAction action = actions.get(index);
			String response = switch (action) {
				case AfterCommitAction.CloseDialog ignored -> "CLOSE_DIALOG";
				case AfterCommitAction.ShowQuestDialog shown -> "SHOW_QUEST_PAGE(page=" + shown.dialogId() + ")";
				case AfterCommitAction.ShowQuestSelectionDialog shown ->
					"SHOW_SELECTION_PAGE(page=" + shown.dialogId() + ")";
				case AfterCommitAction.ShowDialogWindow shown -> "SHOW_DIALOG_WINDOW(page=" + shown.dialogId() + ")";
				default -> null;
			};
			if (response != null) {
				responses.add((index + 1) + ":" + response);
			}
		}
		return String.join(" -> ", responses);
	}

	private static String serializeSequence(List<?> values) {
		List<String> serialized = new ArrayList<>(values.size());
		for (int index = 0; index < values.size(); index++) {
			serialized.add((index + 1) + ":" + serializeValue(values.get(index)));
		}
		return String.join(" -> ", serialized);
	}

	private static String serializeMap(Map<?, ?> values) {
		return values.entrySet().stream()
			.map(entry -> serializeValue(entry.getKey()) + "=" + serializeValue(entry.getValue()))
			.sorted()
			.collect(java.util.stream.Collectors.joining(", ", "{", "}"));
	}

	private static String serializeValue(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof String string) {
			return '"' + string.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
		}
		if (value instanceof Enum<?> enumValue) {
			return enumValue.name();
		}
		if (value instanceof Map<?, ?> map) {
			return serializeMap(map);
		}
		if (value instanceof Set<?> set) {
			return set.stream().map(QuestDialogOrderAudit::serializeValue).sorted()
				.collect(java.util.stream.Collectors.joining(", ", "{", "}"));
		}
		if (value instanceof Collection<?> collection) {
			return collection.stream().map(QuestDialogOrderAudit::serializeValue)
				.collect(java.util.stream.Collectors.joining(", ", "[", "]"));
		}
		if (value.getClass().isArray()) {
			List<String> elements = new ArrayList<>(Array.getLength(value));
			for (int index = 0; index < Array.getLength(value); index++) {
				elements.add(serializeValue(Array.get(value, index)));
			}
			return "[" + String.join(", ", elements) + "]";
		}
		if (value.getClass().isRecord()) {
			List<String> components = new ArrayList<>();
			for (RecordComponent component : value.getClass().getRecordComponents()) {
				try {
					components.add(component.getName() + "=" + serializeValue(component.getAccessor().invoke(value)));
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("cannot serialize " + value.getClass().getName(), e);
				}
			}
			return value.getClass().getSimpleName() + "(" + String.join(", ", components) + ")";
		}
		return String.valueOf(value);
	}

	private static boolean startsFromNode(QuestTransition transition, String nodeLabel,
			QuestDefinition definition) {
		if (Objects.equals(transition.sourceNode(), nodeLabel)) {
			return true;
		}
		if (transition.sourceNode() != null) {
			QuestNode actual = node(definition, nodeLabel);
			QuestNode declared = node(definition, transition.sourceNode());
			return definition.metadata().repeatPolicy().maxRepeatCount() > 1
				&& actual.projection().status() == com.aionemu.gameserver.questEngine.model.QuestStatus.COMPLETE
				&& declared.projection().status() == com.aionemu.gameserver.questEngine.model.QuestStatus.NONE
				&& transition.conditions().stream().anyMatch(QuestCondition.StartEligible.class::isInstance);
		}
		QuestNode node = node(definition, nodeLabel);
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

	private static String joinActions(ClientPage page) {
		List<String> actions = new ArrayList<>();
		page.actions().keySet().stream().sorted().map(String::valueOf).forEach(actions::add);
		page.unmappedActions().keySet().stream().sorted().forEach(actions::add);
		return String.join(" ", actions);
	}

	static Map<Integer, ClientQuest> readClientPages(Path pageIndex, Path details) throws IOException {
		Map<Integer, MutableClientQuest> quests = new LinkedHashMap<>();
		Set<Integer> ambiguousQuests = new LinkedHashSet<>();
		try (BufferedReader reader = Files.newBufferedReader(pageIndex, StandardCharsets.UTF_8)) {
			Map<String, Integer> columns = columns(parseCsvLine(stripBom(reader.readLine())));
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> values = parseCsvLine(line);
				if (!"active".equals(value(values, columns, "source_variant"))
						|| !"exact".equals(value(values, columns, "page_mapping"))) {
					continue;
				}
				int questId = Integer.parseInt(value(values, columns, "quest_id"));
				int pageId = Integer.parseInt(value(values, columns, "page_id"));
				String sourceFile = value(values, columns, "source_file");
				String pageName = value(values, columns, "html_page_name");
				String pageOrder = value(values, columns, "page_order");
				String sourceHash = value(values, columns, "source_sha256");
				MutableClientQuest quest = quests.computeIfAbsent(questId,
					ignored -> new MutableClientQuest(sourceFile));
				quest.sourceFiles.add(sourceFile);
				quest.sourceIdentities.add(sourceFile + "\u0000" + sourceHash);
				if (quest.sourceIdentities.size() > 1) {
					ambiguousQuests.add(questId);
				}
				int actionCount = Integer.parseInt(value(values, columns, "action_count"));
				MutableClientPage page = quest.pages.get(pageId);
				if (page == null) {
					page = new MutableClientPage(pageName, Integer.parseInt(pageOrder), sourceFile, sourceHash,
						actionCount);
					quest.pages.put(pageId, page);
				} else if (!page.pageName.equals(pageName) || !page.sourceFile.equals(sourceFile)
						|| !page.sourceSha256.equals(sourceHash)
						|| page.actionCount != 0 || actionCount != 0) {
					// Repeated interactive pages, or pages from different active sources, are ambiguous.
					ambiguousQuests.add(questId);
				}
				page.evidence.add(sourceFile + "#" + pageName + " page-order=" + pageOrder
					+ " sha256=" + sourceHash);
			}
		}
		try (BufferedReader reader = Files.newBufferedReader(details, StandardCharsets.UTF_8)) {
			Map<String, Integer> columns = columns(parseCsvLine(stripBom(reader.readLine())));
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> values = parseCsvLine(line);
				if (!"active".equals(value(values, columns, "source_variant"))
						|| !"exact".equals(value(values, columns, "page_mapping"))) {
					continue;
				}
				int questId = Integer.parseInt(value(values, columns, "quest_id"));
				int pageId = Integer.parseInt(value(values, columns, "page_id"));
				String sourceFile = value(values, columns, "source_file");
				String pageName = value(values, columns, "html_page_name");
				String buttonText = value(values, columns, "button_text_zh");
				MutableClientQuest quest = quests.get(questId);
				if (quest == null || !quest.pages.containsKey(pageId)) {
					throw new IllegalArgumentException("client action references a page absent from the page index: quest "
						+ questId + " page " + pageId);
				}
				quest.sourceFiles.add(sourceFile);
				MutableClientPage page = quest.pages.get(pageId);
				if (!page.sourceFile.equals(sourceFile) || !page.sourceSha256.equals(value(values, columns, "source_sha256"))) {
					ambiguousQuests.add(questId);
					continue;
				}
				String evidence = sourceFile + "#" + pageName + ": " + buttonText;
				if ("exact".equals(value(values, columns, "action_mapping"))) {
					int actionId = Integer.parseInt(value(values, columns, "action_id"));
					page.actions.putIfAbsent(actionId, new ClientAction(actionId, evidence));
				} else {
					page.unmappedActions.putIfAbsent(value(values, columns, "action_constant"), evidence);
				}
			}
		}
		Map<Integer, ClientQuest> result = new LinkedHashMap<>();
		if (!ambiguousQuests.isEmpty()) {
			throw new IllegalArgumentException("ambiguous active client pages for quests "
				+ ambiguousQuests.stream().sorted().toList());
		}
		quests.forEach((questId, quest) -> {
			Map<Integer, ClientPage> pages = new LinkedHashMap<>();
			quest.pages.forEach((pageId, page) -> pages.put(pageId,
				new ClientPage(pageId, page.pageName, page.pageOrder, Map.copyOf(page.actions),
					Map.copyOf(page.unmappedActions), String.join(" | ", page.evidence))));
			result.put(questId, new ClientQuest(String.join(" | ", quest.sourceFiles), Map.copyOf(pages)));
		});
		return Map.copyOf(result);
	}

	private static Map<String, Integer> columns(List<String> header) {
		Map<String, Integer> columns = new HashMap<>();
		for (int index = 0; index < header.size(); index++) {
			columns.put(header.get(index), index);
		}
		return columns;
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
					row.shownPage(), row.clientVisibleAction(), row.clientExpected(),
					row.candidateCount() < 0 ? "" : Integer.toString(row.candidateCount()),
					row.candidate() == null ? "" : Integer.toString(row.candidate().index()),
					candidateValue(row, CandidateContract::sourceNode),
					candidateValue(row, CandidateContract::targetNode),
					candidateValue(row, CandidateContract::targetStatus),
					candidateValue(row, CandidateContract::targetVariables),
					candidateValue(row, CandidateContract::conditions),
					candidateValue(row, CandidateContract::priority),
					candidateValue(row, CandidateContract::transactionActions),
					candidateValue(row, CandidateContract::response),
					candidateValue(row, CandidateContract::afterCommitSequence), row.evidenceSource(),
					row.auditStatus(), row.fixStatus(), row.unresolvedReason()));
			}
		}
	}

	private static String candidateValue(AuditRow row,
			java.util.function.Function<CandidateContract, String> value) {
		return row.candidate() == null ? "" : value.apply(row.candidate());
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
		String clientExpected, int candidateCount, CandidateContract candidate, String evidenceSource,
		String auditStatus, String fixStatus, String unresolvedReason) {
	}

	record CandidateContract(int index, String sourceNode, String targetNode, String targetStatus,
		String targetVariables, String conditions, String priority, String transactionActions,
		String response, String afterCommitSequence) {
	}

	record ClientQuest(String sourceFile, Map<Integer, ClientPage> pages) {
	}

	record ClientPage(int pageId, String pageName, int pageOrder, Map<Integer, ClientAction> actions,
			Map<String, String> unmappedActions, String evidence) {
	}

	record ClientAction(int actionId, String evidence) {
	}

	private static final class MutableClientQuest {
		private final Set<String> sourceFiles = new LinkedHashSet<>();
		private final Set<String> sourceIdentities = new LinkedHashSet<>();
		private final Map<Integer, MutableClientPage> pages = new LinkedHashMap<>();

		private MutableClientQuest(String sourceFile) {
			sourceFiles.add(sourceFile);
		}
	}

	private static final class MutableClientPage {
		private final String pageName;
		private final int pageOrder;
		private final String sourceFile;
		private final String sourceSha256;
		private final int actionCount;
		private final Map<Integer, ClientAction> actions = new LinkedHashMap<>();
		private final Map<String, String> unmappedActions = new LinkedHashMap<>();
		private final Set<String> evidence = new LinkedHashSet<>();

		private MutableClientPage(String pageName, int pageOrder, String sourceFile, String sourceSha256,
				int actionCount) {
			this.pageName = pageName;
			this.pageOrder = pageOrder;
			this.sourceFile = sourceFile;
			this.sourceSha256 = sourceSha256;
			this.actionCount = actionCount;
		}
	}
}
