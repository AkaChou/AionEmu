package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural and typed-runtime coverage for quest 1929. */
class Quest1929RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/1929.xml");

	@Test
	void preservesMetadataAndAllElevenClassRewards() throws Exception {
		QuestMetadata metadata = load().definition().metadata();
		assertEquals("A Sliver Of Darkness", metadata.name());
		assertEquals(1102929, metadata.displayNameId());
		assertEquals(20, metadata.minLevel());
		assertEquals("MISSION", metadata.category());
		assertTrue(metadata.permittedRaces().contains("ELYOS"));
		assertEquals(25000, metadata.rewards().get(0).amount());
		assertEquals(457760, metadata.rewards().get(1).amount());
		assertEquals(162000048, metadata.rewards().get(2).id());
		assertEquals(11, metadata.classRewards().size());

		Map<String, Integer> expected = Map.ofEntries(
			Map.entry("FIGHTER", 140001110), Map.entry("KNIGHT", 140001133),
			Map.entry("RANGER", 140001159), Map.entry("ASSASSIN", 140001146),
			Map.entry("WIZARD", 140001180), Map.entry("ELEMENTALIST", 140001204),
			Map.entry("PRIEST", 140001237), Map.entry("CHANTER", 140001218),
			Map.entry("GUNSLINGER", 140001257), Map.entry("SONGWEAVER", 140001288),
			Map.entry("AETHERTECH", 140001272));
		for (Map.Entry<String, Integer> entry : expected.entrySet()) {
			assertEquals(entry.getValue(), metadata.classRewards().get(entry.getKey()).get(0).id());
		}
	}

	@Test
	void retainsShortcutMovieAndEquipmentBranches() throws Exception {
		CompiledQuestDefinition compiled = load();
		var transitions = compiled.definition().transitions();

		assertEquals(3, transitions.stream().filter(t -> t.event() instanceof QuestEvent.LevelUp).count());
		QuestTransition shortcut = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.LevelUp && t.targetNode().equals("complete")
				&& t.sourceNode() == null)
			.findFirst().orElseThrow();
		assertTrue(shortcut.conditions().contains(
			new QuestCondition.MembershipPermission(QuestMembershipPermission.STIGMA_SLOT_QUEST)));

		QuestTransition movie = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.MovieEnd movieEnd && movieEnd.movieId() == 155)
			.findFirst().orElseThrow();
		assertEquals("spawned98", movie.targetNode());
		assertTrue(movie.actions().contains(new QuestAction.SetVariable("step", 98)));
		assertTrue(movie.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SpawnNpc spawn
			&& spawn.templateId() == 205111));

		assertEquals(4, transitions.stream().filter(t -> t.event() instanceof QuestEvent.EquipItem).count());
		long equippedBranches = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 205111 && Integer.valueOf(-1).equals(talk.dialogId()))
			.filter(t -> t.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.EquippedItem equipped
				&& equipped.expected()))
			.count();
		long unequippedBranches = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 205111 && Integer.valueOf(-1).equals(talk.dialogId()))
			.filter(t -> t.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.EquippedItem equipped
				&& !equipped.expected()))
			.count();
		assertEquals(11, equippedBranches);
		assertEquals(11, unequippedBranches);
	}

	@Test
	void removesTheClassSpecificStigmaOnDieAndEnterWorldFailure() throws Exception {
		CompiledQuestDefinition compiled = load();
		Map<PlayerClass, Integer> expected = Map.ofEntries(
			Map.entry(PlayerClass.GLADIATOR, 140000003), Map.entry(PlayerClass.TEMPLAR, 140000003),
			Map.entry(PlayerClass.ASSASSIN, 140000003), Map.entry(PlayerClass.RANGER, 140000003),
			Map.entry(PlayerClass.SORCERER, 140000002), Map.entry(PlayerClass.SPIRIT_MASTER, 140000002),
			Map.entry(PlayerClass.CLERIC, 140000002), Map.entry(PlayerClass.CHANTER, 140000003),
			Map.entry(PlayerClass.GUNSLINGER, 140000004), Map.entry(PlayerClass.SONGWEAVER, 140000004),
			Map.entry(PlayerClass.AETHERTECH, 140000004));

		var die = compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.Die)
			.toList();
		assertEquals(11, die.size());
		for (QuestTransition transition : die) {
			PlayerClass playerClass = transition.conditions().stream()
				.filter(QuestCondition.AdvancedClassIs.class::isInstance)
				.map(QuestCondition.AdvancedClassIs.class::cast)
				.map(QuestCondition.AdvancedClassIs::playerClass).findFirst().orElseThrow();
			QuestAction.UnequipItem action = transition.actions().stream()
				.filter(QuestAction.UnequipItem.class::isInstance)
				.map(QuestAction.UnequipItem.class::cast).findFirst().orElseThrow();
			assertEquals(expected.get(playerClass), action.itemId());
			assertEquals("started2", transition.targetNode());
		}

		assertEquals(11, compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.EnterWorld && t.targetNode().equals("started2"))
			.filter(t -> t.actions().stream().anyMatch(QuestAction.UnequipItem.class::isInstance))
			.count());
		assertEquals(11, compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.EnterWorld && t.targetNode().equals("postFight8"))
			.filter(t -> t.actions().stream().anyMatch(QuestAction.UnequipItem.class::isInstance))
			.count());
	}

	@Test
	void plansTheSelectableGladiatorReward() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestTransition reward = compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203711 && Integer.valueOf(8).equals(talk.dialogId())
				&& t.targetNode().equals("complete"))
			.filter(t -> t.conditions().contains(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)))
			.findFirst().orElseThrow();
		int packed = compiled.definition().progressLayout().pack(Map.of("step", 9));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1929, QuestStatus.REWARD, packed, Map.of())
			.withPlayerClass(PlayerClass.GLADIATOR);
		var plan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(203711, 8), reward).orElseThrow();
		assertTrue(plan.requiredActions().stream().anyMatch(action -> action instanceof QuestAction.GrantReward grant
			&& grant.id() == 140001110));
		assertTrue(plan.requiredActions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
