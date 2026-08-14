package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 80016/80018 MOVIE 事件任务（Sock Hop / Sock It To 'Em）：
 * 活动激活时 onLvlUp 自动接取、活动不激活弃任、收集 15 扣物进 REWARD、MOVIE bonus 随机播放。
 * authority: 旧 handler _80016/_80018.java; quest_data.xml:52874/52899; 真实 quest.xml; client_strings_quest.xml nameId。
 */
class QuestMovieEventDefinitionTest {

	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void metadataMatchesQuestDataAndRetail() throws Exception {
		assertMovie(80016, "[Event] Sock Hop", 1180016, "ELYOS", 182214008, 217280, 799763, 103, 104);
		assertMovie(80018, "[Event] Sock It To 'Em", 1180018, "ASMODIANS", 182214010, 217279, 799778, 135, 136);
	}

	@Test
	void levelUpAutoAcceptsWhenActiveAndAbandonsWhenInactive() throws Exception {
		for (int questId : new int[] {80016, 80018}) {
			QuestDefinition definition = load(questId);
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"unaccepted".equals(transition.sourceNode())
					&& "started".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.LevelUp
					&& transition.conditions().contains(new QuestCondition.EventActive(true))
					&& transition.conditions().contains(new QuestCondition.StartEligible())),
				"quest " + questId + " must auto-accept on level-up while the event is active");
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& "unaccepted".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.LevelUp
					&& transition.conditions().contains(new QuestCondition.EventActive(false))
					&& transition.actions().contains(new QuestAction.AbandonQuest())),
				"quest " + questId + " must abandon on level-up when the event is inactive");
		}
	}

	@Test
	void collectedItemsCheckRemovesFifteenAndEntersReward() throws Exception {
		for (int questId : new int[] {80016, 80018}) {
			QuestDefinition definition = load(questId);
			int itemId = collectItemOf(questId);
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(39).equals(talk.dialogId())
					&& transition.conditions().contains(new QuestCondition.HasItem(itemId, 15))
					&& transition.actions().contains(new QuestAction.RemoveItem(itemId, 15))
					&& transition.actions().contains(new QuestAction.SetVariable("var0", 1))),
				"quest " + questId + " must check collected items (39) with removal into REWARD");
		}
	}

	@Test
	void movieBonusPlaysRandomVariantInRewardState() throws Exception {
		for (int questId : new int[] {80016, 80018}) {
			QuestDefinition definition = load(questId);
			List<QuestTransition> bonusRoutes = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.BonusApply
					&& "MOVIE".equals(((QuestEvent.BonusApply) transition.event()).bonusType()))
				.toList();
			assertEquals(1, bonusRoutes.size(), "quest " + questId + " must claim the MOVIE bonus in REWARD");
			assertEquals("reward", bonusRoutes.get(0).sourceNode());
			AfterCommitAction.PlayMovieRandom random = bonusRoutes.get(0).afterCommit().stream()
				.filter(AfterCommitAction.PlayMovieRandom.class::isInstance)
				.map(AfterCommitAction.PlayMovieRandom.class::cast)
				.findFirst().orElseThrow();
			assertEquals(moviesOf(questId), random.movieIds());
		}
	}

	private static void assertMovie(int questId, String name, int displayNameId, String race,
			int collectItem, int dropNpcId, int npcId, int movieA, int movieB) throws Exception {
		QuestDefinition definition = load(questId);
		QuestMetadata metadata = definition.metadata();
		assertEquals(questId, definition.id());
		assertEquals(name, metadata.name());
		assertEquals(displayNameId, metadata.displayNameId());
		assertEquals(10, metadata.minLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertEquals(List.of(new QuestItemRequirement(collectItem, 15)), metadata.itemRequirements());
		assertEquals(List.of(new QuestReward("GOLD", 0, 100000L),
			new QuestReward("EXP", 0, 50000L),
			new QuestReward("ITEM", 188051107, 1L),
			new QuestReward("ITEM", 125040047, 1L)), metadata.rewards());
		assertEquals(List.of(new QuestDrop(dropNpcId, collectItem, 100, false, 0)), metadata.drops());
		assertEquals(Set.of(npcId), definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}

	private static int collectItemOf(int questId) {
		return questId == 80016 ? 182214008 : 182214010;
	}

	private static List<Integer> moviesOf(int questId) {
		return questId == 80016 ? List.of(103, 104) : List.of(135, 136);
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
