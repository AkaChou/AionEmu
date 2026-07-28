package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.AttackEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.HouseItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemEquippedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemObtainedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillInWorldEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.PlayerDeathEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ZoneEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ZoneLeftEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ZoneMissionEndedEvent;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

class QuestGraphRouterTest {

	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");

	@TempDir
	Path tempDir;
	private QuestGraphRouter router;

	@BeforeEach
	void loadGraphs() throws Exception {
		Path xml = tempDir.resolve("graphs.xml");
		Files.writeString(xml, document(), StandardCharsets.UTF_8);
		CompiledQuestGraphData data = QuestGraphCompiler.load(xml, SCHEMA,
			new QuestGraphCompiler.References(Set.of(1, 2), Set.of(100), Set.of(182200001), Set.of(), Set.of("TEST_ZONE")));
		router = new QuestGraphRouter(data);
	}

	@Test
	void dialogUsesStablePriorityAndStopsOnFirstNonMatchResult() {
		List<Integer> visited = new ArrayList<>();
		DispatchResult result = router.dispatch(new DialogEvent("dialog-1", 7, 1000, 100, "QUEST_SELECT"),
			new PlayerQuestGraphStateList(), match -> {
				visited.add(match.route().questId());
				return match.route().questId() == 2 ? Status.NO_MATCH : Status.APPLIED;
			});

		assertEquals(List.of(2, 1), visited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), result);
		visited.clear();
		result = router.dispatch(new DialogEvent("dialog-2", 7, 1001, 100, "QUEST_SELECT"),
			new PlayerQuestGraphStateList(), match -> {
				visited.add(match.route().questId());
				return Status.REJECTED;
			});
		assertEquals(List.of(2), visited);
		assertEquals(new DispatchResult(Status.REJECTED, Propagation.STOP), result);
	}

	@Test
	void dialogPayloadAndInvalidStateBlockEvaluation() {
		DispatchResult noMatch = router.dispatch(new DialogEvent("dialog-3", 7, 1002, 100, "QUEST_ACCEPT"),
			new PlayerQuestGraphStateList(), match -> {
				throw new AssertionError("Mismatched dialog must not reach evaluator");
			});
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE), noMatch);

		List<PlayerQuestGraphState> blockedStates = List.of(
			new PlayerQuestGraphState(2, 2, 0, "start", QuestStatus.START, QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null,
				Map.of(), null),
			new PlayerQuestGraphState(2, 1, 0, "missing", QuestStatus.START, QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null,
				Map.of(), null),
			new PlayerQuestGraphState(2, 1, 0, "start", QuestStatus.NONE, QuestHistory.EMPTY, null, Lifecycle.PREPARED, Map.of(), Map.of(),
				new PreparedTransition(-1, "event", "dialog-q2", 0, new byte[0]), Map.of(), null),
			new PlayerQuestGraphState(2, 1, 0, "start", QuestStatus.START, QuestHistory.EMPTY, null, Lifecycle.QUARANTINED, Map.of(), Map.of(), null,
				Map.of(), "blocked"));
		for (PlayerQuestGraphState blockedState : blockedStates) {
			PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
			states.addLoaded(blockedState);
			DispatchResult failed = router.dispatch(new DialogEvent("dialog-4", 7, 1003, 100, "QUEST_SELECT"), states,
				match -> {
					throw new AssertionError("Blocked state must not reach evaluator");
				});
			assertEquals(new DispatchResult(Status.FAILED, Propagation.STOP), failed);
		}
	}

	@Test
	void killBroadcastIsolatesFailuresAndContinuesOtherQuests() {
		List<String> visited = new ArrayList<>();
		DispatchResult result = router.dispatch(new KillEvent("kill-1", 7, 2000, 100), new PlayerQuestGraphStateList(), match -> {
			visited.add(match.route().transition().id());
			if (match.route().questId() == 2) {
				throw new IllegalStateException("isolated failure");
			}
			return match.route().transition().id().equals("kill-first") ? Status.NO_MATCH : Status.APPLIED;
		});

		assertEquals(List.of("kill-q2", "kill-first", "kill-second"), visited);
		assertEquals(new DispatchResult(Status.FAILED, Propagation.CONTINUE), result);
	}

	@Test
	void killBroadcastStopsRemainingCandidatesForConcludedQuest() {
		List<String> visited = new ArrayList<>();
		DispatchResult result = router.dispatch(new KillEvent("kill-2", 7, 2001, 100), new PlayerQuestGraphStateList(), match -> {
			visited.add(match.route().transition().id());
			return Status.APPLIED;
		});

		assertEquals(List.of("kill-q2", "kill-first"), visited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.CONTINUE), result);
	}

	@Test
	void eventCodecRoundTripsAndRejectsCorruption() {
		DialogEvent dialog = new DialogEvent("dialog", 7, 3000, 100, "QUEST_SELECT");
		KillEvent kill = new KillEvent("kill", 8, 3001, 100);
		assertEquals(dialog, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(dialog)));
		assertEquals(kill, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(kill)));
		AttackEvent attack = new AttackEvent("attack", 7, 3002, 100, 40, 100);
		PlayerDeathEvent death = new PlayerDeathEvent("death", 7, 3003);
		KillInWorldEvent worldKill = new KillInWorldEvent("world-kill", 7, 3004, 400010000, 8, 65);
		ItemUseEvent itemUse = new ItemUseEvent("item-use", 7, 3005, 182200001, 5001);
		ItemObtainedEvent itemObtained = new ItemObtainedEvent("item-obtained", 7, 3006, 182200001);
		ItemEquippedEvent itemEquipped = new ItemEquippedEvent("item-equipped", 7, 3007, 182200001);
		HouseItemUseEvent houseItemUse = new HouseItemUseEvent("house-item-use", 7, 3008, 182200001);
		WorldEnteredEvent worldEntered = new WorldEnteredEvent("world-entered", 7, 3009, 210010000, 1, 10, 20, 30);
		ZoneEnteredEvent zoneEntered = new ZoneEnteredEvent("zone-entered", 7, 3010, "TEST_ZONE", 210010000, 1, 10, 20, 30);
		ZoneLeftEvent zoneLeft = new ZoneLeftEvent("zone-left", 7, 3011, "TEST_ZONE", 210010000, 1);
		ZoneMissionEndedEvent zoneMissionEnded = new ZoneMissionEndedEvent("zone-mission-ended", 7, 3012, 1);
		assertEquals(attack, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(attack)));
		assertEquals(death, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(death)));
		assertEquals(worldKill, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(worldKill)));
		assertEquals(itemUse, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(itemUse)));
		assertEquals(itemObtained, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(itemObtained)));
		assertEquals(itemEquipped, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(itemEquipped)));
		assertEquals(houseItemUse, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(houseItemUse)));
		assertEquals(worldEntered, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(worldEntered)));
		assertEquals(zoneEntered, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(zoneEntered)));
		assertEquals(zoneLeft, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(zoneLeft)));
		assertEquals(zoneMissionEnded, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(zoneMissionEnded)));

		byte[] encoded = QuestGraphEventCodec.encode(kill);
		byte[] unknown = Arrays.copyOf(encoded, encoded.length);
		unknown[4] = 99;
		assertThrows(IllegalArgumentException.class, () -> QuestGraphEventCodec.decode(unknown));
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphEventCodec.decode(Arrays.copyOf(encoded, encoded.length - 1)));
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphEventCodec.decode(Arrays.copyOf(encoded, encoded.length + 1)));
	}

	/**
	 * 验证 combat 事件广播、服务器快照和 world wildcard 路由。
	 * Verifies combat-event broadcast, server snapshots, and world-wildcard routing.
	 */
	@Test
	void combatEventsUseTypedBroadcastRoutes() {
		assertEquals(List.of("attack-q1"), visited(new AttackEvent("attack", 7, 4000, 100, 40, 100)));
		assertEquals(List.of("death-q1"), visited(new PlayerDeathEvent("death", 7, 4001)));
		assertEquals(List.of("world-wildcard-q2", "world-exact-q1"),
			visited(new KillInWorldEvent("world", 7, 4002, 400010000, 8, 65)));
		assertThrows(IllegalArgumentException.class, () -> new AttackEvent("attack", 7, 4003, 100, 101, 100));
		assertThrows(IllegalArgumentException.class, () -> new KillInWorldEvent("world", 7, 4004, 1, 7, 65));
	}

	/**
	 * 验证物品使用采用旧 HandlerResult 独占语义，其余 item/housing 事件固定广播。
	 * Verifies legacy HandlerResult exclusivity for item use and fixed broadcast for other item/housing events.
	 */
	@Test
	void itemAndHousingEventsUseFixedTypedRoutes() {
		List<Integer> visited = new ArrayList<>();
		DispatchResult itemUse = router.dispatch(new ItemUseEvent("item-use", 7, 5000, 182200001, 5001),
			new PlayerQuestGraphStateList(), match -> {
				visited.add(match.route().questId());
				return match.route().questId() == 2 ? Status.NO_MATCH : Status.APPLIED;
			});
		assertEquals(List.of(2, 1), visited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), itemUse);

		assertEquals(List.of("item-obtained-q2", "item-obtained-q1"),
			visited(new ItemObtainedEvent("item-obtained", 7, 5001, 182200001)));
		assertEquals(List.of("item-equipped-q1"),
			visited(new ItemEquippedEvent("item-equipped", 7, 5002, 182200001)));
		assertEquals(List.of("house-item-use-q2", "house-item-use-q1"),
			visited(new HouseItemUseEvent("house-item-use", 7, 5003, 182200001)));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new ItemUseEvent("missing-item", 7, 5004, 182200002, 5002),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertThrows(IllegalArgumentException.class, () -> new ItemUseEvent("invalid", 7, 5005, 182200001, 0));
		assertThrows(IllegalArgumentException.class, () -> new ItemObtainedEvent("invalid", 7, 5006, 0));
	}

	/**
	 * 验证 world/zone 事件固定广播，zone-mission 仅路由显式目标 owner。
	 * Verifies fixed broadcast for world/zone events and explicit owner targeting for zone-mission events.
	 */
	@Test
	void worldAndZoneEventsUseServerSnapshotsAndFixedRoutes() {
		assertEquals(List.of("world-entered-q2", "world-entered-q1"),
			visited(new WorldEnteredEvent("world-entered", 7, 6000, 210010000, 1, 10, 20, 30)));
		assertEquals(List.of("zone-entered-q2", "zone-entered-q1"),
			visited(new ZoneEnteredEvent("zone-entered", 7, 6001, "TEST_ZONE", 210010000, 1, 10, 20, 30)));
		assertEquals(List.of("zone-left-q2", "zone-left-q1"),
			visited(new ZoneLeftEvent("zone-left", 7, 6002, "TEST_ZONE", 210010000, 1)));
		assertEquals(List.of("zone-mission-ended-q1"),
			visited(new ZoneMissionEndedEvent("zone-mission-ended", 7, 6003, 1)));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new ZoneMissionEndedEvent("missing-owner", 7, 6004, 3),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertThrows(IllegalArgumentException.class,
			() -> new WorldEnteredEvent("invalid", 7, 6005, 0, 1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class,
			() -> new WorldEnteredEvent("invalid", 7, 6006, 210010000, 1, Float.NaN, 0, 0));
		assertThrows(IllegalArgumentException.class,
			() -> new ZoneEnteredEvent("invalid", 7, 6007, "lowercase", 210010000, 1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new ZoneMissionEndedEvent("invalid", 7, 6008, 0));
	}

	/** 返回广播访问的转换标识。 / Returns transition identifiers visited by a broadcast event. */
	private List<String> visited(QuestGraphEvent event) {
		List<String> visited = new ArrayList<>();
		DispatchResult result = router.dispatch(event, new PlayerQuestGraphStateList(), match -> {
			visited.add(match.route().transition().id());
			return Status.APPLIED;
		});
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.CONTINUE), result);
		return visited;
	}

	private static String document() {
		String questOneTransitions = String.join("", dialogTransition("dialog-q1", 20), killTransition("kill-first", 15),
			killTransition("kill-second", 25), combatTransition("attack-q1", 10, "<attack npc_id=\"100\"/>"),
			combatTransition("death-q1", 10, "<player-death/>"),
			combatTransition("world-exact-q1", 20, "<kill-in-world world_id=\"400010000\"/>"),
			itemTransition("item-use-q1", 20, "item-use"), itemTransition("item-obtained-q1", 20, "item-obtained"),
			itemTransition("item-equipped-q1", 20, "item-equipped"), itemTransition("house-item-use-q1", 20, "house-item-use"),
			worldZoneTransition("world-entered-q1", 20, "<world-entered/>"),
			worldZoneTransition("zone-entered-q1", 20, "<zone-entered zone_name=\"TEST_ZONE\"/>"),
			worldZoneTransition("zone-left-q1", 20, "<zone-left zone_name=\"TEST_ZONE\"/>"),
			worldZoneTransition("zone-mission-ended-q1", 20, "<zone-mission-ended/>"));
		String questTwoTransitions = String.join("", dialogTransition("dialog-q2", 10), killTransition("kill-q2", 5),
			combatTransition("world-wildcard-q2", 10, "<kill-in-world world_id=\"0\"/>"),
			itemTransition("item-use-q2", 10, "item-use"), itemTransition("item-obtained-q2", 10, "item-obtained"),
			itemTransition("house-item-use-q2", 10, "house-item-use"),
			worldZoneTransition("world-entered-q2", 10, "<world-entered/>"),
			worldZoneTransition("zone-entered-q2", 10, "<zone-entered zone_name=\"TEST_ZONE\"/>"),
			worldZoneTransition("zone-left-q2", 10, "<zone-left zone_name=\"TEST_ZONE\"/>"),
			worldZoneTransition("zone-mission-ended-q2", 10, "<zone-mission-ended/>"));
		return """
			<quest_graphs>
				<quest_graph quest_id="1" version="1" scope="PLAYER" initial_node="start">
					<node id="start">
						%s
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
				<quest_graph quest_id="2" version="1" scope="PLAYER" initial_node="start">
					<node id="start">
						%s
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""".formatted(questOneTransitions, questTwoTransitions);
	}

	private static String dialogTransition(String id, int priority) {
		return """
			<transition id="%s" priority="%d" to="done">
				<dialog npc_id="100" dialog="QUEST_SELECT"/>
			</transition>
			""".formatted(id, priority);
	}

	private static String killTransition(String id, int priority) {
		return """
			<transition id="%s" priority="%d" to="done">
				<kill npc_id="100"/>
			</transition>
			""".formatted(id, priority);
	}

	/** 创建聚焦 combat 路由转换。 / Creates a focused combat route transition. */
	private static String combatTransition(String id, int priority, String event) {
		return "<transition id=\"%s\" priority=\"%d\" to=\"done\">%s</transition>".formatted(id, priority, event);
	}

	/** 创建聚焦 item/housing 路由转换。 / Creates a focused item/housing route transition. */
	private static String itemTransition(String id, int priority, String event) {
		return combatTransition(id, priority, "<" + event + " item_id=\"182200001\"/>");
	}

	/** 创建聚焦 world/zone 路由转换。 / Creates a focused world/zone route transition. */
	private static String worldZoneTransition(String id, int priority, String event) {
		return combatTransition(id, priority, event);
	}
}
