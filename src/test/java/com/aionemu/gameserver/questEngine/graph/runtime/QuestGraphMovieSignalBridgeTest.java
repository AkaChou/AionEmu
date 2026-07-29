package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.model.gameobjects.player.MoviePlaybackAuthority;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.MovieEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 验证影片播放 authority 到正式任务图分发的 typed 边界、恢复和漂移失败合同。
 * Verifies the typed movie-authority boundary into formal graph dispatch, recovery, and drift failures.
 */
class QuestGraphMovieSignalBridgeTest {

	private static final Path SCHEMA = Path.of("src/main/resources/aion/data/static_data/quest_graph_data/quest_graph_data.xsd");
	private static final Path MOVIE_END_PACKET = Path.of(
		"src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_PLAY_MOVIE_END.java");
	private static final Path QUEST_ENGINE = Path.of("src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java");
	private static final ZoneId SERVER_ZONE = ZoneId.of("UTC");

	@TempDir
	Path tempDir;

	/** 验证只有匹配的一次性服务端凭据能生成完整 typed 事件。 / Verifies only matching one-time server authority creates a complete typed event. */
	@Test
	void createsTypedEventOnlyFromMatchingConsumedAuthority() {
		MoviePlaybackAuthority authority = new MoviePlaybackAuthority();
		assertTrue(authority.complete(913, 1001).isEmpty());
		MoviePlaybackAuthority.Playback playback = authority.begin(913, 1000);

		assertTrue(authority.complete(914, 1001).isEmpty());
		assertTrue(authority.complete(913, 999).isEmpty());
		MovieEndedEvent event = QuestGraphMovieSignalBridge.fromPlayback(7, 1001,
			authority.complete(913, 1001).orElseThrow());

		assertEquals("movie-end-7-" + playback.playbackId() + "-1000", event.eventId());
		assertEquals(7, event.playerId());
		assertEquals(913, event.movieId());
		assertEquals(playback.playbackId(), event.playbackId());
		assertEquals(1000, event.startedAt());
		assertEquals(1001, event.occurredAt());
		assertTrue(authority.complete(913, 1002).isEmpty());
	}

	/** 验证登出/重启清理旧凭据，codec 恢复不丢失播放身份。 / Verifies logout/restart clear old authority while codec recovery retains playback identity. */
	@Test
	void logoutRestartAndCodecPreserveTheAuthorityContract() {
		MoviePlaybackAuthority authority = new MoviePlaybackAuthority();
		authority.begin(913, 2000);
		authority.clear();
		assertTrue(authority.complete(913, 2001).isEmpty());

		MoviePlaybackAuthority restartedAuthority = new MoviePlaybackAuthority();
		assertTrue(restartedAuthority.complete(913, 2002).isEmpty());
		MoviePlaybackAuthority.Playback playback = restartedAuthority.begin(913, 2003);
		MovieEndedEvent event = QuestGraphMovieSignalBridge.fromPlayback(7, 2004,
			restartedAuthority.complete(913, 2004).orElseThrow());
		MovieEndedEvent recovered = (MovieEndedEvent) QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(event));

		assertEquals(playback.playbackId(), recovered.playbackId());
		assertEquals(playback.startedAt(), recovered.startedAt());
		assertEquals(event, recovered);
	}

	/** 验证生产包保留完整播放凭据并经 typed 入口投影到旧 owner。 / Verifies production retains full playback authority and projects the typed entry to the legacy owner. */
	@Test
	void productionPacketUsesTypedEntryWithoutSwitchingOwner() throws Exception {
		String packet = Files.readString(MOVIE_END_PACKET);
		String questEngine = Files.readString(QUEST_ENGINE);

		assertTrue(packet.contains(
			"Playback playback = player.getMoviePlaybackAuthority().complete(movieId, endedAt).orElse(null);"));
		assertTrue(packet.contains("QuestGraphMovieSignalBridge.fromPlayback(player.getObjectId(), endedAt, playback)"));
		assertTrue(packet.contains("onMovieEnd(new QuestEnv(null, player, 0, 0), movieEnded)"));
		assertTrue(questEngine.contains("public boolean onMovieEnd(QuestEnv env, MovieEndedEvent event)"));
		assertTrue(questEngine.contains("return onMovieEnd(env, event.movieId());"));
	}

	/** 验证正式 registry/router/executor 链只应用一次匹配影片转换。 / Verifies the formal registry/router/executor chain applies one matching movie transition only. */
	@Test
	void dispatchesThroughCurrentDefinitionsAndRejectsReplay() throws Exception {
		CompiledQuestGraphData data = graphData();
		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		registry.installInitial(data);
		QuestGraphMovieSignalBridge bridge = new QuestGraphMovieSignalBridge(registry, new QuestGraphTransitionExecutor());
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		TransitionContext context = context(7, states);
		MovieEndedEvent event = new MovieEndedEvent("movie-end-7-1-3000", 7, 3001, 913, 1, 3000);

		assertEquals(new DispatchResult(Status.APPLIED, Propagation.STOP), bridge.dispatch(event, context));
		assertEquals("done", states.get(1).getNodeId());
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE), bridge.dispatch(event, context));
		assertEquals(new DispatchResult(Status.NO_MATCH, Propagation.CONTINUE),
			bridge.dispatch(new MovieEndedEvent("wrong-movie", 7, 3002, 914, 2, 3000), context));
	}

	/** 验证未初始化定义、玩家漂移和 owner 删除均失败关闭。 / Verifies cold definitions, player drift, and owner removal fail closed. */
	@Test
	void rejectsColdRegistryPlayerDriftAndOwnerRemoval() throws Exception {
		MovieEndedEvent event = new MovieEndedEvent("movie-end-7-1-4000", 7, 4001, 913, 1, 4000);
		QuestGraphDefinitionRegistry coldRegistry = new QuestGraphDefinitionRegistry();
		QuestGraphMovieSignalBridge coldBridge = new QuestGraphMovieSignalBridge(coldRegistry, new QuestGraphTransitionExecutor());
		assertEquals(new DispatchResult(Status.FAILED, Propagation.STOP),
			coldBridge.dispatch(event, context(7, new PlayerQuestGraphStateList())));

		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		registry.installInitial(graphData());
		QuestGraphMovieSignalBridge bridge = new QuestGraphMovieSignalBridge(registry, new QuestGraphTransitionExecutor());
		assertEquals(new DispatchResult(Status.FAILED, Propagation.STOP),
			bridge.dispatch(event, context(8, new PlayerQuestGraphStateList())));
		assertThrows(IllegalArgumentException.class,
			() -> registry.reload(new CompiledQuestGraphData(Map.of(), Map.of()), List.of()));
		assertEquals(1, registry.snapshot().generation());
	}

	/** 创建真实 XSD/compiler 完成的影片图。 / Creates a movie graph completed by the production XSD and compiler. */
	private CompiledQuestGraphData graphData() throws Exception {
		Path xml = tempDir.resolve("movie-graph.xml");
		Files.writeString(xml, """
			<quest_graphs>
				<quest_graph quest_id="1" version="1" scope="PLAYER" initial_node="start">
					<node id="start">
						<transition id="movie-finished" priority="1" to="done">
							<movie-ended movie_id="913"/>
						</transition>
					</node>
					<node id="done" terminal="true"/>
				</quest_graph>
			</quest_graphs>
			""", StandardCharsets.UTF_8);
		return QuestGraphCompiler.load(xml, SCHEMA,
			new QuestGraphCompiler.References(Set.of(1), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(913)));
	}

	/** 创建无副作用动作的正式转换上下文。 / Creates a production transition context for a side-effect-free transition. */
	private static TransitionContext context(int playerId, PlayerQuestGraphStateList states) {
		return new TransitionContext(playerId, 0, SERVER_ZONE, states, invocation -> ConditionResult.MATCHED,
			invocation -> PreflightResult.READY, invocation -> ActionResult.APPLIED,
			(expectedRevision, state) -> PersistenceResult.APPLIED);
	}
}
