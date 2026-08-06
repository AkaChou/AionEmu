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
 * 80008/80009（Cake 系, 收集 1 扣物完成）与 80028/80031/80032（Fayrefolk 系, 对话即完成）:
 * 事件激活自动弃任（level-up + event-active(false) + abandon-quest）、1009/23 进 REWARD。
 * authority: 旧 handler _80008/_80009/_80028/_80031/_80032; quest_data.xml:52806/52815/53016/53037/53042; 真端 quest.xml; client_strings_quest.xml nameId。
 */
class QuestEventQuestBatchDefinitionTest {

	private static final Path DIR = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void cakeQuestsMatchQuestDataAndRetail() throws Exception {
		assertCake(80008, "[Event] Piece Of Cake!", 1180008, "ELYOS", 182214006, 798415);
		assertCake(80009, "[Event] The Cake Is The Truth!", 1180009, "ASMODIANS", 182214007, 798417);
	}

	@Test
	void cakeQuestsRemoveWorkItemOnSelectReward() throws Exception {
		for (int questId : new int[] {80008, 80009}) {
			int itemId = questId == 80008 ? 182214006 : 182214007;
			QuestDefinition definition = load(questId);
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(1009).equals(talk.dialogId())
					&& transition.conditions().contains(new QuestCondition.HasItem(itemId, 1))
					&& transition.actions().contains(new QuestAction.RemoveItem(itemId, 1))
					&& transition.actions().contains(new QuestAction.SetVariable("var0", 1))),
				"quest " + questId + " must remove the work item on SELECT_REWARD(1009) into REWARD");
		}
	}

	@Test
	void fayrefolkQuestsEnterRewardWithoutCollecting() throws Exception {
		for (int questId : new int[] {80028, 80031, 80032}) {
			QuestDefinition definition = load(questId);
			for (int dialogId : new int[] {1009, 23}) {
				assertTrue(definition.transitions().stream().anyMatch(transition ->
					"started".equals(transition.sourceNode())
						&& "reward".equals(transition.targetNode())
						&& transition.event() instanceof QuestEvent.TalkToNpc talk
						&& Integer.valueOf(dialogId).equals(talk.dialogId())
						&& transition.actions().isEmpty()),
					"quest " + questId + " dialog " + dialogId + " must enter REWARD without actions (no collectibles)");
			}
		}
	}

	@Test
	void levelUpAbandonsWhenEventInactive() throws Exception {
		for (int questId : new int[] {80008, 80009, 80028, 80031, 80032}) {
			QuestDefinition definition = load(questId);
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
	void fayrefolkMetadataMatchesQuestDataAndRetail() throws Exception {
		assertFayrefolk(80028, "[Event] Meet The Fayrefolk", 1180028, "ELYOS", 10, 799766, 169610036, 1);
		assertFayrefolk(80031, "[Event] The Fayrefolk", 1180031, "ASMODIANS", 10, 799781, 169610036, 1);
		assertFayrefolk(80032, "[Event] A Charmed Existence", 1180032, "ASMODIANS", 15, 799781, 188051133, 10);
	}

	private static void assertCake(int questId, String name, int displayNameId, String race,
			int workItem, int npcId) throws Exception {
		QuestDefinition definition = load(questId);
		QuestMetadata metadata = definition.metadata();
		assertEquals(questId, definition.id());
		assertEquals(name, metadata.name());
		assertEquals(displayNameId, metadata.displayNameId());
		assertEquals(10, metadata.minLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertEquals(10, metadata.repeatPolicy().maxRepeatCount());
		assertTrue(metadata.cannotShare());
		assertEquals(List.of(new QuestItemRequirement(workItem, 1)), metadata.questWorkItems());
		assertEquals(List.of(new QuestReward("GOLD", 0, 20000L),
			new QuestReward("EXP", 0, 10000L),
			new QuestReward("ITEM", 160010100, 5L),
			new QuestReward("ITEM", 164002019, 3L)), metadata.rewards());
		assertEquals(Set.of(npcId), definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}

	private static void assertFayrefolk(int questId, String name, int displayNameId, String race,
			int minLevel, int npcId, int rewardItem, int rewardCount) throws Exception {
		QuestDefinition definition = load(questId);
		QuestMetadata metadata = definition.metadata();
		assertEquals(questId, definition.id());
		assertEquals(name, metadata.name());
		assertEquals(displayNameId, metadata.displayNameId());
		assertEquals(minLevel, metadata.minLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertEquals(1, metadata.repeatPolicy().maxRepeatCount());
		assertTrue(metadata.cannotShare());
		assertTrue(metadata.itemRequirements().isEmpty(), "Fayrefolk quests have no collectibles");
		assertEquals(List.of(new QuestReward("ITEM", rewardItem, (long) rewardCount)), metadata.rewards());
		assertEquals(Set.of(npcId), definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}

	private static QuestDefinition load(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(DIR.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
