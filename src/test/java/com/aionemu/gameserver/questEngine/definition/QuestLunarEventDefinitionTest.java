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
 * 80034-80037 LUNAR 事件任务（Greater Glories / Only The Best / Gambling with Grace / From The Gutter）：
 * onLvlUp 背包收集物达标自动接取、NPC CHECK_COLLECTED_ITEMS(39) 扣物进 REWARD、LUNAR bonus 事件保持状态、
 * quest_data rewards 为空（奖励由 LUNAR bonus 系统下发）。
 * authority: 旧 handler _80034~80037.java; quest_data.xml:53058-53098; client_strings_quest.xml nameId 1180034-1180037。
 */
class QuestLunarEventDefinitionTest {

	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void lunarEventMetadataMatchesQuestData() throws Exception {
		assertLunar(80034, "[Event] Greater Glories", 1180034, "ELYOS", 164002016, 10, 80029);
		assertLunar(80035, "[Event] Only The Best", 1180035, "ELYOS", 164002017, 5, 80029);
		assertLunar(80036, "[Event] Gambling with Grace", 1180036, "ELYOS", 164002018, 1, 80029);
		assertLunar(80037, "[Event] From The Gutter", 1180037, "ASMODIANS", 164002016, 10, 80032);
	}

	@Test
	void levelUpAutoAcceptsWhenCollectItemsAreHeld() throws Exception {
		for (int questId : new int[] {80034, 80035, 80036, 80037}) {
			QuestDefinition definition = load(questId);
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"unaccepted".equals(transition.sourceNode())
					&& "started".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.LevelUp
					&& transition.conditions().stream().anyMatch(condition ->
						condition instanceof QuestCondition.HasItem hasItem
							&& hasItem.itemId() == collectItemOf(questId)
							&& hasItem.count() == collectCountOf(questId))),
				"quest " + questId + " must auto-accept on level-up when collect items are held");
		}
	}

	@Test
	void checkCollectedItemsRemovesItemsAndEntersReward() throws Exception {
		for (int questId : new int[] {80034, 80035, 80036, 80037}) {
			QuestDefinition definition = load(questId);
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(39).equals(talk.dialogId())
					&& transition.conditions().stream().anyMatch(condition ->
						condition instanceof QuestCondition.HasItem hasItem
							&& hasItem.itemId() == collectItemOf(questId))
					&& transition.actions().contains(new QuestAction.RemoveItem(collectItemOf(questId),
						collectCountOf(questId)))),
				"quest " + questId + " must check collected items (39) with removal into REWARD");
		}
	}

	@Test
	void lunarBonusIsClaimedInStartedState() throws Exception {
		for (int questId : new int[] {80034, 80035, 80036, 80037}) {
			QuestDefinition definition = load(questId);
			List<QuestTransition> bonusRoutes = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.BonusApply
					&& "LUNAR".equals(((QuestEvent.BonusApply) transition.event()).bonusType()))
				.toList();
			assertEquals(1, bonusRoutes.size(),
				"quest " + questId + " must claim LUNAR bonus in START (COMPLETE keep is rejected by compiler: COMPLETE projection requires complete-quest)");
			assertEquals("started", bonusRoutes.get(0).sourceNode());
		}
	}

	@Test
	void completionHasNoDirectRewardsBecauseLunarBonusIsTheReward() throws Exception {
		for (int questId : new int[] {80034, 80035, 80036, 80037}) {
			QuestDefinition definition = load(questId);
			assertTrue(definition.metadata().rewards().isEmpty(),
				"quest " + questId + " must not declare direct rewards (LUNAR bonus is the reward)");
			List<List<QuestAction>> completions = definition.transitions().stream()
				.filter(t -> t.targetNode().equals("complete"))
				.map(QuestTransition::actions).toList();
			assertEquals(16, completions.size(), "quest " + questId + " completion dialog routes 8..23");
			for (List<QuestAction> path : completions) {
				assertTrue(path.contains(new QuestAction.CompleteQuest(0)));
				assertEquals(1, path.size(), "quest " + questId + " completion must only complete without grants");
			}
		}
	}

	private static int collectItemOf(int questId) {
		return switch (questId) {
			case 80034, 80037 -> 164002016;
			case 80035 -> 164002017;
			case 80036 -> 164002018;
			default -> throw new IllegalArgumentException("unknown quest " + questId);
		};
	}

	private static int collectCountOf(int questId) {
		return switch (questId) {
			case 80034, 80037 -> 10;
			case 80035 -> 5;
			case 80036 -> 1;
			default -> throw new IllegalArgumentException("unknown quest " + questId);
		};
	}

	private static void assertLunar(int questId, String name, int displayNameId, String race,
			int collectItem, int collectCount, int prerequisite) throws Exception {
		QuestDefinition definition = load(questId);
		QuestMetadata metadata = definition.metadata();
		assertEquals(questId, definition.id());
		assertEquals(name, metadata.name());
		assertEquals(displayNameId, metadata.displayNameId());
		assertEquals(10, metadata.minLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertEquals(List.of(new QuestItemRequirement(collectItem, collectCount)), metadata.itemRequirements());
		assertEquals(List.of(new QuestBonus("LUNAR", 10, null)), metadata.bonuses());
		assertEquals(List.of(new QuestStartCondition("finished", prerequisite, 0)), metadata.startConditions());
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
