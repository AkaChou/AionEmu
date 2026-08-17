package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖生产目录中所有 Aion 5.8 客户端可实时报告的任务奖励完成合同。
 * Covers reward-completion contracts for every production quest reportable by the Aion 5.8 client.
 */
class QuestReportedRewardCoverageTest {
	private static final Set<Integer> FIXED_QUEST_IDS = fixedQuestIds();
	private static final Set<Integer> CHOICE_QUEST_IDS = Set.of(
		13955, 13956,
		16821, 16822, 16824, 16825, 16836,
		23955, 23956,
		26821, 26822, 26824, 26825, 26836);
	private static final Set<Integer> CLASS_QUEST_IDS = Set.of(
		16801, 16802, 16803, 16804, 26801, 26802, 26803, 26804);
	private static final Set<Integer> EXPLICIT_CLASS_QUEST_IDS = Set.of(
		13830, 13831, 13832, 13833, 13834, 23830, 23831, 23832, 23833, 23834);

	@Test
	void all182ReportedQuestOwnersHaveTargetlessCompletionContracts() {
		QuestCatalog catalog = productionCatalog();
		Set<Integer> expected = new LinkedHashSet<>();
		expected.addAll(FIXED_QUEST_IDS);
		expected.addAll(CHOICE_QUEST_IDS);
		expected.addAll(CLASS_QUEST_IDS);
		expected.addAll(EXPLICIT_CLASS_QUEST_IDS);
		assertEquals(182, expected.size());

		for (int questId : FIXED_QUEST_IDS) {
			assertTargetlessCompletionRoutes(catalog.findExecutable(questId).orElseThrow(),
				QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id(), 1);
		}
		for (int questId : CHOICE_QUEST_IDS) {
			CompiledQuestDefinition compiled = catalog.findExecutable(questId).orElseThrow();
			List<QuestReward> choices = compiled.definition().metadata().rewards().stream()
				.filter(reward -> QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.SELECTABLE_ITEM)
				.toList();
			assertTrue(choices.size() >= 2 && choices.size() <= 15, "quest=" + questId);
			for (int slot = 0; slot < choices.size(); slot++) {
				assertTargetlessCompletionRoutes(compiled,
					QuestDialogAction.SELECTED_QUEST_REWARD1.id() + slot, 1);
				assertTargetlessCompletionRoutes(compiled,
					QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + slot, 1);
			}
		}
		for (int questId : union(CLASS_QUEST_IDS, EXPLICIT_CLASS_QUEST_IDS)) {
			CompiledQuestDefinition compiled = catalog.findExecutable(questId).orElseThrow();
			assertTargetlessCompletionRoutes(compiled, QuestDialogAction.SELECTED_QUEST_REWARD1.id(), 11);
			assertTargetlessCompletionRoutes(compiled, QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id(), 11);
		}
	}

	@Test
	void classRewardMetadataMatchesEveryReportedCompletionAction() {
		QuestCatalog catalog = productionCatalog();
		for (int questId : CLASS_QUEST_IDS) {
			CompiledQuestDefinition compiled = catalog.findExecutable(questId).orElseThrow();
			QuestMetadata metadata = compiled.definition().metadata();
			assertEquals(1, metadata.useClassReward(), "quest=" + questId);
			assertEquals(11, metadata.classRewards().size(), "quest=" + questId);
			for (QuestTransition transition : targetlessRoutes(compiled,
					QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id())) {
				PlayerClass playerClass = transition.conditions().stream()
					.filter(QuestCondition.AdvancedClassIs.class::isInstance)
					.map(QuestCondition.AdvancedClassIs.class::cast)
					.map(QuestCondition.AdvancedClassIs::playerClass)
					.findFirst().orElseThrow();
				QuestReward reward = metadata.classRewards().get(classRewardKey(playerClass)).getFirst();
				assertTrue(transition.actions().contains(new QuestAction.GrantReward(
					QuestRewardKind.ITEM.name(), reward.id(), reward.amount(), QuestRewardAmountMode.EXACT)),
					"quest=" + questId + " class=" + playerClass);
			}
		}
	}

	private static void assertTargetlessCompletionRoutes(CompiledQuestDefinition compiled,
			int dialogId, int expectedCount) {
		List<QuestTransition> routes = targetlessRoutes(compiled, dialogId);
		assertEquals(expectedCount, routes.size(), "quest=" + compiled.id() + " action=" + dialogId);
		Map<String, QuestStatus> statuses = new LinkedHashMap<>();
		for (QuestNode node : compiled.definition().nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		for (QuestTransition transition : routes) {
			assertEquals(QuestStatus.REWARD, statuses.get(transition.sourceNode()),
				"quest=" + compiled.id() + " action=" + dialogId);
			assertEquals(QuestStatus.COMPLETE, statuses.get(transition.targetNode()),
				"quest=" + compiled.id() + " action=" + dialogId);
			assertEquals(1, transition.actions().stream()
				.filter(QuestAction.CompleteQuest.class::isInstance).count());
			assertTrue(transition.afterCommit().getFirst() instanceof AfterCommitAction.RefreshPlayerStats);
			assertEquals(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				transition.afterCommit().get(1));
			assertTrue(transition.afterCommit().getLast() instanceof AfterCommitAction.CloseDialog);
			assertFalse(transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog
					|| action instanceof AfterCommitAction.ShowQuestSelectionDialog
					|| action instanceof AfterCommitAction.ShowDialogWindow));
		}
	}

	private static List<QuestTransition> targetlessRoutes(CompiledQuestDefinition compiled, int dialogId) {
		return compiled.definition().transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.QuestDialog(dialogId)))
			.toList();
	}

	private static QuestCatalog productionCatalog() {
		return QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest_definition"));
	}

	private static Set<Integer> fixedQuestIds() {
		Set<Integer> ids = new LinkedHashSet<>();
		addRange(ids, 1877, 1887);
		addRange(ids, 2877, 2887);
		add(ids, 13841, 13845, 13849, 13947);
		addRange(ids, 15671, 15673);
		addRange(ids, 15681, 15688);
		addRange(ids, 15710, 15717);
		addRange(ids, 16805, 16808);
		add(ids, 16823, 16832);
		addRange(ids, 16826, 16831);
		add(ids, 16839, 16931, 17511);
		addRange(ids, 19673, 19676);
		addRange(ids, 19680, 19682);
		add(ids, 23841, 23845, 23849, 23947);
		addRange(ids, 25671, 25673);
		addRange(ids, 25681, 25688);
		addRange(ids, 25710, 25717);
		addRange(ids, 26805, 26808);
		add(ids, 26823);
		addRange(ids, 26826, 26832);
		add(ids, 26839, 26931, 27511);
		addRange(ids, 29673, 29676);
		addRange(ids, 29680, 29682);
		add(ids, 50125, 51125);
		addRange(ids, 80709, 80714);
		addRange(ids, 80900, 80919);
		addRange(ids, 80979, 80988);
		return Set.copyOf(ids);
	}

	private static void addRange(Set<Integer> target, int first, int last) {
		for (int id = first; id <= last; id++) {
			target.add(id);
		}
	}

	private static void add(Set<Integer> target, int... ids) {
		for (int id : ids) {
			target.add(id);
		}
	}

	private static Set<Integer> union(Set<Integer> first, Set<Integer> second) {
		Set<Integer> result = new LinkedHashSet<>(first);
		result.addAll(second);
		return result;
	}

	private static String classRewardKey(PlayerClass playerClass) {
		return switch (playerClass) {
			case GLADIATOR -> "FIGHTER";
			case TEMPLAR -> "KNIGHT";
			case RANGER -> "RANGER";
			case ASSASSIN -> "ASSASSIN";
			case SORCERER -> "WIZARD";
			case SPIRIT_MASTER -> "ELEMENTALIST";
			case CLERIC -> "PRIEST";
			case CHANTER -> "CHANTER";
			case GUNSLINGER -> "GUNSLINGER";
			case SONGWEAVER -> "SONGWEAVER";
			case AETHERTECH -> "AETHERTECH";
			default -> throw new IllegalArgumentException("unsupported advanced class " + playerClass);
		};
	}
}
