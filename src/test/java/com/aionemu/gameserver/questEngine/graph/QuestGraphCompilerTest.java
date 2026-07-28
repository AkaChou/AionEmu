package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuestGraphCompilerTest {
	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");
	private static final QuestGraphCompiler.References REFERENCES = new QuestGraphCompiler.References(Set.of(1, 2), Set.of(203709));

	@TempDir
	Path tempDir;
	private int fileIndex;

	@Test
	void compilesSchemaValidatedGraphsIntoDeterministicImmutableIr() throws Exception {
		String xml = """
			<quest_graphs>
				%s
				%s
			</quest_graphs>
			""".formatted(graph(2, "b", transition("late", 20, "done") + transition("first", 10, "done"), "<node id=\"done\" terminal=\"true\"/>"),
			graph(1, "offer", transition("accept", 10, "accepted"), "<node id=\"accepted\" terminal=\"true\"/>"));
		CompiledQuestGraphData data = load(xml);
		var graphs = data.graphs();

		assertEquals(List.of(1, 2), new ArrayList<>(graphs.keySet()));
		CompiledQuestGraph graph = graphs.get(2);
		assertEquals(List.of("b", "done"), new ArrayList<>(graph.nodes().keySet()));
		assertEquals(List.of("counter", "enabled"), new ArrayList<>(graph.variables().keySet()));
		assertEquals(new CompiledQuestGraph.IntVariable("counter", CompiledQuestGraph.StateScope.PLAYER, 0, 0, 4),
			graph.variables().get("counter"));
		assertEquals(List.of("first", "late"), graph.nodes().get("b").transitions().stream().map(CompiledQuestGraph.Transition::id).toList());
		assertEquals(CompiledQuestGraph.EventType.DIALOG, graph.nodes().get("b").transitions().getFirst().event().type());
		assertEquals(CompiledQuestGraph.ConditionType.QUEST_STATUS,
			graph.nodes().get("b").transitions().getFirst().conditions().getFirst().type());
		assertEquals(CompiledQuestGraph.ActionType.START_QUEST,
			graph.nodes().get("b").transitions().getFirst().actions().getFirst().type());
		var routes = data.eventIndex().get(new CompiledQuestGraphData.EventKey(CompiledQuestGraph.EventType.DIALOG, 203709));
		assertEquals(List.of("accept", "first", "late"), routes.stream().map(route -> route.transition().id()).toList());
		assertEquals(data, load(xml));
		assertThrows(UnsupportedOperationException.class, () -> graphs.put(3, graph));
		assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
		assertThrows(UnsupportedOperationException.class, () -> graph.nodes().get("b").transitions().clear());
		assertThrows(UnsupportedOperationException.class, routes::clear);
	}

	@Test
	void schemaRejectsUnknownElementsCapabilitiesAndMissingRequiredAttributes() {
		assertThrows(IllegalArgumentException.class, () -> load("<quest_graphs><script/></quest_graphs>"));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("<start-quest/>", "<complete-quest/>"), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("<dialog", "<kill"), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done"), terminal()).replace(" scope=\"PLAYER\"", ""))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace(" priority=\"10\"", ""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			variables().replaceFirst(" scope=\"PLAYER\"", ""),
			transition("accept", 10, "done"), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "x".repeat(129),
			transition("accept", 10, "done"), terminal()))));
		assertThrows(IllegalArgumentException.class,
			() -> load("<!DOCTYPE quest_graphs [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><quest_graphs>&xxe;</quest_graphs>"));
	}

	@Test
	void compilerRejectsDuplicateOwnersNodesTransitionsAndBadEdges() {
		assertFailureContains(document(graph(1, "offer", transition("accept", 10, "done"), terminal())
			+ graph(1, "offer", transition("accept", 10, "done"), terminal())), "Duplicate quest owner 1");
		assertFailureContains(document(graph(1, "offer", transition("accept", 10, "done"),
			"<node id=\"done\" terminal=\"true\"/><node id=\"done\" terminal=\"true\"/>")), "duplicate node done");
		assertFailureContains(document(graph(1, "offer", transition("same", 10, "middle"),
			"<node id=\"middle\">" + transition("same", 20, "done") + "</node>" + terminal())), "duplicate transition same");
		assertFailureContains(document(graph(1, "offer", transition("accept", 10, "missing"), terminal())), "targets missing node missing");
	}

	@Test
	void compilerRejectsUnreachableNodesMissingTerminalAndAmbiguousTransitions() {
		assertFailureContains(document(graph(1, "offer", transition("accept", 10, "done"),
			terminal() + "<node id=\"orphan\" terminal=\"true\"/>")), "unreachable nodes [orphan]");
		assertFailureContains(document(graph(1, "offer", transition("again", 10, "offer"), "")), "no reachable terminal node");
		assertFailureContains(document(graph(1, "offer",
			transition("first", 10, "done") + transition("second", 10, "done"), terminal())), "ambiguous DIALOG priority 10");
	}

	@Test
	void compilerRejectsInvalidVariablesAndMissingReferences() {
		String duplicateVariables = variables()
			.replace("</variables>", "<variable name=\"counter\" type=\"INT\" scope=\"PLAYER\" initial=\"0\" min=\"0\" max=\"1\"/></variables>");
		assertFailureContains(document(graph(1, "offer", duplicateVariables, transition("accept", 10, "done"), terminal())),
			"duplicate variable counter");
		assertFailureContains(document(graph(1, "offer", variables().replace("initial=\"0\"", "initial=\"5\""),
			transition("accept", 10, "done"), terminal())), "initial value is out of range");
		assertFailureContains(document(graph(1, "offer", variables().replace("name=\"enabled\"", "name=\"enabled\" min=\"0\""),
			transition("accept", 10, "done"), terminal())), "BOOLEAN variable enabled is invalid");

		IllegalArgumentException missingQuest = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", transition("accept", 10, "done"), terminal())),
				new QuestGraphCompiler.References(Set.of(), Set.of(203709))));
		assertCauseContains(missingQuest, "references missing quest 1");
		IllegalArgumentException missingNpc = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", transition("accept", 10, "done"), terminal())),
				new QuestGraphCompiler.References(Set.of(1), Set.of())));
		assertCauseContains(missingNpc, "references missing NPC 203709");
	}

	private CompiledQuestGraphData load(String xml) throws Exception {
		return load(xml, REFERENCES);
	}

	private CompiledQuestGraphData load(String xml, QuestGraphCompiler.References references) throws Exception {
		Path file = tempDir.resolve("graphs-" + fileIndex++ + ".xml");
		Files.writeString(file, xml, StandardCharsets.UTF_8);
		return QuestGraphCompiler.load(file, SCHEMA, references);
	}

	private void assertFailureContains(String xml, String message) {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> load(xml));
		assertCauseContains(error, message);
	}

	private static void assertCauseContains(Throwable error, String message) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause.getMessage() != null && cause.getMessage().contains(message)) {
				return;
			}
		}
		fail("Expected failure containing: " + message);
	}

	private static String document(String graph) {
		return "<quest_graphs>" + graph + "</quest_graphs>";
	}

	private static String graph(int questId, String initialNode, String transitions, String extraNodes) {
		return graph(questId, initialNode, variables(), transitions, extraNodes);
	}

	private static String graph(int questId, String initialNode, String variables, String transitions, String extraNodes) {
		return """
			<quest_graph quest_id="%d" version="1" scope="PLAYER" initial_node="%s">
				%s
				<node id="%s">%s</node>
				%s
			</quest_graph>
			""".formatted(questId, initialNode, variables, initialNode, transitions, extraNodes);
	}

	private static String variables() {
		return """
			<variables>
				<variable name="counter" type="INT" scope="PLAYER" initial="0" min="0" max="4"/>
				<variable name="enabled" type="BOOLEAN" scope="PLAYER" initial="false"/>
			</variables>
			""";
	}

	private static String transition(String id, int priority, String target) {
		return """
			<transition id="%s" priority="%d" to="%s">
				<dialog npc_id="203709" dialog="QUEST_SELECT"/>
				<conditions><quest-status value="NONE"/></conditions>
				<actions><start-quest/></actions>
			</transition>
			""".formatted(id, priority, target);
	}

	private static String terminal() {
		return "<node id=\"done\" terminal=\"true\"/>";
	}
}
