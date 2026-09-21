package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
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

class QuestMinionTutorialRetailAlignmentTest {
	@Test
	void tutorialOwnersUseTheRetailQuestWorkItemsAndItemPlayLifecycle() {
		assertTutorial(19900, 1007, 190080020, 836073);
		assertTutorial(29900, 2009, 190080021, 836074);
	}

	@Test
	void archDaevaMinionChainGrantsItsOwnContractOnAccept() {
		// 66+ 守护灵教学链：接取时必须发放本任务专属契约书，MinionService 只认该 id 才会把任务推进到 REWARD。
		// Level-66+ minion tutorial chain: accepting must grant the quest-specific contract, because
		// MinionService only advances the quest to REWARD for that exact item id.
		assertAcceptGrant(15545, 835514, 190080010, List.of(new QuestReward("ITEM", 190080012, 1)));
		assertAcceptGrant(25545, 835515, 190080011, List.of(new QuestReward("ITEM", 190080012, 1)));
	}

	@Test
	void legacyAcceptItemGrantsSurviveTheTypedMigration() {
		// 这三个任务的旧 handler 都在接取分支 giveQuestItem，而当前目录没有任何其它产出源；
		// 缺少发放会让任务道具在交出/完成页根本不存在。奖励物品不在此断言范围。
		// Their legacy handlers all granted the work item in the accept branch and the production catalog has
		// no other source, so a dropped grant leaves the hand-in step with nothing to hand over. Reward items
		// are intentionally not asserted here.
		assertAcceptGrant(2266, 203558, 182203244, null);
		assertAcceptGrant(3085, 798144, 182208048, null);
		assertAcceptGrant(28808, 830392, 182213216, null);
	}

	private static void assertAcceptGrant(int questId, int npcId, int workItemId, List<QuestReward> expectedRewards) {
		QuestDefinition definition = load(questId).definition();

		assertEquals(List.of(new QuestItemRequirement(workItemId, 1)),
			definition.metadata().questWorkItems(), "quest " + questId + " quest work items");
		if (expectedRewards != null) {
			assertEquals(expectedRewards, definition.metadata().rewards(), "quest " + questId + " rewards");
		}

		for (QuestDialogAction acceptAction : List.of(
			QuestDialogAction.QUEST_ACCEPT_1, QuestDialogAction.QUEST_ACCEPT_SIMPLE)) {
			QuestTransition accept = definition.transitions().stream()
				.filter(t -> Objects.equals(t.sourceNode(), "unaccepted") && t.targetNode().equals("started"))
				.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
					&& Integer.valueOf(acceptAction.id()).equals(talk.dialogId()))
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + questId + "缺少 NPC " + npcId + " 的接取动作 " + acceptAction));
			assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions(),
				"quest " + questId + " accept conditions");
			assertTrue(accept.actions().contains(new QuestAction.GiveItem(workItemId, 1)),
				"quest " + questId + " 接取时必须发放任务工作物品 " + workItemId);
		}
	}

	private static void assertTutorial(int questId, int prerequisiteId, int workItemId, int npcId) {
		CompiledQuestDefinition compiled = load(questId);
		QuestDefinition definition = compiled.definition();

		assertTrue(definition.metadata().itemRequirements().isEmpty());
		assertEquals(Set.of(prerequisiteId), definition.metadata().prerequisites());
		assertTrue(definition.metadata().startConditions().stream()
			.noneMatch(condition -> condition.questId() == prerequisiteId));
		assertEquals(List.of(new QuestReward("ITEM", 190080012, 1)), definition.metadata().rewards());
		assertEquals(List.of(new QuestItemRequirement(workItemId, 1)),
			definition.metadata().questWorkItems());

		assertStartTransition(definition, new QuestEvent.LevelUp(), workItemId);
		assertStartTransition(definition, new QuestEvent.EnterWorld(), workItemId);

		QuestTransition itemPlay = definition.transitions().stream()
			.filter(t -> Objects.equals(t.sourceNode(), "started") && t.targetNode().equals("reward")
				&& t.event() instanceof QuestEvent.ItemPlay play
				&& play.itemId() == workItemId)
			.findFirst().orElseThrow();
		assertEquals(1500, ((QuestEvent.ItemPlay) itemPlay.event()).animationMillis());
		assertTrue(itemPlay.actions().isEmpty());
		var completedContractPlan = QuestMutationPlanner.plan(compiled,
			new QuestSnapshot(7, questId, QuestStatus.START, 0, Map.of()), itemPlay.event(), itemPlay)
			.orElseThrow();
		assertEquals(QuestStatus.REWARD, completedContractPlan.nextStatus());
		assertEquals(1, completedContractPlan.nextPackedVariables());
		assertTrue(completedContractPlan.requiredActions().isEmpty());
		assertFalse(definition.transitions().stream().anyMatch(t ->
			t.event() instanceof QuestEvent.UseItem use && use.itemId() == workItemId));

		assertTrue(definition.transitions().stream().anyMatch(t ->
			Objects.equals(t.sourceNode(), "unaccepted") && t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(31).equals(talk.dialogId())));
		assertTrue(definition.nodes().stream()
			.filter(n -> n.label().equals("reward") || n.label().equals("complete"))
			.allMatch(n -> n.projection().variables().get("var0") == 1));
		assertEquals(0, definition.nodes().stream()
			.filter(n -> n.label().equals("legacy-reward"))
			.findFirst().orElseThrow().projection().variables().get("var0"));
		assertEquals(QuestStatus.REWARD, definition.nodes().stream()
			.filter(n -> n.label().equals("reward"))
			.findFirst().orElseThrow().projection().status());
	}

	private static void assertStartTransition(QuestDefinition definition, QuestEvent event, int workItemId) {
		QuestTransition start = definition.transitions().stream()
			.filter(t -> Objects.equals(t.sourceNode(), "unaccepted") && t.targetNode().equals("started")
				&& t.event().equals(event))
			.findFirst().orElseThrow();
		assertTrue(start.actions().contains(new QuestAction.GiveItem(workItemId, 1)));
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestMinionTutorialRetailAlignmentTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new AssertionError("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("failed to load " + resource, e);
		}
	}
}
