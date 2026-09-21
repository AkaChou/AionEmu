package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 电影 self-loop 家族回归：翻页动作必须在 play-movie 后下发客户端目标页，不能只有影片副作用。
 * Family regression for movie self-loops: the page-turn action must emit its client target page after play-movie.
 *
 * <p>24053 的 step1-step4 仍保留旧 handler switch fallthrough 的 movie-only 路由；那些状态不在
 * 客户端 1011->1012->10000 可达链上，本测试只锁定可达链。</p>
 * <p>24053 keeps the old-handler switch-fallthrough movie-only routes in step1-step4; those states are
 * not on the reachable 1011->1012->10000 client chain, so this test locks only the reachable chain.</p>
 */
class MovieContinuationResponseFamilyTest {

	@Test
	void wheresRaeMoviePageTurnAlsoShowsSelect2_1() throws Exception {
		QuestDefinition definition = load(2002).definition();

		QuestTransition movie = talk(definition, "s1", "s1", 203534, QuestDialogAction.SELECT2_1);
		assertEquals(List.of(), movie.conditions());
		assertEquals(List.of(), movie.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(52),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			movie.afterCommit());

		QuestTransition page = talk(definition, "s1", "s1", 203534, QuestDialogAction.SELECT2_1_1);
		assertEquals(List.of(), page.conditions());
		assertEquals(List.of(), page.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1_1.id())),
			page.afterCommit());
		assertTerminalRoute(definition, 203534, QuestDialogAction.SETPRO2);
	}

	@Test
	void wheresRaeThisTimeMoviePageTurnAlsoShowsSelect3_1() throws Exception {
		QuestDefinition definition = load(2007).definition();

		QuestTransition movie = talk(definition, "v2", "v2", 203539, QuestDialogAction.SELECT3_1);
		assertEquals(List.of(), movie.conditions());
		assertEquals(List.of(), movie.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(55),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())),
			movie.afterCommit());

		QuestTransition page = talk(definition, "v2", "v2", 203539, QuestDialogAction.SELECT3_1_1);
		assertEquals(List.of(), page.conditions());
		assertEquals(List.of(), page.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1_1.id())),
			page.afterCommit());
		assertTerminalRoute(definition, 203539, QuestDialogAction.SETPRO3);
	}

	@Test
	void ascensionMoviePageTurnAlsoShowsSelect5_1() throws Exception {
		QuestDefinition definition = load(2008).definition();

		QuestTransition movie = talk(definition, "s4", "s4", 203550, QuestDialogAction.SELECT5_1);
		assertEquals(List.of(), movie.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(182203009, 1),
			new QuestAction.RemoveItem(182203010, 1),
			new QuestAction.RemoveItem(182203011, 1)),
			movie.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(57),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5_1.id())),
			movie.afterCommit());

		QuestTransition page = talk(definition, "s4", "s4", 203550, QuestDialogAction.SELECT5_1_1);
		assertEquals(List.of(), page.conditions());
		assertEquals(List.of(), page.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5_1_1.id())),
			page.afterCommit());
		assertTerminalRoute(definition, 203550, QuestDialogAction.SETPRO5);
	}

	@Test
	void speedyErrandMoviePageTurnAlsoShowsSelect2_1() throws Exception {
		QuestDefinition definition = load(24045).definition();

		QuestTransition movie = talk(definition, "s1", "s1", 279004, QuestDialogAction.SELECT2_1);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), movie.conditions());
		assertEquals(List.of(), movie.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(292),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			movie.afterCommit());
		assertTerminalRoute(definition, 279004, QuestDialogAction.SETPRO2);
	}

	@Test
	void frozenCityMoviePageTurnAlsoShowsSelect1_1() throws Exception {
		QuestDefinition definition = load(24052).definition();

		QuestTransition movie = talk(definition, "s0", "s0", 204753, QuestDialogAction.SELECT1_1);
		assertEquals(List.of(), movie.conditions());
		assertEquals(List.of(), movie.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(242),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			movie.afterCommit());

		QuestTransition page = talk(definition, "s0", "s0", 204753, QuestDialogAction.SELECT1_2);
		assertEquals(List.of(), page.conditions());
		assertEquals(List.of(), page.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_2.id())),
			page.afterCommit());
		assertTerminalRoute(definition, 204753, QuestDialogAction.SETPRO1);
	}

	@Test
	void maulingOfTheMauMoviePageTurnAlsoShowsSelect1_1() throws Exception {
		QuestDefinition definition = load(24053).definition();

		QuestTransition movie = talk(definition, "started", "started", 204787, QuestDialogAction.SELECT1_1);
		assertEquals(List.of(), movie.conditions());
		assertEquals(List.of(), movie.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(252),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			movie.afterCommit());
		assertTerminalRoute(definition, 204787, QuestDialogAction.SETPRO1);
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source)
				&& candidate.targetNode().equals(target)
				&& candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst().orElseThrow();
	}

	private static void assertTerminalRoute(QuestDefinition definition, int npcId, QuestDialogAction action) {
		assertTrue(definition.transitions().stream()
				.anyMatch(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id()))),
			"missing terminal route " + action);
	}

	private CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
