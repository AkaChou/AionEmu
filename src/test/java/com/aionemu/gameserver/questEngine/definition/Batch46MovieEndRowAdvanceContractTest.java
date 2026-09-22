package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 46：1123（Where's Tutty?）的感应区影片推进必须落在客户端影片结束回调上。
 * <p>
 * 用户 2026-09-22 真机 + QUEST-TRACE 证明服务端已推进（进入 LF1 感应区后
 * {@code SM_QUEST_ACTION} 状态=4 步数=1），但影片遮罩期间下发的任务书刷新被客户端丢弃，
 * 任务书停在行 0。本任务客户端脚本是
 * {@code quest_script_monster.csv} 的 {@code 1123,ProgressAll,,sensoryArea,,1,LF1_SensoryArea_Q88}，
 * 与同族 1336（12 个感应区影片）同型；1336 的既定写法是 enter-zone 只播片自环 +
 * {@code movie-end} 落行。
 * </p>
 * <p>
 * Locks batch 46: the sensory-zone transition only plays the movie and the journal row lands on the
 * client movie-end callback (CM_PLAY_MOVIE_END), matching sibling family 1336.
 * </p>
 */
class Batch46MovieEndRowAdvanceContractTest {

	private static final int QUEST_ID = 1123;
	private static final int SIBLING = 1336;
	private static final int PERNOS = 790001;
	private static final String ZONE = "LF1_SENSORY_AREA_Q1123_210010000";
	private static final int MOVIE_ID = 11;

	@Test
	void sensoryZoneOnlyPlaysTheMovie() throws Exception {
		QuestDefinition definition = definition(QUEST_ID);
		List<QuestTransition> zoneRoutes = definition.transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.EnterZone zone && ZONE.equals(zone.zone()))
			.toList();
		assertEquals(1, zoneRoutes.size(), "quest 1123 must own exactly one LF1 sensory-zone route");
		QuestTransition zoneRoute = zoneRoutes.getFirst();
		assertEquals("started", zoneRoute.sourceNode(), "quest 1123 zone route source");
		assertEquals("started", zoneRoute.targetNode(),
			"quest 1123 zone route must stay a play-movie self loop");
		assertEquals(List.of(new AfterCommitAction.PlayMovie(MOVIE_ID, QuestMovieType.CUTSCENE)),
			zoneRoute.afterCommit(), "quest 1123 zone route only plays movie 11");
		assertTrue(zoneRoute.afterCommit().stream()
				.noneMatch(AfterCommitAction.SyncQuestState.class::isInstance),
			"quest 1123 must not refresh the journal while the cutscene overlay is up");
	}

	@Test
	void journalRowLandsOnMovieEnd() throws Exception {
		QuestDefinition definition = definition(QUEST_ID);
		List<QuestTransition> movieEnd = definition.transitions().stream()
			.filter(route -> route.event().equals(new QuestEvent.MovieEnd(MOVIE_ID)))
			.toList();
		assertEquals(1, movieEnd.size(), "quest 1123 must own exactly one movie-end 11 route");
		QuestTransition route = movieEnd.getFirst();
		assertEquals("started", route.sourceNode(), "quest 1123 movie-end source");
		assertEquals("reward", route.targetNode(), "quest 1123 movie-end must land the reward row");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), route.afterCommit(),
			"quest 1123 must refresh the journal after the movie ends");

		QuestNode reward = definition.nodes().stream().filter(node -> "reward".equals(node.label()))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, reward.projection().status(), "quest 1123 reward status");
		assertEquals(1, reward.projection().variables().get("var0"),
			"quest 1123 reward row must stay the second journal row");
	}

	@Test
	void siblingFamilyKeepsTheSameMovieContract() throws Exception {
		QuestDefinition sibling = definition(SIBLING);
		List<QuestTransition> zoneMovies = sibling.transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.EnterZone)
			.filter(route -> route.afterCommit().stream()
				.anyMatch(AfterCommitAction.PlayMovie.class::isInstance))
			.toList();
		assertFalse(zoneMovies.isEmpty(), "family 1336 must keep its sensory-area movie routes");
		assertTrue(zoneMovies.stream().allMatch(route -> route.sourceNode().equals(route.targetNode())
				&& route.afterCommit().stream().noneMatch(AfterCommitAction.SyncQuestState.class::isInstance)),
			"family 1336 sensory-area movie routes must stay play-movie self loops");
		assertTrue(sibling.transitions().stream()
				.anyMatch(route -> route.event() instanceof QuestEvent.MovieEnd
					&& route.afterCommit().stream().anyMatch(AfterCommitAction.SyncQuestState.class::isInstance)),
			"family 1336 must advance its rows on movie-end");
	}

	@Test
	void staleRewardSavesStillHealToRowOne() throws Exception {
		QuestDefinition definition = definition(QUEST_ID);
		List<QuestTransition> heal = definition.transitions().stream()
			.filter(route -> route.sourceNode() == null)
			.filter(route -> "reward".equals(route.targetNode()))
			.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
			.filter(route -> route.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 0))))
			.toList();
		assertEquals(1, heal.size(), "quest 1123 must heal the pre-batch REWARD/var0=0 save");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), heal.getFirst().actions(),
			"quest 1123 REWARD/var0=0 heals to row 1");
	}

	@Test
	void rewardPageAndCompletionStayOnPernos() throws Exception {
		QuestDefinition definition = definition(QUEST_ID);
		List<QuestTransition> talk = definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.toList();
		assertTrue(talk.stream().allMatch(route -> route.event() instanceof QuestEvent.TalkToNpc npc
			&& npc.npcId() == PERNOS), "quest 1123 reward dialog must stay on Pernos 790001");
		assertTrue(talk.stream().flatMap(route -> route.afterCommit().stream())
				.anyMatch(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())::equals),
			"quest 1123 reward dialog must still show the client select2 page");
		assertTrue(talk.stream().flatMap(route -> route.actions().stream())
				.anyMatch(new QuestAction.CompleteQuest(0)::equals),
			"quest 1123 must complete at reward index 0");
	}

	private static QuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch46MovieEndRowAdvanceContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
