package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestEquipmentFacts;
import com.aionemu.gameserver.questEngine.runtime.QuestMembershipFacts;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for the migrated owners repaired in this batch. */
class MigratedQuestRepairDefinitionTest {

	@Test
	void relicRewardFamiliesConsumeTheCorrectRelicAndSelectMetadataAp() {
		Map<Integer, long[]> expected = Map.of(
			11279, new long[] {300, 600, 900, 1200},
			11280, new long[] {600, 1200, 1800, 2400},
			11281, new long[] {800, 1600, 2400, 3200},
			11282, new long[] {1600, 3200, 4800, 6400},
			11283, new long[] {3000, 6000, 9000, 12000},
			11284, new long[] {6000, 12000, 18000, 24000},
			11285, new long[] {8000, 16000, 24000, 32000},
			11286, new long[] {16000, 32000, 48000, 64000});
		for (Map.Entry<Integer, long[]> entry : expected.entrySet()) {
			CompiledQuestDefinition definition = load(entry.getKey());
			assertEquals(4, definition.definition().metadata().rewards().size());
			for (int index = 0; index < entry.getValue().length; index++) {
				QuestReward reward = definition.definition().metadata().rewards().get(index);
				assertEquals("AP", reward.kind());
				assertEquals(entry.getValue()[index], reward.amount());
			}
			List<QuestTransition> completion = definition.definition().transitions().stream()
				.filter(transition -> transition.sourceNode().startsWith("reward")
					&& transition.targetNode().equals("complete"))
				.toList();
			assertEquals(4, completion.size());
			assertTrue(completion.stream().allMatch(transition ->
				transition.actions().stream().anyMatch(QuestAction.GrantSelectedReward.class::isInstance)));
		}
	}

	@Test
	void loneDefenseUsesRetailPrerequisitesRandomDefenseAndExplicitSelectableBranches() {
		CompiledQuestDefinition definition = load(14026);
		assertEquals(Set.of(14020, 14021, 14022, 14023, 14024, 14025),
			definition.definition().metadata().prerequisites());
		assertEquals(3, definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet
				&& Objects.equals(transition.sourceNode(), "defense"))
			.flatMap(transition -> ((QuestEvent.KillNpcSet) transition.event()).npcIds().stream())
			.distinct().count());
		assertTrue(definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "defense"))
			.flatMap(transition -> transition.afterCommit().stream())
			.anyMatch(action -> action instanceof AfterCommitAction.SpawnNpcRandom random
				&& random.variants().stream().noneMatch(variant -> variant.templateId() == 213579)));
		List<QuestTransition> completion = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "reward")
				&& transition.targetNode().equals("complete"))
			.toList();
		assertEquals(Set.of(8, 9, 10, 11, 12, 13), completion.stream()
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId())
			.collect(java.util.stream.Collectors.toSet()));
		assertTrue(completion.stream().allMatch(transition -> transition.actions().stream()
			.anyMatch(action -> action instanceof QuestAction.GrantReward reward
				&& reward.rewardKind() == QuestRewardKind.ITEM
				&& Set.of(110101843, 110301826, 110301828, 110551154, 110551156, 110601629)
				.contains(reward.id()))));

		QuestTransition start = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.ZoneMissionEnd).findFirst().orElseThrow();
		int packed = definition.definition().progressLayout().pack(Map.of("var0", 0));
		QuestSnapshot ready = new QuestSnapshot(7, 14026, QuestStatus.NONE, packed, Map.of())
			.withCompletedQuestIds(Set.of(14020, 14021, 14022, 14023, 14024, 14025))
			.withActiveQuestIds(Set.of())
			.withStartEligibility(QuestStartEligibility.allowed());
		QuestSnapshot blocked = ready.withCompletedQuestIds(Set.of(14020, 14021));
		assertTrue(QuestMutationPlanner.plan(definition, ready, new QuestEvent.ZoneMissionEnd(), start).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition, blocked, new QuestEvent.ZoneMissionEnd(), start).isEmpty());
	}

	@Test
	void pippiQuestPreservesPaidSupplyAndBothRibbonZoneOrders() {
		CompiledQuestDefinition definition = load(3090);
		assertEquals(Set.of(3089), definition.definition().metadata().prerequisites());
		assertEquals(Set.of(182208050, 182208051), definition.definition().metadata().questWorkItems().stream()
			.map(QuestItemRequirement::itemId).collect(java.util.stream.Collectors.toSet()));
		assertEquals(4952329, definition.definition().metadata().rewards().get(0).amount());
		assertEquals(162003001, definition.definition().metadata().rewards().get(1).id());

		List<QuestTransition> paidBranches = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 798193 && talk.dialogId() == 10002)
			.toList();
		assertEquals(2, paidBranches.size());
		QuestTransition paid = paidBranches.stream()
			.filter(transition -> transition.targetNode().equals("s3")).findFirst().orElseThrow();
		assertTrue(paid.conditions().contains(new QuestCondition.CurrencyAtLeast(QuestRewardKind.GOLD, 10000)));
		assertTrue(paid.actions().contains(new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 10000)));
		assertTrue(paid.actions().contains(new QuestAction.GiveItem(182208050, 1)));
		QuestTransition insufficient = paidBranches.stream()
			.filter(transition -> transition.targetNode().equals("s2")).findFirst().orElseThrow();
		assertTrue(insufficient.conditions().contains(new QuestCondition.CurrencyBelow(QuestRewardKind.GOLD, 10000)));

		assertEquals(2, definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.EnterZone).count());
		assertEquals(2, definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 700420)
			.count());

		int s2Packed = definition.definition().progressLayout().pack(Map.of("var0", 2, "var1", 0, "var2", 0));
		QuestSnapshot withKinah = new QuestSnapshot(7, 3090, QuestStatus.START, s2Packed, Map.of(),
			Map.of(QuestRewardKind.GOLD, 10000L));
		assertTrue(QuestMutationPlanner.plan(definition, withKinah, new QuestEvent.TalkToNpc(798193, 10002), paid)
			.isPresent());
		QuestSnapshot withoutKinah = new QuestSnapshot(7, 3090, QuestStatus.START, s2Packed, Map.of(),
			Map.of(QuestRewardKind.GOLD, 9999L));
		assertTrue(QuestMutationPlanner.plan(definition, withoutKinah,
			new QuestEvent.TalkToNpc(798193, 10002), paid).isEmpty());
		assertTrue(QuestMutationPlanner.plan(definition, withoutKinah,
			new QuestEvent.TalkToNpc(798193, 10002), insufficient).isPresent());

		int s1Packed = definition.definition().progressLayout().pack(Map.of("var0", 1, "var1", 0, "var2", 0));
		QuestSnapshot s1 = new QuestSnapshot(7, 3090, QuestStatus.START, s1Packed, Map.of());
		QuestTransition ribbonFirst = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "s1")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 700420)
			.findFirst().orElseThrow();
		assertTrue(QuestMutationPlanner.plan(definition, s1, new QuestEvent.TalkToNpc(700420), ribbonFirst).isPresent());
		int s1ZonePacked = definition.definition().progressLayout().pack(Map.of("var0", 1, "var1", 1, "var2", 0));
		QuestSnapshot s1Zone = new QuestSnapshot(7, 3090, QuestStatus.START, s1ZonePacked, Map.of());
		QuestTransition zoneSecond = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "s1-zone")).findFirst().orElseThrow();
		assertTrue(QuestMutationPlanner.plan(definition, s1Zone, new QuestEvent.TalkToNpc(700420), zoneSecond)
			.isPresent());

		QuestTransition feed = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 798193 && talk.dialogId() == 10000)
			.findFirst().orElseThrow();
		assertTrue(feed.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10)));
		QuestTransition supply = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 700421 && talk.dialogId() == 10255)
			.findFirst().orElseThrow();
		assertTrue(supply.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	@Test
	void ascensionRestoresRetailMetadataCombatGateAndClassChangeBranches() {
		CompiledQuestDefinition definition = load(1006);
		assertEquals("Ascension", definition.definition().metadata().name());
		assertEquals(1102006, definition.definition().metadata().displayNameId());
		assertEquals(9, definition.definition().metadata().minLevel());
		assertEquals(Set.of("ELYOS"), definition.definition().metadata().permittedRaces());
		assertEquals(73200, definition.definition().metadata().rewards().get(0).amount());
		assertEquals(7, definition.definition().progressLayout().field("var0").width());

		List<QuestTransition> classChoices = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "s5")
				&& transition.targetNode().equals("reward")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 790001)
			.toList();
		assertEquals(11, classChoices.size());
		assertTrue(classChoices.stream().allMatch(transition -> transition.afterCommit().stream()
			.anyMatch(action -> action instanceof AfterCommitAction.SetPlayerClass)));
		assertTrue(classChoices.stream().anyMatch(transition -> transition.afterCommit()
			.contains(new AfterCommitAction.SetPlayerClass(PlayerClass.AETHERTECH))));

		QuestTransition itemPlay = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.ItemPlay(int itemId, int animationMillis)
				&& itemId == 182200007 && animationMillis == 3000)
			.findFirst().orElseThrow();
		assertTrue(itemPlay.conditions().contains(new QuestCondition.ZoneIs("CLIONA_LAKE_210010000")));
		assertTrue(itemPlay.actions().contains(new QuestAction.RemoveItem(182200007, 1)));
		assertTrue(itemPlay.actions().contains(new QuestAction.GiveItem(182200008, 1)));

		QuestTransition attack = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.AttackNpc attackEvent
				&& attackEvent.npcId() == 211043)
			.findFirst().orElseThrow();
		assertTrue(attack.conditions().contains(new QuestCondition.NpcHpBelowPercent(211043, 50)));
		assertTrue(attack.afterCommit().contains(new AfterCommitAction.PlayMovie(151)));
		assertTrue(attack.afterCommit().contains(new AfterCommitAction.DespawnNpc("boss")));

		long failureRoutes = definition.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.Die
				&& transition.targetNode().equals("s3")
				&& transition.afterCommit().contains(new AfterCommitAction.SendSystemMessage(
					QuestSystemMessage.QUEST_FAILED)))
			.count();
		assertEquals(6, failureRoutes);

		List<QuestTransition> completion = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "reward")
				&& transition.targetNode().equals("complete"))
			.toList();
		assertEquals(16, completion.size());
		assertTrue(completion.stream().allMatch(transition -> transition.actions().contains(
				new QuestAction.GrantReward("EXP", 0, 73200, QuestRewardAmountMode.QUEST_BASE))));
	}

	@Test
	void sagesGiftUsesSharedEquipmentDpAndSameEventFinalKillCapabilities() {
		CompiledQuestDefinition definition = load(1990);
		QuestMetadata metadata = definition.definition().metadata();
		assertEquals("A Sage's Gift", metadata.name());
		assertEquals(30, metadata.minLevel());
		assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
		assertTrue(metadata.startConditions().contains(new QuestStartCondition("finished", 1989, 0)));
		assertEquals(150, metadata.itemRequirements().get(0).count());
		assertEquals(874200, metadata.rewards().get(0).amount());
		assertEquals(Map.ofEntries(Map.entry("FIGHTER", 5), Map.entry("KNIGHT", 4),
			Map.entry("RANGER", 3), Map.entry("ASSASSIN", 3), Map.entry("WIZARD", 2),
			Map.entry("ELEMENTALIST", 2), Map.entry("PRIEST", 3), Map.entry("CHANTER", 3),
			Map.entry("GUNSLINGER", 2), Map.entry("SONGWEAVER", 1), Map.entry("AETHERTECH", 1)),
			metadata.classRewards().entrySet().stream()
				.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size())));

		List<QuestTransition> classCompletions = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "reward")
				&& transition.targetNode().equals("complete")
				&& transition.conditions().stream().anyMatch(QuestCondition.AdvancedClassIs.class::isInstance))
			.toList();
		assertEquals(29, classCompletions.size());
		assertTrue(classCompletions.stream().allMatch(transition -> transition.actions().stream()
			.anyMatch(action -> action instanceof QuestAction.GrantReward reward
				&& reward.rewardKind() == QuestRewardKind.ITEM)));

		QuestEvent.TalkToNpc startDialog = new QuestEvent.TalkToNpc(203771, 31);
		QuestTransition equippedRoute = definition.definition().transitions().stream()
			.filter(transition -> transition.event().equals(startDialog)
				&& transition.conditions().contains(new QuestCondition.EquipmentSetEquipped(
					Set.of(9, 8, 7, 6, 378), 5, true)))
			.findFirst().orElseThrow();
		QuestSnapshot equipped = new QuestSnapshot(7, 1990, QuestStatus.NONE, 0, Map.of())
			.withEquipmentFacts(new QuestEquipmentFacts(Map.of(9, 5)));
		assertTrue(QuestMutationPlanner.plan(definition, equipped, startDialog, equippedRoute).isPresent());

		QuestTransition dpRoute = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "started3")
				&& transition.event().equals(new QuestEvent.TalkToNpc(203771, 2035))
				&& transition.targetNode().equals("reward"))
			.findFirst().orElseThrow();
		QuestSnapshot dpReady = new QuestSnapshot(7, 1990, QuestStatus.START,
			definition.definition().progressLayout().pack(Map.of("step", 3, "abex", 30, "ettin", 30,
				"worg", 30, "hunt-complete", 60)), Map.of(186000040, 1),
			Map.of(QuestRewardKind.DP, 1000L)).withMaxDp(1000);
		var dpPlan = QuestMutationPlanner.plan(definition, dpReady,
			new QuestEvent.TalkToNpc(203771, 2035), dpRoute).orElseThrow();
		assertTrue(dpPlan.requiredActions().contains(new QuestAction.RemoveItem(186000040, 1)));
		assertTrue(dpPlan.requiredActions().contains(new QuestAction.SetCurrency(QuestRewardKind.DP, 0)));

		QuestTransition finalAbex = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "started2")
				&& transition.targetNode().equals("started2-complete")
				&& transition.event() instanceof QuestEvent.KillNpcSet
				&& transition.conditions().contains(new QuestCondition.VariableBelow("abex", 30)))
			.findFirst().orElseThrow();
		int nearlyComplete = definition.definition().progressLayout().pack(Map.of("step", 2, "abex", 29,
			"ettin", 30, "worg", 30, "hunt-complete", 0));
		var finalPlan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1990, QuestStatus.START, nearlyComplete, Map.of()),
			new QuestEvent.KillNpc(211869), finalAbex).orElseThrow();
		assertEquals(30, definition.definition().progressLayout().unpack(finalPlan.nextPackedVariables()).get("abex"));
		assertEquals(60, definition.definition().progressLayout().unpack(finalPlan.nextPackedVariables()).get("hunt-complete"));
	}

	@Test
	void daevanionWeaponUsesAsmodianMetadataAndSharedEquipmentDpCapabilities() {
		CompiledQuestDefinition definition = load(2990);
		QuestMetadata metadata = definition.definition().metadata();
		assertEquals("Making The Daevanion Weapon", metadata.name());
		assertEquals(30, metadata.minLevel());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertEquals(1, metadata.repeatPolicy().maxRepeatCount());
		assertTrue(metadata.startConditions().contains(new QuestStartCondition("finished", 2989, 0)));
		assertEquals(150, metadata.itemRequirements().get(0).count());
		assertEquals(874200, metadata.rewards().get(0).amount());
		assertEquals(32, metadata.drops().size());
		assertEquals(101800570, metadata.classRewards().get("GUNSLINGER").get(0).id());
		assertEquals(102000593, metadata.classRewards().get("SONGWEAVER").get(0).id());

		List<QuestTransition> classCompletions = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "reward")
				&& transition.targetNode().equals("complete")
				&& transition.conditions().stream().anyMatch(QuestCondition.AdvancedClassIs.class::isInstance))
			.toList();
		assertEquals(29, classCompletions.size());

		QuestEvent.TalkToNpc startDialog = new QuestEvent.TalkToNpc(204146, 31);
		QuestTransition equippedRoute = definition.definition().transitions().stream()
			.filter(transition -> transition.event().equals(startDialog)
				&& transition.conditions().contains(new QuestCondition.EquipmentSetEquipped(
					Set.of(9, 8, 7, 6, 378), 5, true)))
			.findFirst().orElseThrow();
		QuestSnapshot equipped = new QuestSnapshot(7, 2990, QuestStatus.NONE, 0, Map.of())
			.withEquipmentFacts(new QuestEquipmentFacts(Map.of(9, 5)));
		assertTrue(QuestMutationPlanner.plan(definition, equipped, startDialog, equippedRoute).isPresent());

		QuestTransition dpRoute = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "started3")
				&& transition.event().equals(new QuestEvent.TalkToNpc(204146, 2035))
				&& transition.targetNode().equals("reward"))
			.findFirst().orElseThrow();
		int started3 = definition.definition().progressLayout().pack(Map.of("step", 3, "crestlich", 30,
			"monitor", 30, "vespine", 30, "hunt-complete", 60));
		QuestSnapshot dpReady = new QuestSnapshot(7, 2990, QuestStatus.START, started3,
			Map.of(186000040, 1), Map.of(QuestRewardKind.DP, 1000L)).withMaxDp(1000);
		var dpPlan = QuestMutationPlanner.plan(definition, dpReady,
			new QuestEvent.TalkToNpc(204146, 2035), dpRoute).orElseThrow();
		assertTrue(dpPlan.requiredActions().contains(new QuestAction.RemoveItem(186000040, 1)));
		assertTrue(dpPlan.requiredActions().contains(new QuestAction.SetCurrency(QuestRewardKind.DP, 0)));

		QuestTransition finalCrestlich = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "started2")
				&& transition.targetNode().equals("started2-complete")
				&& transition.event() instanceof QuestEvent.KillNpcSet
				&& transition.conditions().contains(new QuestCondition.VariableBelow("crestlich", 30)))
			.findFirst().orElseThrow();
		int nearlyComplete = definition.definition().progressLayout().pack(Map.of("step", 2, "crestlich", 29,
			"monitor", 30, "vespine", 30, "hunt-complete", 0));
		var finalPlan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 2990, QuestStatus.START, nearlyComplete, Map.of()),
			new QuestEvent.KillNpc(213011), finalCrestlich).orElseThrow();
		assertEquals(30, definition.definition().progressLayout().unpack(finalPlan.nextPackedVariables()).get("crestlich"));
		assertEquals(60, definition.definition().progressLayout().unpack(finalPlan.nextPackedVariables()).get("hunt-complete"));
	}

	@Test
	void noEscapingDestinyUsesClassSpecificStigmasAndCompleteRecoveryRoutes() {
		CompiledQuestDefinition definition = load(2900);
		QuestMetadata metadata = definition.definition().metadata();
		assertEquals("No Escaping Destiny", metadata.name());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertEquals(20, metadata.minLevel());
		assertEquals(25000, metadata.rewards().get(0).amount());
		assertEquals(457760, metadata.rewards().get(1).amount());
		assertEquals(11, metadata.classRewards().size());

		QuestTransition movieEnd = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "instance95")
				&& transition.targetNode().equals("movie96")
				&& transition.event().equals(new QuestEvent.MovieEnd(156)))
			.findFirst().orElseThrow();
		assertTrue(movieEnd.actions().contains(new QuestAction.SetVariable("step", 96)));

		Map<PlayerClass, Integer> classStigmas = Map.ofEntries(
			Map.entry(PlayerClass.GLADIATOR, 140000003), Map.entry(PlayerClass.TEMPLAR, 140000003),
			Map.entry(PlayerClass.ASSASSIN, 140000003), Map.entry(PlayerClass.RANGER, 140000003),
			Map.entry(PlayerClass.SORCERER, 140000002), Map.entry(PlayerClass.SPIRIT_MASTER, 140000002),
			Map.entry(PlayerClass.CLERIC, 140000002), Map.entry(PlayerClass.CHANTER, 140000003),
			Map.entry(PlayerClass.GUNSLINGER, 140000004), Map.entry(PlayerClass.SONGWEAVER, 140000004),
			Map.entry(PlayerClass.AETHERTECH, 140000004));
		QuestEvent.TalkToNpc selectStone = new QuestEvent.TalkToNpc(204264, 3058);
		QuestEvent.TalkToNpc useObject = new QuestEvent.TalkToNpc(204264, -1);
		for (Map.Entry<PlayerClass, Integer> entry : classStigmas.entrySet()) {
			QuestCondition.AdvancedClassIs classCondition = new QuestCondition.AdvancedClassIs(entry.getKey());
			QuestCondition.EquippedItem equipped = new QuestCondition.EquippedItem(entry.getValue(), 1, true);
			QuestCondition.EquippedItem notEquipped = new QuestCondition.EquippedItem(entry.getValue(), 1, false);

			QuestTransition alreadyEquipped = definition.definition().transitions().stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), "movie96")
					&& transition.targetNode().equals("movie96") && transition.event().equals(selectStone)
					&& transition.conditions().contains(classCondition))
				.findFirst().orElseThrow();
			assertTrue(alreadyEquipped.conditions().contains(equipped));

			QuestTransition grant = definition.definition().transitions().stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), "movie96")
					&& transition.targetNode().equals("equipped99") && transition.event().equals(selectStone)
					&& transition.conditions().contains(classCondition))
				.findFirst().orElseThrow();
			assertTrue(grant.conditions().contains(notEquipped));
			assertTrue(grant.actions().contains(new QuestAction.GiveItem(entry.getValue(), 1)));

			QuestTransition prompt = definition.definition().transitions().stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), "equipped99")
					&& transition.event().equals(useObject) && transition.conditions().contains(classCondition))
				.findFirst().orElseThrow();
			assertTrue(prompt.conditions().contains(notEquipped));
		}

		int movie96 = definition.definition().progressLayout().pack(Map.of("step", 96));
		QuestSnapshot gladiatorEquipped = new QuestSnapshot(7, 2900, QuestStatus.START, movie96, Map.of())
			.withPlayerClass(PlayerClass.GLADIATOR)
			.withEquipmentFacts(new QuestEquipmentFacts(Map.of(), Map.of(140000003, 1)));
		QuestTransition gladiatorClose = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "movie96")
				&& transition.targetNode().equals("movie96") && transition.event().equals(selectStone)
				&& transition.conditions().contains(new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR)))
			.findFirst().orElseThrow();
		assertTrue(QuestMutationPlanner.plan(definition, gladiatorEquipped, selectStone, gladiatorClose).isPresent());

		long failedDeathRoutes = definition.definition().transitions().stream()
			.filter(transition -> Set.of("instance95", "movie96", "equipped99", "equipped97", "fight98")
				.contains(transition.sourceNode())
				&& transition.targetNode().equals("started4")
				&& transition.event() instanceof QuestEvent.Die
				&& transition.actions().contains(new QuestAction.UnequipItem(140000002, 1))
				&& transition.actions().contains(new QuestAction.UnequipItem(140000003, 1))
				&& transition.actions().contains(new QuestAction.UnequipItem(140000004, 1))
				&& transition.afterCommit().contains(new AfterCommitAction.SendSystemMessage(
					QuestSystemMessage.QUEST_FAILED)))
			.count();
		assertEquals(5, failedDeathRoutes);

		QuestTransition memberSkip = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "unaccepted")
				&& transition.targetNode().equals("complete")
				&& transition.event() instanceof QuestEvent.LevelUp)
			.findFirst().orElseThrow();
		QuestSnapshot member = new QuestSnapshot(7, 2900, QuestStatus.NONE, 0, Map.of())
			.withStartEligibility(QuestStartEligibility.allowed())
			.withMembershipFacts(new QuestMembershipFacts(Set.of(QuestMembershipPermission.STIGMA_SLOT_QUEST)));
		assertTrue(QuestMutationPlanner.plan(definition, member, new QuestEvent.LevelUp(), memberSkip).isPresent());
	}

	private static void assertGroupQuest(int questId, int itemId, int itemCount, int dropNpcId, int rewardNpcId,
		Set<Integer> selectableItems) {
		CompiledQuestDefinition definition = load(questId);
		if (itemId > 0) {
			assertEquals(itemCount, definition.definition().metadata().itemRequirements().stream()
				.filter(item -> item.itemId() == itemId).findFirst().orElseThrow().count());
			assertEquals(1, definition.definition().metadata().drops().stream()
				.filter(drop -> drop.npcId() == dropNpcId && drop.itemId() == itemId
					&& drop.collectingStep() > 0 && drop.eachMember()).count());
			QuestTransition report = definition.definition().transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == 39).findFirst().orElseThrow();
			// 移除量以真端 collect_item 计数为权威（遗留迁移的 ALL 没收无真端/客户端痕迹，
			// 四家组队任务元数据同为 drop_each_member=1 无机械判据，按真端优先裁剪）。
			// Removal follows the retail collect_item counts (the legacy ALL confiscation has no
			// retail/client trace — all four party quests share drop_each_member=1 with no
			// mechanical discriminator — trimmed retail-first).
			assertTrue(report.actions().contains(new QuestAction.RemoveItem(itemId, itemCount)));
		} else {
			assertTrue(definition.definition().metadata().drops().isEmpty());
		}

		Set<Integer> declaredSelectable = definition.definition().metadata().rewards().stream()
			.filter(reward -> reward.kind().equals("SELECTABLE_ITEM"))
			.map(QuestReward::id).collect(java.util.stream.Collectors.toSet());
		assertEquals(selectableItems, declaredSelectable);
		List<QuestTransition> completionRoutes = definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "reward")
				&& transition.targetNode().equals("complete")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == rewardNpcId)
			.toList();
		assertEquals(9, completionRoutes.size());
		assertEquals(Set.of(8, 9, 10, 11, 12, 13, 14, 15, 16), completionRoutes.stream()
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId())
			.collect(java.util.stream.Collectors.toSet()));
		assertTrue(completionRoutes.stream().allMatch(transition ->
			transition.actions().stream().anyMatch(action -> action.equals(
				new QuestAction.GrantReward("ITEM", 188055318, 1)))
				&& transition.actions().stream().anyMatch(action -> action instanceof QuestAction.GrantReward reward
					&& reward.rewardKind() == QuestRewardKind.ITEM
					&& selectableItems.contains(reward.id()))));
	}

	private static CompiledQuestDefinition load(int questId) {
		// 退役任务不再有 XML：统一走生产视图（保留任务解析内容不变，退役任务走真端 overlay）。
		// Retired quests have no XML: resolve through the production view (XML catalog + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
