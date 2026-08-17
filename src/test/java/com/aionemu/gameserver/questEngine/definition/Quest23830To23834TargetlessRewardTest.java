package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestEventIndex;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 五个阿斯莫升级型职业奖励任务的无目标奖励确认合同。
 * Targetless reward-confirmation contract for the five Asmodian level-up class-reward quests.
 */
class Quest23830To23834TargetlessRewardTest {
	private static final List<PlayerClass> CLASSES = List.of(
		PlayerClass.GLADIATOR, PlayerClass.TEMPLAR, PlayerClass.RANGER, PlayerClass.ASSASSIN,
		PlayerClass.SORCERER, PlayerClass.SPIRIT_MASTER, PlayerClass.CLERIC, PlayerClass.CHANTER,
		PlayerClass.GUNSLINGER, PlayerClass.SONGWEAVER, PlayerClass.AETHERTECH);

	private static final List<Spec> SPECS = List.of(
		new Spec(23830, 182216123, 46544, false, false, false,
			List.of(140001109, 140001130, 140001168, 140001145, 140001189, 140001202,
				140001236, 140001221, 140001253, 140001289, 140001271)),
		new Spec(23831, 182216124, 337255, false, false, false,
			List.of(140001111, 140001128, 140001171, 140001142, 140001186, 140001203,
				140001240, 140001220, 140001254, 140001290, 140001275)),
		new Spec(23832, 182216125, 555019, false, true, false,
			List.of(140001105, 140001123, 140001154, 140001136, 140001177, 140001196,
				140001231, 140001216, 140001249, 140001284, 140001264)),
		new Spec(23833, 182216126, 731094, false, false, false,
			List.of(140001103, 140001124, 140001156, 140001137, 140001176, 140001198,
				140001228, 140001214, 140001247, 140001282, 140001265)),
		new Spec(23834, 182216127, 1005193, true, true, true,
			List.of(140001118, 140001135, 140001173, 140001151, 140001192, 140001210,
				140001245, 140001227, 140001262, 140001296, 140001279)));

	@Test
	void compilesNormalAndRealtimeTargetlessRewardsWithClassRewardsAndCloseOrder() throws Exception {
		for (Spec spec : SPECS) {
			CompiledQuestDefinition compiled = load(spec.questId());
			QuestEventIndex eventIndex = new QuestEventIndex(new ImmutableQuestCatalog(List.of(compiled)));
			for (int classIndex = 0; classIndex < CLASSES.size(); classIndex++) {
				PlayerClass playerClass = CLASSES.get(classIndex);
				int rewardId = spec.rewardIds().get(classIndex);
				assertTargetlessRoute(compiled, eventIndex, spec, playerClass, rewardId,
					QuestDialogAction.SELECTED_QUEST_REWARD1.id(), spec.targetlessPriority(classIndex));
				assertTargetlessRoute(compiled, eventIndex, spec, playerClass, rewardId,
					QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id(), spec.targetlessPriority(classIndex));

				QuestTransition npc = route(compiled, new QuestEvent.TalkToNpc(204061, 8), playerClass,
					spec.npcPriority(classIndex));
				assertEquals("reward", npc.sourceNode());
				assertEquals("complete", npc.targetNode());
				assertEquals(expectedActions(spec, rewardId), npc.actions());
				assertEquals(List.of(new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
					npc.afterCommit());
			}
		}
	}

	private static void assertTargetlessRoute(CompiledQuestDefinition compiled, QuestEventIndex index,
			Spec spec, PlayerClass playerClass, int rewardId, int dialogId, Integer priority) {
		QuestEvent event = new QuestEvent.QuestDialog(dialogId);
		assertTrue(index.routesFor(event, spec.questId()).stream()
			.anyMatch(candidate -> candidate.transition().conditions()
				.contains(new QuestCondition.AdvancedClassIs(playerClass))));
		QuestTransition targetless = route(compiled, event, playerClass, priority);

		assertEquals("reward", targetless.sourceNode());
		assertEquals("complete", targetless.targetNode());
		assertEquals(expectedActions(spec, rewardId), targetless.actions());
		assertEquals(List.of(new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.CloseDialog()), targetless.afterCommit());

		var source = compiled.definition().nodes().stream()
			.filter(node -> node.label().equals("reward")).findFirst().orElseThrow();
		var complete = compiled.definition().nodes().stream()
			.filter(node -> node.label().equals("complete")).findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, source.projection().status());
		assertEquals(Map.of("var0", 1), source.projection().variables());
		assertEquals(QuestStatus.COMPLETE, complete.projection().status());
		assertEquals(Map.of("var0", 0), complete.projection().variables());

		int packed = compiled.definition().progressLayout().pack(Map.of("var0", 1));
		QuestSnapshot snapshot = new QuestSnapshot(7, spec.questId(), QuestStatus.REWARD, packed,
			Map.of(spec.workItem(), 1)).withPlayerClass(playerClass);
		var plan = QuestMutationPlanner.plan(compiled, snapshot, event, targetless).orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(compiled.definition().progressLayout().pack(Map.of("var0", 0)),
			plan.nextPackedVariables());
		assertEquals(targetless.actions(), plan.requiredActions());
		assertEquals(targetless.afterCommit(), plan.afterCommit());
	}

	private static List<QuestAction> expectedActions(Spec spec, int rewardId) {
		QuestAction item = new QuestAction.GrantReward("ITEM", rewardId, 1);
		QuestAction exp = new QuestAction.GrantReward("EXP", 0, spec.exp(), QuestRewardAmountMode.QUEST_BASE);
		QuestAction removeItem = new QuestAction.RemoveItem(spec.workItem(), QuestAction.RemoveItem.ALL);
		QuestAction complete = new QuestAction.CompleteQuest(0);
		return spec.itemFirst() ? List.of(item, exp, removeItem, complete) : List.of(exp, item, removeItem, complete);
	}

	private static QuestTransition route(CompiledQuestDefinition compiled, QuestEvent event,
			PlayerClass playerClass, Integer priority) {
		return compiled.definition().transitions().stream()
			.filter(transition -> transition.event().equals(event))
			.filter(transition -> transition.sourceNode().equals("reward"))
			.filter(transition -> transition.conditions().contains(new QuestCondition.AdvancedClassIs(playerClass)))
			.filter(transition -> priority == null || priority.equals(transition.priority()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + questId + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record Spec(int questId, int workItem, long exp, boolean indexedTargetlessPriority,
		boolean indexedNpcPriority, boolean itemFirst, List<Integer> rewardIds) {
		private Integer targetlessPriority(int index) {
			return indexedTargetlessPriority ? index : null;
		}

		private Integer npcPriority(int index) {
			return indexedNpcPriority ? index : null;
		}
	}
}
