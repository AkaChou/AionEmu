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
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.LevelUpEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.MovieEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.NpcProximityEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortReachedTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortLostTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.PlayerDeathEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.PlayerLogoutEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.QuestTimerEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.RankedPlayerKillEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DredgionSettledEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.CraftFailedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.NpcAggroListedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WindstreamEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.FlyingRingPassedEvent;
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
			new QuestGraphCompiler.References(Set.of(1, 2), Set.of(100), Set.of(182200001), Set.of(), Set.of("TEST_ZONE"), Set.of(913),
				Set.of(new QuestGraphCompiler.WindstreamRouteReference(210130000, 405)),
				Set.of(new QuestGraphCompiler.FlyingRingReference(210020000, "ELTNEN_AIR_BOOSTER_1"))));
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
		LevelUpEvent levelUp = new LevelUpEvent("level-up", 7, 3013, 66);
		PlayerLogoutEvent playerLogout = new PlayerLogoutEvent("player-logout", 7, 3014, 210010000, 1);
		QuestTimerEndedEvent timerEnded = new QuestTimerEndedEvent("timer-ended", 7, 3015, 1, "QUEST_TIMER", 3015);
		MovieEndedEvent movieEnded = new MovieEndedEvent("movie-ended", 7, 3016, 913, 4, 3000);
		NpcProximityEvent proximity = new NpcProximityEvent("proximity", 7, 3017, 100, 5001, 210010000, 1, 12.5f);
		EscortReachedTargetEvent reached = new EscortReachedTargetEvent("escort-reached", 7, 3018, 1, 100, 5001, 210010000, 1);
		EscortLostTargetEvent lost = new EscortLostTargetEvent("escort-lost", 7, 3019, 1, 100, 5001, 210010000, 1);
		RankedPlayerKillEvent rankedKill = new RankedPlayerKillEvent("ranked-kill", 7, 3020, 7, 8, 12, 210010000, 1, 5, true);
		DredgionSettledEvent dredgionSettled = new DredgionSettledEvent("dredgion-settled", 7, 3021, 400010000, 1);
		CraftFailedEvent craftFailed = new CraftFailedEvent("craft-failed", 7, 3022, 182200001, 0);
		NpcAggroListedEvent aggroListed = new NpcAggroListedEvent("aggro-listed", 7, 3023, 8, 100, 5001, 210010000, 1, 12.5f, true);
		WindstreamEnteredEvent windstreamEntered = new WindstreamEnteredEvent("windstream-entered", 7, 3024, 210130000, 1,
			405001, 405, 120, true, true, true);
		FlyingRingPassedEvent flyingRingPassed = new FlyingRingPassedEvent("flying-ring-passed", 7, 3025, 210020000, 1,
			"ELTNEN_AIR_BOOSTER_1", 6, 2.5f, true, true);
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
		assertEquals(levelUp, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(levelUp)));
		assertEquals(playerLogout, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(playerLogout)));
		assertEquals(timerEnded, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(timerEnded)));
		assertEquals(movieEnded, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(movieEnded)));
		assertEquals(proximity, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(proximity)));
		assertEquals(reached, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(reached)));
		assertEquals(lost, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(lost)));
		assertEquals(rankedKill, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(rankedKill)));
		assertEquals(dredgionSettled, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(dredgionSettled)));
		assertEquals(craftFailed, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(craftFailed)));
		assertEquals(aggroListed, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(aggroListed)));
		assertEquals(windstreamEntered, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(windstreamEntered)));
		assertEquals(flyingRingPassed, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(flyingRingPassed)));

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

	/**
	 * 验证升级与登出广播、计时器 owner/名称匹配，以及影片独占路由与权威快照。
	 * Verifies level/logout broadcast, timer owner/name matching, and exclusive movie routing with authority snapshots.
	 */
	@Test
	void lifecycleTimerAndMovieEventsUseFixedTypedRoutes() {
		assertEquals(List.of("level-up-q2", "level-up-q1"), visited(new LevelUpEvent("level", 7, 7000, 66)));
		assertEquals(List.of("player-logout-q2", "player-logout-q1"),
			visited(new PlayerLogoutEvent("logout", 7, 7001, 210010000, 1)));
		assertEquals(List.of("timer-ended-q1"),
			visited(new QuestTimerEndedEvent("timer", 7, 7002, 1, "QUEST_TIMER", 7000)));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new QuestTimerEndedEvent("wrong-timer", 7, 7003, 1, "OTHER_TIMER", 7000),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));

		List<Integer> movieOwners = new ArrayList<>();
		DispatchResult movie = router.dispatch(new MovieEndedEvent("movie", 7, 7004, 913, 1, 6990),
			new PlayerQuestGraphStateList(), match -> {
				movieOwners.add(match.route().questId());
				return Status.APPLIED;
			});
		assertEquals(List.of(2), movieOwners);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), movie);
		assertThrows(IllegalArgumentException.class, () -> new LevelUpEvent("invalid", 7, 7005, 0));
		assertThrows(IllegalArgumentException.class,
			() -> new PlayerLogoutEvent("invalid", 7, 7006, 210010000, 0));
		assertThrows(IllegalArgumentException.class,
			() -> new QuestTimerEndedEvent("invalid", 7, 6999, 1, "QUEST_TIMER", 7000));
		assertThrows(IllegalArgumentException.class,
			() -> new MovieEndedEvent("invalid", 7, 7007, 913, 0, 6990));
	}

	/**
	 * 验证邻近事件按 NPC 广播，护送结果只投递原任务 owner，并拒绝越界或无效快照。
	 * Verifies NPC-broadcast proximity, owner-targeted escort results, and rejection of invalid snapshots.
	 */
	@Test
	void npcSignalEventsUseServerSnapshotsAndFixedRoutes() {
		assertEquals(List.of("npc-proximity-q2", "npc-proximity-q1"),
			visited(new NpcProximityEvent("proximity", 7, 8000, 100, 5001, 210010000, 1, 12.5f)));
		assertEquals(List.of("escort-reached-q1"),
			visited(new EscortReachedTargetEvent("reached", 7, 8001, 1, 100, 5001, 210010000, 1)));
		assertEquals(List.of("escort-lost-q1"),
			visited(new EscortLostTargetEvent("lost", 7, 8002, 1, 100, 5001, 210010000, 1)));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new EscortReachedTargetEvent("missing-owner", 7, 8003, 3, 100, 5001, 210010000, 1),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertThrows(IllegalArgumentException.class,
			() -> new NpcProximityEvent("invalid", 7, 8004, 100, 5001, 210010000, 1, 20.1f));
		assertThrows(IllegalArgumentException.class,
			() -> new EscortLostTargetEvent("invalid", 7, 8005, 0, 100, 5001, 210010000, 1));
		assertThrows(IllegalArgumentException.class,
			() -> new EscortReachedTargetEvent("invalid", 7, 8006, 1, 100, 5001, 210010000, 0));
	}

	/**
	 * 验证军衔击杀合并全部已满足最低军衔的路由并稳定广播，Dredgion 结算固定广播。
	 * Verifies ranked kills merge every satisfied minimum-rank route with stable broadcast order and Dredgion
	 * settlement uses fixed broadcast.
	 */
	@Test
	void pvpSignalEventsUseRankRangesAndFixedBroadcast() {
		assertEquals(List.of("ranked-kill-q2", "ranked-kill-q1"),
			visited(new RankedPlayerKillEvent("ranked", 7, 9000, 7, 8, 12, 210010000, 1, 5, true)));
		assertEquals(List.of("dredgion-settled-q2", "dredgion-settled-q1"),
			visited(new DredgionSettledEvent("settled", 7, 9001, 400010000, 1)));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new RankedPlayerKillEvent("below-rank", 7, 9002, 7, 8, 4, 210010000, 1, 5, true),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertThrows(IllegalArgumentException.class,
			() -> new RankedPlayerKillEvent("invalid-rank", 7, 9003, 7, 8, 19, 210010000, 1, 5, true));
		assertThrows(IllegalArgumentException.class,
			() -> new RankedPlayerKillEvent("dead-recipient", 7, 9004, 7, 8, 12, 210010000, 1, 5, false));
		assertThrows(IllegalArgumentException.class,
			() -> new DredgionSettledEvent("invalid-instance", 7, 9005, 400010000, 0));
	}

	/**
	 * 验证制作失败采用固定独占路由，NPC 仇恨感知按 NPC 固定广播。
	 * Verifies fixed exclusive routing for craft failures and fixed NPC-keyed broadcast for aggro perception.
	 */
	@Test
	void craftAndAggroSignalsUseFixedTypedRoutes() {
		List<String> craftVisited = new ArrayList<>();
		DispatchResult craftResult = router.dispatch(new CraftFailedEvent("craft", 7, 10000, 182200001, 0),
			new PlayerQuestGraphStateList(), match -> {
				craftVisited.add(match.route().transition().id());
				return Status.APPLIED;
			});
		assertEquals(List.of("craft-failed-q2"), craftVisited);
		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), craftResult);
		assertEquals(List.of("aggro-listed-q2", "aggro-listed-q1"),
			visited(new NpcAggroListedEvent("aggro", 7, 10001, 8, 100, 5001, 210010000, 1, 12.5f, true)));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new CraftFailedEvent("missing-item", 7, 10002, 182200002, 0),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new NpcAggroListedEvent("missing-npc", 7, 10003, 8, 101, 5002, 210010000, 1, 12.5f, true),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertThrows(IllegalArgumentException.class,
			() -> new CraftFailedEvent("product-present", 7, 10004, 182200001, 1));
		assertThrows(IllegalArgumentException.class,
			() -> new NpcAggroListedEvent("boundary", 7, 10005, 8, 100, 5001, 210010000, 1, 50, true));
		assertThrows(IllegalArgumentException.class,
			() -> new NpcAggroListedEvent("unknown-recipient", 7, 10006, 8, 100, 5001, 210010000, 1, 12.5f, false));
	}

	/**
	 * 验证 movement 事件按 world 与完整 qualifier 广播，错误 world/route/ring 不匹配。
	 * Verifies movement events broadcast by world and full qualifier while wrong world, route, or ring does not match.
	 */
	@Test
	void movementSignalsUseCompositeWorldQualifiedRoutes() {
		assertEquals(List.of("windstream-entered-q2", "windstream-entered-q1"),
			visited(new WindstreamEnteredEvent("windstream", 7, 11000, 210130000, 1, 405001, 405, 120, true, true, true)));
		assertEquals(List.of("flying-ring-passed-q2", "flying-ring-passed-q1"),
			visited(new FlyingRingPassedEvent("ring", 7, 11001, 210020000, 1, "ELTNEN_AIR_BOOSTER_1", 6, 2, true, true)));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new WindstreamEnteredEvent("wrong-world", 7, 11002, 210140000, 1, 405001, 405, 120, true, true, true),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new WindstreamEnteredEvent("wrong-route", 7, 11003, 210130000, 1, 406001, 406, 120, true, true, true),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			router.dispatch(new FlyingRingPassedEvent("wrong-ring", 7, 11004, 210020000, 1, "ELTNEN_AIR_BOOSTER_2", 6, 2, true, true),
				new PlayerQuestGraphStateList(), match -> Status.APPLIED));
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
				worldZoneTransition("zone-mission-ended-q1", 20, "<zone-mission-ended/>"),
				worldZoneTransition("level-up-q1", 20, "<level-up/>"),
				worldZoneTransition("player-logout-q1", 20, "<player-logout/>"),
				worldZoneTransition("timer-ended-q1", 20, "<quest-timer-ended timer=\"QUEST_TIMER\"/>"),
				worldZoneTransition("movie-ended-q1", 20, "<movie-ended movie_id=\"913\"/>"),
				worldZoneTransition("npc-proximity-q1", 20, "<npc-proximity npc_id=\"100\"/>"),
				worldZoneTransition("escort-reached-q1", 20, "<escort-reached-target/>"),
				worldZoneTransition("escort-lost-q1", 20, "<escort-lost-target/>"),
				worldZoneTransition("ranked-kill-q1", 20, "<ranked-player-kill minimum_rank=\"10\"/>"),
				worldZoneTransition("dredgion-settled-q1", 20, "<dredgion-settled/>"),
				worldZoneTransition("craft-failed-q1", 20, "<craft-failed item_id=\"182200001\"/>"),
				worldZoneTransition("aggro-listed-q1", 20, "<npc-aggro-listed npc_id=\"100\"/>"),
				worldZoneTransition("windstream-entered-q1", 20, "<windstream-entered world_id=\"210130000\" route_id=\"405001\"/>"),
				worldZoneTransition("flying-ring-passed-q1", 20,
					"<flying-ring-passed world_id=\"210020000\" ring_name=\"ELTNEN_AIR_BOOSTER_1\"/>"));
		String questTwoTransitions = String.join("", dialogTransition("dialog-q2", 10), killTransition("kill-q2", 5),
			combatTransition("world-wildcard-q2", 10, "<kill-in-world world_id=\"0\"/>"),
			itemTransition("item-use-q2", 10, "item-use"), itemTransition("item-obtained-q2", 10, "item-obtained"),
			itemTransition("house-item-use-q2", 10, "house-item-use"),
			worldZoneTransition("world-entered-q2", 10, "<world-entered/>"),
				worldZoneTransition("zone-entered-q2", 10, "<zone-entered zone_name=\"TEST_ZONE\"/>"),
				worldZoneTransition("zone-left-q2", 10, "<zone-left zone_name=\"TEST_ZONE\"/>"),
				worldZoneTransition("zone-mission-ended-q2", 10, "<zone-mission-ended/>"),
				worldZoneTransition("level-up-q2", 10, "<level-up/>"),
				worldZoneTransition("player-logout-q2", 10, "<player-logout/>"),
				worldZoneTransition("timer-ended-q2", 10, "<quest-timer-ended timer=\"QUEST_TIMER\"/>"),
				worldZoneTransition("movie-ended-q2", 10, "<movie-ended movie_id=\"913\"/>"),
				worldZoneTransition("npc-proximity-q2", 10, "<npc-proximity npc_id=\"100\"/>"),
				worldZoneTransition("escort-reached-q2", 10, "<escort-reached-target/>"),
				worldZoneTransition("escort-lost-q2", 10, "<escort-lost-target/>"),
				worldZoneTransition("ranked-kill-q2", 10, "<ranked-player-kill minimum_rank=\"5\"/>"),
				worldZoneTransition("dredgion-settled-q2", 10, "<dredgion-settled/>"),
				worldZoneTransition("craft-failed-q2", 10, "<craft-failed item_id=\"182200001\"/>"),
				worldZoneTransition("aggro-listed-q2", 10, "<npc-aggro-listed npc_id=\"100\"/>"),
				worldZoneTransition("windstream-entered-q2", 10, "<windstream-entered world_id=\"210130000\" route_id=\"405001\"/>"),
				worldZoneTransition("flying-ring-passed-q2", 10,
					"<flying-ring-passed world_id=\"210020000\" ring_name=\"ELTNEN_AIR_BOOSTER_1\"/>"));
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
