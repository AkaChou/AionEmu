package com.aionemu.gameserver.questEngine.graph;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.ConditionOperation;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

class QuestGraphCompilerTest {
	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");
	private static final QuestGraphCompiler.References REFERENCES = new QuestGraphCompiler.References(Set.of(1, 2), Set.of(203709),
			Set.of(182200001), Set.of(42), Set.of("TEST_ZONE"), Set.of(913),
			Set.of(new QuestGraphCompiler.WindstreamRouteReference(210130000, 405)),
			Set.of(new QuestGraphCompiler.FlyingRingReference(210020000, "ELTNEN_AIR_BOOSTER_1")), Set.of(9832));

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
		assertEquals(List.of(
			new CompiledQuestGraph.QuestStatusCondition(CompiledQuestGraph.QuestStatus.NONE),
			new CompiledQuestGraph.QuestStatusCondition(2, ConditionOperation.IN,
				Set.of(CompiledQuestGraph.QuestStatus.COMPLETE)),
			new CompiledQuestGraph.QuestVariableCondition("counter", ConditionOperation.EQUAL, 0),
			new CompiledQuestGraph.QuestRepeatAvailableCondition(255, true, true),
			new CompiledQuestGraph.QuestCollectItemsCondition(),
			new CompiledQuestGraph.QuestRewardCondition(2, 1),
			new CompiledQuestGraph.QuestCompletionCountCondition(2, ConditionOperation.EQUAL, 3),
			new CompiledQuestGraph.PlayerLevelCondition(10, 55),
			new CompiledQuestGraph.PlayerRaceCondition(Set.of(Race.ELYOS, Race.ASMODIANS)),
			new CompiledQuestGraph.PlayerClassCondition(Set.of(PlayerClass.GLADIATOR, PlayerClass.TEMPLAR)),
			new CompiledQuestGraph.PlayerGenderCondition(Gender.MALE),
			new CompiledQuestGraph.PlayerTitleCondition(42),
			new CompiledQuestGraph.PlayerAbyssRankCondition(AbyssRankEnum.STAR1_OFFICER),
			new CompiledQuestGraph.PlayerInventoryCondition(182200001, ConditionOperation.GREATER_EQUAL, 1),
			new CompiledQuestGraph.PlayerEquippedCondition(182200001)),
			graph.nodes().get("b").transitions().getFirst().conditions());
		assertEquals(List.of(
			CompiledQuestGraph.ActionType.START_QUEST,
			CompiledQuestGraph.ActionType.SET_QUEST_VARIABLE,
			CompiledQuestGraph.ActionType.ADD_QUEST_VARIABLE,
			CompiledQuestGraph.ActionType.SET_QUEST_STATUS,
			CompiledQuestGraph.ActionType.GIVE_QUEST_ITEM,
			CompiledQuestGraph.ActionType.REMOVE_QUEST_ITEM,
			CompiledQuestGraph.ActionType.REMOVE_COLLECTED_ITEMS,
			CompiledQuestGraph.ActionType.FINISH_QUEST,
			CompiledQuestGraph.ActionType.SYNC_QUEST_STATUS,
			CompiledQuestGraph.ActionType.SEND_DIALOG,
			CompiledQuestGraph.ActionType.CLOSE_DIALOG,
			CompiledQuestGraph.ActionType.SHOW_QUEST_LIST,
			CompiledQuestGraph.ActionType.SEND_PLAYER_MESSAGE),
			graph.nodes().get("b").transitions().getFirst().actions().stream().map(CompiledQuestGraph.Action::type).toList());
		var routes = data.eventIndex().get(new CompiledQuestGraphData.EventKey(CompiledQuestGraph.EventType.DIALOG, 203709));
		assertEquals(List.of("accept", "first", "late"), routes.stream().map(route -> route.transition().id()).toList());
		assertEquals(data, load(xml));
		assertThrows(UnsupportedOperationException.class, () -> graphs.put(3, graph));
		assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
		assertThrows(UnsupportedOperationException.class, () -> graph.nodes().get("b").transitions().clear());
		assertThrows(UnsupportedOperationException.class, routes::clear);
	}

	/**
	 * 使用真实编译器离线编译由系统属性指定的完整候选批次。
	 * Offline-compiles a complete candidate batch selected through system properties with the real compiler.
	 */
	@Test
	@EnabledIfSystemProperty(named = "questGraphBatchFile", matches = ".+")
	void compilesRequestedGeneratedBatchFile() throws Exception {
		Path file = Path.of(System.getProperty("questGraphBatchFile"));
		CompiledQuestGraphData data = QuestGraphCompiler.load(file, SCHEMA, referencesDeclaredIn(file));
		long transitionCount = data.graphs().values().stream().flatMap(graph -> graph.nodes().values().stream())
			.flatMap(node -> node.transitions().stream()).count();

		assertEquals(Integer.parseInt(System.getProperty("questGraphExpectedCount")), data.graphs().size());
		assertEquals(Long.parseLong(System.getProperty("questGraphExpectedTransitionCount")), transitionCount);
	}

	@Test
	void schemaRejectsUnknownElementsCapabilitiesAndMissingRequiredAttributes() throws Exception {
		CompiledQuestGraph locked = load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("values=\"NONE\"", "values=\"LOCKED\""), terminal()))).graphs().get(1);
		assertEquals(new CompiledQuestGraph.QuestStatusCondition(CompiledQuestGraph.QuestStatus.LOCKED),
			locked.nodes().get("offer").transitions().getFirst().conditions().getFirst());
		assertThrows(IllegalArgumentException.class, () -> load("<quest_graphs><script/></quest_graphs>"));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("<start-quest/>", "<complete-quest/>"), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("mode=\"TOP_UP_TO\"", "mode=\"ADD\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("mode=\"EXACT\"", "mode=\"BEST_EFFORT\""), terminal()))));
		assertFailureContains(document(graph(1, "offer",
			transition("accept", 10, "done").replace("<start-quest/>", "<send-dialog dialog_id=\"1\"/><start-quest/>"), terminal())),
			"invalid action phase order");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("<dialog", "<attack"), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("<player-level min=\"10\" max=\"55\"/>", "<player-level min=\"10\" max=\"5\"/>"),
			terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("values=\"ELYOS ASMODIANS\"", "values=\"NPC\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("values=\"GLADIATOR TEMPLAR\"", "values=\"ALL\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("value=\"MALE\"", "value=\"DUMMY\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("minimum=\"10\"", "minimum=\"19\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("op=\"GREATER_EQUAL\"", "op=\"IN\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("op=\"IN\" values=\"NONE\"", "op=\"EQUAL\" values=\"NONE\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("accept", 10, "done").replace("count=\"1\"", "count=\"-1\""), terminal()))));
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

	/**
	 * 验证 XSD 与正式 compiler 只接受封闭的可选精确扣除模式。
	 * Verifies that the XSD and production compiler accept only the closed optional-exact removal mode.
	 */
	@Test
	void compilerBuildsClosedOptionalExactItemRemoval() throws Exception {
		String optional = document(graph(1, "offer",
			transition("accept", 10, "done").replace("mode=\"EXACT\"", "mode=\"OPTIONAL_EXACT\""), terminal()));
		CompiledQuestGraph.Action action = load(optional).graphs().get(1).nodes().get("offer").transitions().getFirst().actions().stream()
			.filter(CompiledQuestGraph.RemoveQuestItemAction.class::isInstance)
			.findFirst().orElseThrow();

		assertEquals(new CompiledQuestGraph.RemoveQuestItemAction(182200001, 1,
			CompiledQuestGraph.QuestItemRemovalMode.OPTIONAL_EXACT), action);
		assertThrows(IllegalArgumentException.class, () -> load(optional.replace("OPTIONAL_EXACT", "BEST_EFFORT")));
	}

	/**
	 * 验证编译器在 typed bridge 完成前拒绝非 PLAYER 图和变量范围。
	 * Verifies that the compiler rejects non-PLAYER graph and variable scopes until typed bridges exist.
	 */
	@Test
	void compilerRejectsScopesWithoutTypedRuntimeBridge() {
		String source = document(graph(1, "offer", transition("accept", 10, "done"), terminal()));

		assertFailureContains(source.replace("scope=\"PLAYER\" initial_node", "scope=\"PARTY\" initial_node"),
			"graph requires unsupported PARTY scope");
		assertFailureContains(source.replaceFirst("type=\"INT\" scope=\"PLAYER\"", "type=\"INT\" scope=\"WORLD\""),
			"variable counter requires unsupported WORLD scope");
	}

	/**
	 * 验证完成次数与计时器 XML 编译为封闭动作，并固定追加提交后协议。
	 * Verifies completion-count and timer XML compile into closed actions with fixed post-commit protocol.
	 */
	@Test
	void compilerBuildsCompletionCountAndTimerActions() throws Exception {
		String sourceTransition = transition("accept", 10, "done")
			.replace("<give-quest-item", "<set-completion-count count=\"0\"/><add-completion-count delta=\"1\"/><give-quest-item")
			.replace("<sync-quest-status/>", "<start-quest-timer timer=\"QUEST_TIMER\" duration_seconds=\"300\"/>");
		List<CompiledQuestGraph.Action> actions = load(document(graph(1, "offer", sourceTransition, terminal()))).graphs().get(1)
			.nodes().get("offer").transitions().getFirst().actions();

		assertTrue(actions.contains(new CompiledQuestGraph.SetCompletionCountAction(0)));
		assertTrue(actions.contains(new CompiledQuestGraph.AddCompletionCountAction(1)));
		assertTrue(actions.contains(new CompiledQuestGraph.StartQuestTimerAction("QUEST_TIMER", 300)));
		assertTrue(actions.contains(new CompiledQuestGraph.SyncQuestTimerAction("QUEST_TIMER", 300)));

		String endTimer = transition("end", 10, "done").replace("<sync-quest-status/>",
			"<end-quest-timer timer=\"QUEST_TIMER\"/>");
		List<CompiledQuestGraph.Action> endActions = load(document(graph(1, "offer", endTimer, terminal()))).graphs().get(1)
			.nodes().get("offer").transitions().getFirst().actions();
		assertTrue(endActions.contains(new CompiledQuestGraph.EndQuestTimerAction("QUEST_TIMER")));
		assertTrue(endActions.contains(new CompiledQuestGraph.SyncQuestTimerAction("QUEST_TIMER", 0)));
	}

	/**
	 * 验证 item/housing 事件编译为以物品模板为键的强类型 IR，并强制引用闭包。
	 * Verifies item/housing events compile into item-template-keyed typed IR with reference closure.
	 */
	@Test
	void compilerBuildsTypedItemAndHousingEvents() throws Exception {
		List<String> elements = List.of("item-use", "item-obtained", "item-equipped", "house-item-use");
		List<CompiledQuestGraph.EventType> types = List.of(CompiledQuestGraph.EventType.ITEM_USE,
			CompiledQuestGraph.EventType.ITEM_OBTAINED, CompiledQuestGraph.EventType.ITEM_EQUIPPED,
			CompiledQuestGraph.EventType.HOUSE_ITEM_USE);
		for (int i = 0; i < elements.size(); i++) {
			String event = "<" + elements.get(i) + " item_id=\"182200001\"/>";
			String source = transition("item-event", 10, "done")
				.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", event);
			CompiledQuestGraph.Event compiled = load(document(graph(1, "offer", source, terminal()))).graphs().get(1)
				.nodes().get("offer").transitions().getFirst().event();
			assertEquals(types.get(i), compiled.type());
			assertEquals(182200001, compiled.targetId());
		}

		String missingReference = transition("item-event", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<item-use item_id=\"182200002\"/>");
		assertFailureContains(document(graph(1, "offer", missingReference, terminal())),
			"item-use references missing item 182200002");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			missingReference.replace(" item_id=\"182200002\"", ""), terminal()))));
	}

	/**
	 * 验证 world/zone 事件编译为固定目标或规范化区域引用，并拒绝缺失引用。
	 * Verifies world/zone events compile to fixed targets or canonical zone references and reject missing references.
	 */
	@Test
	void compilerBuildsTypedWorldAndZoneEvents() throws Exception {
		List<String> events = List.of("<world-entered/>", "<zone-entered zone_name=\"TEST_ZONE\"/>",
			"<zone-left zone_name=\"TEST_ZONE\"/>", "<zone-mission-ended/>");
		List<CompiledQuestGraph.EventType> types = List.of(CompiledQuestGraph.EventType.WORLD_ENTERED,
			CompiledQuestGraph.EventType.ZONE_ENTERED, CompiledQuestGraph.EventType.ZONE_LEFT,
			CompiledQuestGraph.EventType.ZONE_MISSION_ENDED);
		List<Integer> targets = List.of(0, "TEST_ZONE".hashCode(), "TEST_ZONE".hashCode(), 1);
		for (int i = 0; i < events.size(); i++) {
			String source = transition("world-zone-event", 10, "done")
				.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", events.get(i));
			CompiledQuestGraph.Event compiled = load(document(graph(1, "offer", source, terminal()))).graphs().get(1)
				.nodes().get("offer").transitions().getFirst().event();
			assertEquals(types.get(i), compiled.type());
			assertEquals(targets.get(i), compiled.targetId());
		}

		String missingZone = transition("zone-event", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<zone-entered zone_name=\"MISSING_ZONE\"/>");
		assertFailureContains(document(graph(1, "offer", missingZone, terminal())),
			"zone-entered references missing zone MISSING_ZONE");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			missingZone.replace("MISSING_ZONE", "lowercase_zone"), terminal()))));
	}

	/**
	 * 验证升级、登出、任务计时器和影片事件编译为封闭 IR，并强制影片引用闭包。
	 * Verifies level-up, logout, quest-timer, and movie events compile to closed IR with movie reference closure.
	 */
	@Test
	void compilerBuildsTypedLevelRecoveryTimerAndMovieEvents() throws Exception {
		List<String> events = List.of("<level-up/>", "<player-logout/>",
			"<quest-timer-ended timer=\"QUEST_TIMER\"/>", "<movie-ended movie_id=\"913\"/>");
		List<CompiledQuestGraph.EventType> types = List.of(CompiledQuestGraph.EventType.LEVEL_UP,
			CompiledQuestGraph.EventType.PLAYER_LOGOUT, CompiledQuestGraph.EventType.QUEST_TIMER_ENDED,
			CompiledQuestGraph.EventType.MOVIE_ENDED);
		List<Integer> targets = List.of(0, 0, 1, 913);
		List<String> qualifiers = java.util.Arrays.asList(null, null, "QUEST_TIMER", null);
		for (int i = 0; i < events.size(); i++) {
			String source = transition("lifecycle-event", 10, "done")
				.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", events.get(i));
			CompiledQuestGraph.Event compiled = load(document(graph(1, "offer", source, terminal()))).graphs().get(1)
				.nodes().get("offer").transitions().getFirst().event();
			assertEquals(types.get(i), compiled.type());
			assertEquals(targets.get(i), compiled.targetId());
			assertEquals(qualifiers.get(i), compiled.qualifier());
		}

		String missingMovie = transition("movie-event", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<movie-ended movie_id=\"914\"/>");
		assertFailureContains(document(graph(1, "offer", missingMovie, terminal())),
			"movie-ended references missing movie 914");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			missingMovie.replace(" movie_id=\"914\"", ""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			transition("timer-event", 10, "done").replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>",
				"<quest-timer-ended timer=\"invalid timer\"/>"), terminal()))));
	}

	/**
	 * 验证邻近 NPC 引用和护送 owner 信号编译为封闭事件 IR。
	 * Verifies NPC proximity references and escort-owner signals compile into closed event IR.
	 */
	@Test
	void compilerBuildsTypedProximityAndEscortEvents() throws Exception {
		List<String> events = List.of("<npc-proximity npc_id=\"203709\"/>", "<escort-reached-target/>",
			"<escort-lost-target/>");
		List<CompiledQuestGraph.EventType> types = List.of(CompiledQuestGraph.EventType.NPC_PROXIMITY,
			CompiledQuestGraph.EventType.ESCORT_REACHED_TARGET, CompiledQuestGraph.EventType.ESCORT_LOST_TARGET);
		List<Integer> targets = List.of(203709, 1, 1);
		for (int i = 0; i < events.size(); i++) {
			String source = transition("npc-signal-event", 10, "done")
				.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", events.get(i));
			CompiledQuestGraph.Event compiled = load(document(graph(1, "offer", source, terminal()))).graphs().get(1)
				.nodes().get("offer").transitions().getFirst().event();
			assertEquals(types.get(i), compiled.type());
			assertEquals(targets.get(i), compiled.targetId());
		}

		String missingNpc = transition("proximity-event", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<npc-proximity npc_id=\"203710\"/>");
		assertFailureContains(document(graph(1, "offer", missingNpc, terminal())),
			"npc-proximity references missing NPC 203710");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			missingNpc.replace(" npc_id=\"203710\"", ""), terminal()))));
	}

	/**
	 * 验证军衔击杀范围和 Dredgion 结算编译为封闭的 PvP 信号 IR，并拒绝缺失或越界军衔。
	 * Verifies ranked-kill ranges and Dredgion settlement compile into closed PvP-signal IR and reject missing or
	 * out-of-range ranks.
	 */
	@Test
	void compilerBuildsTypedPvpSignalEvents() throws Exception {
		for (int minimumRank : List.of(1, 18)) {
			String source = transition("ranked-kill", 10, "done")
				.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>",
					"<ranked-player-kill minimum_rank=\"" + minimumRank + "\"/>");
			CompiledQuestGraph.Event compiled = load(document(graph(1, "offer", source, terminal()))).graphs().get(1)
				.nodes().get("offer").transitions().getFirst().event();
			assertEquals(CompiledQuestGraph.EventType.RANKED_PLAYER_KILL, compiled.type());
			assertEquals(minimumRank, compiled.targetId());
			assertEquals(null, compiled.qualifier());
		}

		String settlement = transition("dredgion-settled", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<dredgion-settled/>");
		CompiledQuestGraph.Event compiledSettlement = load(document(graph(1, "offer", settlement, terminal()))).graphs().get(1)
			.nodes().get("offer").transitions().getFirst().event();
		assertEquals(CompiledQuestGraph.EventType.DREDGION_SETTLED, compiledSettlement.type());
		assertEquals(0, compiledSettlement.targetId());

		String ranked = transition("ranked-kill", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<ranked-player-kill minimum_rank=\"1\"/>");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			ranked.replace(" minimum_rank=\"1\"", ""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			ranked.replace("minimum_rank=\"1\"", "minimum_rank=\"0\""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			ranked.replace("minimum_rank=\"1\"", "minimum_rank=\"19\""), terminal()))));
	}

	/**
	 * 验证制作失败和 NPC 仇恨感知事件编译为引用闭合的强类型 IR。
	 * Verifies craft-failure and NPC aggro-perception events compile into reference-closed typed IR.
	 */
	@Test
	void compilerBuildsTypedCraftAndAggroEvents() throws Exception {
		List<String> events = List.of("<craft-failed item_id=\"182200001\"/>", "<npc-aggro-listed npc_id=\"203709\"/>");
		List<CompiledQuestGraph.EventType> types = List.of(CompiledQuestGraph.EventType.CRAFT_FAILED,
			CompiledQuestGraph.EventType.NPC_AGGRO_LISTED);
		List<Integer> targets = List.of(182200001, 203709);
		for (int i = 0; i < events.size(); i++) {
			String source = transition("server-signal", 10, "done")
				.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", events.get(i));
			CompiledQuestGraph.Event compiled = load(document(graph(1, "offer", source, terminal()))).graphs().get(1)
				.nodes().get("offer").transitions().getFirst().event();
			assertEquals(types.get(i), compiled.type());
			assertEquals(targets.get(i), compiled.targetId());
		}

		String missingItem = transition("craft-failed", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<craft-failed item_id=\"182200002\"/>");
		assertFailureContains(document(graph(1, "offer", missingItem, terminal())),
			"craft-failed references missing item 182200002");
		String missingNpc = transition("aggro-listed", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<npc-aggro-listed npc_id=\"203710\"/>");
		assertFailureContains(document(graph(1, "offer", missingNpc, terminal())),
			"npc-aggro-listed references missing NPC 203710");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			missingItem.replace(" item_id=\"182200002\"", ""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			missingNpc.replace(" npc_id=\"203710\"", ""), terminal()))));
	}

	/**
	 * 验证 movement 事件使用 world 复合引用闭包并保留完整协议 qualifier。
	 * Verifies movement events use world-composite reference closure and preserve full protocol qualifiers.
	 */
	@Test
	void compilerBuildsReferenceClosedMovementEvents() throws Exception {
		String windstream = transition("windstream", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>",
				"<windstream-entered world_id=\"210130000\" route_id=\"405001\"/>");
		CompiledQuestGraph.Event compiledWindstream = load(document(graph(1, "offer", windstream, terminal()))).graphs().get(1)
			.nodes().get("offer").transitions().getFirst().event();
		assertEquals(CompiledQuestGraph.EventType.WINDSTREAM_ENTERED, compiledWindstream.type());
		assertEquals(210130000, compiledWindstream.targetId());
		assertEquals("405001", compiledWindstream.qualifier());

		String flyingRing = transition("flying-ring", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>",
				"<flying-ring-passed world_id=\"210020000\" ring_name=\"ELTNEN_AIR_BOOSTER_1\"/>");
		CompiledQuestGraph.Event compiledRing = load(document(graph(1, "offer", flyingRing, terminal()))).graphs().get(1)
			.nodes().get("offer").transitions().getFirst().event();
		assertEquals(CompiledQuestGraph.EventType.FLYING_RING_PASSED, compiledRing.type());
		assertEquals(210020000, compiledRing.targetId());
		assertEquals("ELTNEN_AIR_BOOSTER_1", compiledRing.qualifier());

		assertFailureContains(document(graph(1, "offer",
			windstream.replace("405001", "406001"), terminal())),
			"windstream-entered references missing route 210130000/406001");
		assertFailureContains(document(graph(1, "offer",
			flyingRing.replace("ELTNEN_AIR_BOOSTER_1", "ELTNEN_AIR_BOOSTER_2"), terminal())),
			"flying-ring-passed references missing ring 210020000/ELTNEN_AIR_BOOSTER_2");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			windstream.replace(" world_id=\"210130000\"", ""), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			flyingRing.replace("ELTNEN_AIR_BOOSTER_1", "eltnen_air_booster_1"), terminal()))));
	}

	/** 验证 skill-use 事件强制 skill 引用与显式重复策略。 / Verifies skill-use events enforce skill references and explicit duplicate policy. */
	@Test
	void compilerBuildsReferenceClosedSkillUseEvents() throws Exception {
		String rawSkill = transition("skill-raw", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>",
				"<skill-used skill_id=\"9832\" duplicate_policy=\"RAW_SOURCE\"/>");
		CompiledQuestGraph.Event compiledRaw = load(document(graph(1, "offer", rawSkill, terminal()))).graphs().get(1)
			.nodes().get("offer").transitions().getFirst().event();
		assertEquals(CompiledQuestGraph.EventType.SKILL_USED, compiledRaw.type());
		assertEquals(9832, compiledRaw.targetId());
		assertEquals("RAW_SOURCE", compiledRaw.qualifier());

		String legacySkill = rawSkill.replace("RAW_SOURCE", "LEGACY_500_MILLIS");
		CompiledQuestGraph.Event compiledLegacy = load(document(graph(1, "offer", legacySkill, terminal()))).graphs().get(1)
			.nodes().get("offer").transitions().getFirst().event();
		assertEquals("LEGACY_500_MILLIS", compiledLegacy.qualifier());

		QuestGraphCompiler.References withoutSkill = new QuestGraphCompiler.References(Set.of(1), Set.of(203709), Set.of(182200001),
			Set.of(42), Set.of("TEST_ZONE"), Set.of(913), Set.of(), Set.of(), Set.of());
		assertFailureContains(document(graph(1, "offer", rawSkill, terminal())), withoutSkill,
			"skill-used references missing skill 9832");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			rawSkill.replace("RAW_SOURCE", "SILENT_FALLBACK"), terminal()))));
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			rawSkill.replace(" duplicate_policy=\"RAW_SOURCE\"", ""), terminal()))));
	}

	/**
	 * 验证 daily、weekly 与 anchored cooldown 被编译为封闭的强类型策略。
	 * Verifies that daily, weekly, and anchored cooldown compile into closed typed policies.
	 */
	@Test
	void compilerBuildsTypedRepeatDeadlinePolicies() throws Exception {
		assertEquals(new CompiledQuestGraph.DailyRepeatDeadlinePolicy(CompiledQuestGraph.RepeatTimeBasis.SERVER_LOCAL, 9),
			repeatFinish(load(document(graph(1, "offer", repeatTransition(
				"repeat_kind=\"DAILY\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\"",
				"repeat_kind=\"DAILY\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\""), terminal())))).repeatDeadlinePolicy());
		assertEquals(new CompiledQuestGraph.WeeklyRepeatDeadlinePolicy(CompiledQuestGraph.RepeatTimeBasis.SERVER_LOCAL,
			Set.of(CompiledQuestGraph.RepeatWeekday.MON, CompiledQuestGraph.RepeatWeekday.WED), 9),
			repeatFinish(load(document(graph(1, "offer", repeatTransition(
				"repeat_kind=\"WEEKLY\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\" weekdays=\"MON WED\"",
				"repeat_kind=\"WEEKLY\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\" weekdays=\"MON WED\""), terminal()))))
				.repeatDeadlinePolicy());
		assertEquals(new CompiledQuestGraph.AnchoredCooldownRepeatDeadlinePolicy(CompiledQuestGraph.RepeatTimeBasis.SERVER_LOCAL, 2_592_000, 9),
			repeatFinish(load(document(graph(1, "offer", repeatTransition(
				"repeat_kind=\"ANCHORED_COOLDOWN\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\" cooldown_seconds=\"2592000\"",
				"repeat_kind=\"ANCHORED_COOLDOWN\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\" cooldown_seconds=\"2592000\""),
				terminal())))).repeatDeadlinePolicy());
	}

	/**
	 * 验证缺字段、冲突字段、重复 weekday 与不配对提示都会阻断编译。
	 * Verifies missing fields, conflicting fields, duplicate weekdays, and unpaired messages block compilation.
	 */
	@Test
	void compilerRejectsInvalidRepeatDeadlineProtocols() {
		String daily = "repeat_kind=\"DAILY\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\"";
		String cooldown = "repeat_kind=\"ANCHORED_COOLDOWN\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\" cooldown_seconds=\"3600\"";
		assertFailureContains(document(graph(1, "offer", repeatTransition(
			"repeat_kind=\"DAILY\" time_basis=\"SERVER_LOCAL\"", daily), terminal())), "repeat policy is incomplete");
		assertFailureContains(document(graph(1, "offer", repeatTransition(daily + " cooldown_seconds=\"3600\"", daily), terminal())),
			"conflicting fields");
		String duplicateWeekday = "repeat_kind=\"WEEKLY\" time_basis=\"SERVER_LOCAL\" reset_hour=\"9\" weekdays=\"MON MON\"";
		assertFailureContains(document(graph(1, "offer", repeatTransition(duplicateWeekday, duplicateWeekday), terminal())),
			"duplicate weekdays");
		assertFailureContains(document(graph(1, "offer", repeatTransition(daily, daily)
			.replace("<finish-quest reward_index=\"0\" " + daily + "/>", ""), terminal())), "repeat protocol without finish");
		assertFailureContains(document(graph(1, "offer", repeatTransition(daily, cooldown), terminal())),
			"mismatched repeat deadline protocol");
		String messageOnly = transition("message-only", 10, "done").replace("<finish-quest reward_index=\"0\"/>",
			"<finish-quest reward_index=\"0\"/><send-repeat-deadline-message " + daily + "/>");
		assertFailureContains(document(graph(1, "offer", messageOnly, terminal())), "repeat protocol without deadline");
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
		String terminalTransition = transition("repeat", 20, "offer")
			.replace("<quest-status op=\"IN\" values=\"NONE\"/>",
				"<quest-status op=\"IN\" values=\"COMPLETE\"/>");
		loadUnchecked(document(graph(1, "offer", transition("accept", 10, "done"),
			"<node id=\"done\" terminal=\"true\">" + terminalTransition + "</node>")));
		String invalidTerminalTransition = transition("invalid", 20, "offer")
			.replace("<quest-repeat-available max_completions=\"255\" requires_deadline=\"true\" expected=\"true\"/>", "");
		assertFailureContains(document(graph(1, "offer", transition("accept", 10, "done"),
			"<node id=\"done\" terminal=\"true\">" + invalidTerminalTransition + "</node>")),
			"transition without repeat eligibility or a guarded protocol self-loop");
		String terminalProtocolLoop = """
			<transition id="close" priority="30" to="done">
				<dialog npc_id="203709" dialog="USE_OBJECT"/>
				<conditions>
					<quest-status op="IN" values="COMPLETE"/>
					<quest-repeat-available max_completions="255" requires_deadline="true" expected="false"/>
				</conditions>
				<actions><close-dialog/></actions>
			</transition>
			""";
		loadUnchecked(document(graph(1, "offer", transition("accept", 10, "done"),
			"<node id=\"done\" terminal=\"true\">" + terminalProtocolLoop + "</node>")));
		assertFailureContains(document(graph(1, "offer", transition("accept", 10, "done"),
			"<node id=\"done\" terminal=\"true\">" + terminalProtocolLoop.replace("<close-dialog/>", "<start-quest/>") + "</node>")),
			"transition without repeat eligibility or a guarded protocol self-loop");
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
				new QuestGraphCompiler.References(Set.of(), Set.of(203709), Set.of(182200001), Set.of(42), Set.of("TEST_ZONE"), Set.of(913))));
		assertCauseContains(missingQuest, "references missing quest 1");
		IllegalArgumentException missingNpc = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", transition("accept", 10, "done"), terminal())),
				new QuestGraphCompiler.References(Set.of(1), Set.of(), Set.of(182200001), Set.of(42), Set.of("TEST_ZONE"), Set.of(913))));
		assertCauseContains(missingNpc, "references missing NPC 203709");
		String killTransition = transition("kill", 10, "done")
			.replace("<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>", "<kill npc_id=\"203709\"/>");
		IllegalArgumentException missingKillNpc = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", killTransition, terminal())),
				new QuestGraphCompiler.References(Set.of(1), Set.of(), Set.of(182200001), Set.of(42), Set.of("TEST_ZONE"), Set.of(913))));
		assertCauseContains(missingKillNpc, "kill references missing NPC 203709");
		IllegalArgumentException missingTitle = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", transition("accept", 10, "done"), terminal())),
				new QuestGraphCompiler.References(Set.of(1, 2), Set.of(203709), Set.of(182200001), Set.of(), Set.of("TEST_ZONE"), Set.of(913))));
		assertCauseContains(missingTitle, "references missing title 42");
		IllegalArgumentException missingItem = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", transition("accept", 10, "done"), terminal())),
				new QuestGraphCompiler.References(Set.of(1, 2), Set.of(203709), Set.of(), Set.of(42), Set.of("TEST_ZONE"), Set.of(913))));
		assertCauseContains(missingItem, "references missing item 182200001");
		String missingGiveItem = transition("accept", 10, "done")
			.replace("<give-quest-item item_id=\"182200001\"", "<give-quest-item item_id=\"182200002\"");
		IllegalArgumentException missingGive = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", missingGiveItem, terminal()))));
		assertCauseContains(missingGive, "give action references missing item 182200002");
		String missingRemoveItem = transition("accept", 10, "done")
			.replace("<remove-quest-item item_id=\"182200001\"", "<remove-quest-item item_id=\"182200002\"");
		IllegalArgumentException missingRemove = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", missingRemoveItem, terminal()))));
		assertCauseContains(missingRemove, "remove action references missing item 182200002");
		IllegalArgumentException missingPrerequisiteQuest = assertThrows(IllegalArgumentException.class,
			() -> load(document(graph(1, "offer", transition("accept", 10, "done"), terminal())),
				new QuestGraphCompiler.References(Set.of(1), Set.of(203709), Set.of(182200001), Set.of(42), Set.of("TEST_ZONE"), Set.of(913))));
		assertCauseContains(missingPrerequisiteQuest, "references missing quest 2");
	}

	/**
	 * 验证绕过 XSD 时 typed condition 自身仍拒绝非法值。
	 * Verifies that typed conditions reject invalid values even when XSD is bypassed.
	 */
	@Test
	void typedConditionsRejectInvalidDirectConstruction() {
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.QuestStatusCondition(null, ConditionOperation.EQUAL,
			Set.of(CompiledQuestGraph.QuestStatus.NONE)));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.QuestStatusCondition(null, ConditionOperation.IN, Set.of()));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.QuestRewardCondition(0, 0));
		assertThrows(IllegalArgumentException.class,
			() -> new CompiledQuestGraph.QuestCompletionCountCondition(1, ConditionOperation.IN, 1));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerLevelCondition(0, null));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerLevelCondition(10, 9));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.KillVictimLevelDeltaCondition(null, null));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.KillVictimLevelDeltaCondition(10, 9));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerRaceCondition(Set.of()));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerRaceCondition(Set.of(Race.NPC)));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerClassCondition(Set.of(PlayerClass.ALL)));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerGenderCondition(Gender.DUMMY));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerTitleCondition(0));
		assertThrows(NullPointerException.class, () -> new CompiledQuestGraph.PlayerAbyssRankCondition(null));
		assertThrows(IllegalArgumentException.class,
			() -> new CompiledQuestGraph.PlayerInventoryCondition(182200001, ConditionOperation.IN, 1));
		assertThrows(IllegalArgumentException.class,
			() -> new CompiledQuestGraph.PlayerInventoryCondition(182200001, ConditionOperation.EQUAL, -1));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayerEquippedCondition(0));
		assertThrows(IllegalArgumentException.class,
			() -> new CompiledQuestGraph.QuestVariableCondition("counter", ConditionOperation.IN, 0));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.QuestRepeatAvailableCondition(0, false, true));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.AddQuestVariableAction("counter", 0));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.GiveQuestItemAction(0, 1,
			CompiledQuestGraph.QuestItemGrantMode.TOP_UP_TO));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.RemoveQuestItemAction(1, 0,
			CompiledQuestGraph.QuestItemRemovalMode.EXACT));
		assertDoesNotThrow(() -> new CompiledQuestGraph.SetQuestStatusAction(CompiledQuestGraph.QuestStatus.COMPLETE));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.SetCompletionCountAction(-1));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.AddCompletionCountAction(0));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.StartQuestTimerAction("QUEST_TIMER", 0));
		assertThrows(IllegalArgumentException.class, () -> new CompiledQuestGraph.PlayMovieAction(0));
	}

	/**
	 * 验证击杀事件等级差条件只读取 KILL_IN_WORLD 快照，并拒绝空区间、反向区间和错误事件。
	 * Verifies that the kill-victim level-delta condition only reads KILL_IN_WORLD snapshots and rejects empty, reversed,
	 * or wrong-event definitions.
	 */
	@Test
	void compilerClosesKillVictimLevelDeltaCondition() throws Exception {
		String transition = """
			<transition id="pvp-level" priority="1" to="done">
				<kill-in-world world_id="0"/>
				<conditions><kill-victim-level-delta min="-5" max="9"/></conditions>
			</transition>
			""";
		CompiledQuestGraph.Condition condition = load(document(graph(1, "active", transition, terminal()))).graphs().get(1)
			.nodes().get("active").transitions().getFirst().conditions().getFirst();
		assertEquals(new CompiledQuestGraph.KillVictimLevelDeltaCondition(-5, 9), condition);

		IllegalArgumentException wrongEvent = assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "active",
			transition.replace("<kill-in-world world_id=\"0\"/>", "<dialog npc_id=\"203709\" dialog=\"QUEST_SELECT\"/>"), terminal()))));
		assertCauseContains(wrongEvent, "uses kill-victim level delta outside KILL_IN_WORLD");
		IllegalArgumentException missingBounds = assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "active",
			transition.replace(" min=\"-5\" max=\"9\"", ""), terminal()))));
		assertCauseContains(missingBounds, "invalid kill-victim level delta range");
		IllegalArgumentException reversedBounds = assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "active",
			transition.replace("min=\"-5\" max=\"9\"", "min=\"10\" max=\"9\""), terminal()))));
		assertCauseContains(reversedBounds, "invalid kill-victim level delta range");
	}

	/**
	 * 验证交互资格只能编译为引用闭合的 actionless self-loop，影片动作属于引用闭合的 post-commit 协议。
	 * Verifies that interaction eligibility compiles only as a reference-closed actionless self-loop and that movie
	 * actions are reference-closed post-commit protocol.
	 */
	@Test
	void compilerClosesInteractionEligibilityAndPlayMovieContracts() throws Exception {
		String eligibility = """
			<transition id="can-act" priority="1" to="offer">
				<interaction-eligibility object_id="203709" action="ACTION_ITEM_USE"/>
				<conditions><quest-status op="IN" values="START"/></conditions>
			</transition>
			""";
		String movie = transition("movie", 10, "done")
			.replace("<send-dialog dialog_id=\"5\"/>", "<play-movie movie_id=\"913\"/>");
		CompiledQuestGraph graph = load(document(graph(1, "offer", eligibility + movie, terminal()))).graphs().get(1);
		CompiledQuestGraph.Transition query = graph.nodes().get("offer").transitions().getFirst();
		assertEquals(CompiledQuestGraph.EventType.INTERACTION_ELIGIBILITY, query.event().type());
		assertEquals(203709, query.event().targetId());
		assertEquals("ACTION_ITEM_USE", query.event().qualifier());
		assertTrue(query.actions().isEmpty());
		CompiledQuestGraph.Action movieAction = graph.nodes().get("offer").transitions().get(1).actions().stream()
			.filter(action -> action.type() == CompiledQuestGraph.ActionType.PLAY_MOVIE).findFirst().orElseThrow();
		assertEquals(CompiledQuestGraph.ActionPhase.POST_COMMIT_PROTOCOL, movieAction.type().phase());

		assertFailureContains(document(graph(1, "offer", eligibility.replace("203709", "203710") + movie, terminal())),
			"interaction eligibility references missing NPC 203710");
		assertFailureContains(document(graph(1, "offer", eligibility.replace("to=\"offer\"", "to=\"done\"") + movie, terminal())),
			"must be an actionless self-loop");
		assertFailureContains(document(graph(1, "offer",
			eligibility.replace("</transition>", "<actions><play-movie movie_id=\"913\"/></actions></transition>") + movie, terminal())),
			"must be an actionless self-loop");
		assertFailureContains(document(graph(1, "offer", eligibility + movie.replace("913", "914"), terminal())),
			"play-movie action references missing movie 914");
		assertThrows(IllegalArgumentException.class, () -> load(document(graph(1, "offer",
			eligibility.replace("ACTION_ITEM_USE", "UNPROVEN_ACTION") + movie, terminal()))));
	}

	private CompiledQuestGraphData load(String xml) throws Exception {
		return load(xml, REFERENCES);
	}

	private CompiledQuestGraphData load(String xml, QuestGraphCompiler.References references) throws Exception {
		Path file = tempDir.resolve("graphs-" + fileIndex++ + ".xml");
		Files.writeString(file, xml, StandardCharsets.UTF_8);
		return QuestGraphCompiler.load(file, SCHEMA, references);
	}

	/** 编译预期成功的内联 XML，并把检查异常提升为断言失败。 / Compiles inline XML expected to succeed and promotes checked errors. */
	private CompiledQuestGraphData loadUnchecked(String xml) {
		try {
			return load(xml);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void assertFailureContains(String xml, String message) {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> load(xml));
		assertCauseContains(error, message);
	}

	/** 使用显式引用集合断言编译失败原因。 / Asserts a compiler failure using an explicit reference set. */
	private void assertFailureContains(String xml, QuestGraphCompiler.References references, String message) {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> load(xml, references));
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

	/**
	 * 从待编译 XML 收集 compiler 所需的引用集合；真实静态数据存在性由生成器的引用闭包门禁独立证明。
	 * Collects compiler reference sets from the XML; the generator independently proves existence in authoritative static data.
	 */
	private static QuestGraphCompiler.References referencesDeclaredIn(Path file) throws Exception {
		Set<Integer> questIds = new HashSet<>();
		Set<Integer> npcIds = new HashSet<>();
		Set<Integer> itemIds = new HashSet<>();
		Set<Integer> titleIds = new HashSet<>();
		Set<String> zoneNames = new HashSet<>();
		Set<Integer> movieIds = new HashSet<>();
		Set<Integer> skillIds = new HashSet<>();
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (var stream = Files.newInputStream(file)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
			try {
				while (reader.hasNext()) {
					if (reader.next() != XMLStreamConstants.START_ELEMENT) {
						continue;
					}
					addIntegerAttribute(reader, "quest_id", questIds);
					addIntegerAttribute(reader, "npc_id", npcIds);
					addIntegerAttribute(reader, "object_id", npcIds);
					addIntegerAttribute(reader, "item_id", itemIds);
					addIntegerAttribute(reader, "skill_id", skillIds);
					addIntegerAttribute(reader, "title_id", titleIds);
					addStringAttribute(reader, "zone_name", zoneNames);
					addIntegerAttribute(reader, "movie_id", movieIds);
				}
			} finally {
				reader.close();
			}
		}
		return new QuestGraphCompiler.References(questIds, npcIds, itemIds, titleIds, zoneNames, movieIds, Set.of(), Set.of(), skillIds);
	}

	/** 添加存在的正整数 XML 属性。 / Adds a present positive-integer XML attribute. */
	private static void addIntegerAttribute(XMLStreamReader reader, String name, Set<Integer> target) {
		String value = reader.getAttributeValue(null, name);
		if (value != null) {
			target.add(Integer.parseInt(value));
		}
	}

	/** 添加存在的字符串 XML 属性。 / Adds a present string XML attribute. */
	private static void addStringAttribute(XMLStreamReader reader, String name, Set<String> target) {
		String value = reader.getAttributeValue(null, name);
		if (value != null) {
			target.add(value);
		}
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
			<conditions>
				<quest-status op="IN" values="NONE"/>
				<quest-status quest_id="2" op="IN" values="COMPLETE"/>
				<quest-variable variable="counter" op="EQUAL" value="0"/>
				<quest-repeat-available max_completions="255" requires_deadline="true" expected="true"/>
				<quest-collect-items/>
				<quest-reward quest_id="2" reward_index="1"/>
				<quest-completion-count quest_id="2" op="EQUAL" count="3"/>
				<player-level min="10" max="55"/>
				<player-race values="ELYOS ASMODIANS"/>
				<player-class values="GLADIATOR TEMPLAR"/>
				<player-gender value="MALE"/>
				<player-title title_id="42"/>
				<player-abyss-rank minimum="10"/>
				<player-inventory item_id="182200001" op="GREATER_EQUAL" count="1"/>
				<player-equipped item_id="182200001"/>
			</conditions>
				<actions>
					<start-quest/>
					<set-quest-variable variable="counter" value="1"/>
					<add-quest-variable variable="counter" delta="1"/>
					<set-quest-status status="REWARD"/>
					<give-quest-item item_id="182200001" count="3" mode="TOP_UP_TO"/>
					<remove-quest-item item_id="182200001" count="1" mode="EXACT"/>
					<remove-collected-items/>
					<finish-quest reward_index="0"/>
					<sync-quest-status/>
					<send-dialog dialog_id="5"/>
					<close-dialog/>
					<show-quest-list/>
					<send-player-message text="Missing item" channel="BRIGHT_YELLOW_CENTER"/>
				</actions>
			</transition>
			""".formatted(id, priority, target);
	}

	/** 创建带 finish/message 对的 repeat 转换。 / Creates a repeat transition with a paired finish/message protocol. */
	private static String repeatTransition(String finishAttributes, String messageAttributes) {
		return transition("repeat-finish", 10, "done")
			.replace("<finish-quest reward_index=\"0\"/>", "<finish-quest reward_index=\"0\" " + finishAttributes + "/>")
			.replace("<sync-quest-status/>",
				"<sync-quest-status/><send-repeat-deadline-message " + messageAttributes + "/>");
	}

	/** 返回已编译转换中的唯一 finish action。 / Returns the only compiled finish action in the transition. */
	private static CompiledQuestGraph.FinishQuestAction repeatFinish(CompiledQuestGraphData data) {
		return data.graphs().get(1).nodes().get("offer").transitions().getFirst().actions().stream()
			.filter(CompiledQuestGraph.FinishQuestAction.class::isInstance)
			.map(CompiledQuestGraph.FinishQuestAction.class::cast)
			.findFirst().orElseThrow();
	}

	private static String terminal() {
		return "<node id=\"done\" terminal=\"true\"/>";
	}
}
