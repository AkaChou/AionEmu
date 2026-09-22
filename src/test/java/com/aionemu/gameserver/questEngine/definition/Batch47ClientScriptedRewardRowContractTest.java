package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 47：1123（Where's Tutty?）的领奖态投影保持 `REWARD/var0=0`。
 * <p>
 * 用户 2026-09-22 真机判定：“看完影片，状态应该是 reward 0”；同时给出截图证据——把领奖态抬到
 * `var0=1`（批次 15 的改法）之后，影片结束任务说明**整块空白**（两条 `<p visible="[%0]"/[%3]">` 都不亮）。
 * 原因是本任务的客户端脚本是 `1123,ProgressAll,,sensoryArea,,1,LF1_SensoryArea_Q88`：客户端自己累计
 * 感应区进度，任务说明行 = 客户端进度 + 服务端 `SECTION_0`，服务端再抬 1 就整体越出两行范围。
 * 全库另外两个 `ProgressAll + sensoryArea` 任务 50008/51008（同样两行、槽位 0/3）在审计脚本里早已
 * 登记为「var0 不是任务书行号」；迁移前 Java handler `_1123Wheres_Tutty` 也是
 * `playQuestMovie(env, 11)` + `setStatus(REWARD)`（不写 var0）。
 * </p>
 * <p>
 * Locks batch 47: quest 1123 keeps the legacy REWARD/var0=0 projection because its client journal is
 * scripted (ProgressAll + sensoryArea) and adds the client's own progress to SECTION_0; the batch-15
 * projection of 1 blanked the whole journal. The zone entry plays movie 11 and lands REWARD, and the
 * client's movie-end callback re-syncs the journal after the cutscene.
 * </p>
 */
class Batch47ClientScriptedRewardRowContractTest {

	private static final int QUEST_ID = 1123;
	private static final int PERNOS = 790001;
	private static final String ZONE = "LF1_SENSORY_AREA_Q1123_210010000";
	private static final int MOVIE_ID = 11;

	@Test
	void rewardRowKeepsTheLegacyZero() throws Exception {
		QuestDefinition definition = definition();
		QuestNode reward = definition.nodes().stream().filter(node -> "reward".equals(node.label()))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, reward.projection().status(), "quest 1123 reward status");
		assertEquals(0, reward.projection().variables().get("var0"),
			"quest 1123 must keep REWARD/var0=0 for the client-scripted journal");
		assertTrue(definition.transitions().stream().flatMap(route -> route.actions().stream())
				.noneMatch(action -> action instanceof QuestAction.SetVariable set
					&& "var0".equals(set.field()) && set.value() == 1),
			"quest 1123 must never write var0=1 again (it blanks the client journal)");
	}

	@Test
	void zoneEntryPlaysMovieAndLandsReward() throws Exception {
		QuestDefinition definition = definition();
		List<QuestTransition> zoneRoutes = definition.transitions().stream()
			.filter(route -> route.event() instanceof QuestEvent.EnterZone zone && ZONE.equals(zone.zone()))
			.toList();
		assertEquals(1, zoneRoutes.size(), "quest 1123 must own exactly one LF1 sensory-zone route");
		QuestTransition zoneRoute = zoneRoutes.getFirst();
		assertEquals("started", zoneRoute.sourceNode(), "quest 1123 zone route source");
		assertEquals("reward", zoneRoute.targetNode(), "quest 1123 zone route target");
		assertEquals(List.of(new AfterCommitAction.PlayMovie(MOVIE_ID, QuestMovieType.CUTSCENE),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			zoneRoute.afterCommit(), "quest 1123 zone entry matches the legacy movie + status order");
	}

	@Test
	void movieEndResyncsTheJournal() throws Exception {
		QuestDefinition definition = definition();
		List<QuestTransition> movieEnd = definition.transitions().stream()
			.filter(route -> route.event().equals(new QuestEvent.MovieEnd(MOVIE_ID)))
			.toList();
		assertEquals(1, movieEnd.size(), "quest 1123 must own exactly one movie-end 11 route");
		QuestTransition route = movieEnd.getFirst();
		assertEquals("reward", route.sourceNode(), "quest 1123 movie-end source");
		assertEquals("reward", route.targetNode(), "quest 1123 movie-end target");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), route.afterCommit(),
			"quest 1123 must refresh the journal after the cutscene ends");
	}

	@Test
	void staleOneSavesHealBackToZero() throws Exception {
		QuestDefinition definition = definition();
		List<QuestTransition> heal = definition.transitions().stream()
			.filter(route -> route.sourceNode() == null)
			.filter(route -> "reward".equals(route.targetNode()))
			.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
			.filter(route -> route.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 1))))
			.toList();
		assertEquals(1, heal.size(), "quest 1123 must heal the batch-15 REWARD/var0=1 save");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 0)), heal.getFirst().actions(),
			"quest 1123 REWARD/var0=1 heals back to the legacy row 0");
	}

	@Test
	void rewardPageAndCompletionStayOnPernos() throws Exception {
		QuestDefinition definition = definition();
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

	private static QuestDefinition definition() throws IOException {
		try (InputStream input = Batch47ClientScriptedRewardRowContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + QUEST_ID + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST_ID + ".xml");
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
