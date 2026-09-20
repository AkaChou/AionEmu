package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定首次守护者装备任务的三阶段链和独占领奖，并防止重复任务在交物后再次扣除材料。
 * Guards initial Daevanion phases and exclusive rewards, plus single material consumption in repeatable variants.
 */
class QuestDaevanionThreeStageFlowTest {

	@ParameterizedTest
	@MethodSource("contracts")
	void clientQuestRowIsHandledImmediatelyAfterClientAcceptance(Contract contract) throws Exception {
		CompiledQuestDefinition compiled = definition(contract.questId());
		// 鞋任务还支持日志中的快捷接取，其余六条客户端仅发送快捷接取。
		// Shoe quests also support the logged simple acceptance; the other six quest pages only expose simple acceptance.
		List<QuestDialogAction> acceptanceActions = contract.introDepth() == 3
			? List.of(QuestDialogAction.QUEST_ACCEPT_SIMPLE, QuestDialogAction.QUEST_ACCEPT_1)
			: List.of(QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		for (QuestDialogAction action : acceptanceActions) {
			QuestTransition accept = route(compiled, "unaccepted", contract.issuer(), action, null);
			assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
			assertEquals(List.of(), accept.actions());
			QuestMutationPlan accepted = handled(compiled, snapshot(compiled, QuestStatus.NONE, 0, Map.of()),
				contract.issuer(), action);
			assertState(compiled, accepted, QuestStatus.START, 0);
			AfterCommitAction response = contract.introDepth() == 3 && action == QuestDialogAction.QUEST_ACCEPT_1
				? new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())
				: new AfterCommitAction.CloseDialog();
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				response), accepted.afterCommit());

			QuestMutationPlan firstDialog = handled(compiled,
				snapshot(compiled, accepted.nextStatus(), 0, Map.of()), contract.worker(), QuestDialogAction.QUEST_SELECT);
			assertState(compiled, firstDialog, QuestStatus.START, 0);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), firstDialog.afterCommit());
		}
	}

	@ParameterizedTest
	@MethodSource("contracts")
	void nodesAndClientPagesKeepAllThreePhasesReachable(Contract contract) throws Exception {
		CompiledQuestDefinition compiled = definition(contract.questId());
		assertNode(compiled, "unaccepted", QuestStatus.NONE, 0);
		for (int stage = 0; stage <= 2; stage++) {
			assertNode(compiled, "s" + stage, QuestStatus.START, stage);
		}
		assertNode(compiled, "reward", QuestStatus.REWARD, 3);
		assertNode(compiled, "complete", QuestStatus.COMPLETE, 0);
		assertEquals(6, compiled.definition().nodes().size());
		assertPage(compiled, "s0", contract.worker(), QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertPage(compiled, "s0", contract.worker(), QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		if (contract.introDepth() == 3) {
			assertPage(compiled, "s0", contract.worker(), QuestDialogAction.SELECT1_1_1, QuestDialogPage.SELECT1_1_1);
			assertPage(compiled, "s0", contract.worker(), QuestDialogAction.SELECT1_1_1_1,
				QuestDialogPage.SELECT1_1_1_1);
		}
		QuestTransition introduction = route(compiled, "s0", contract.worker(), QuestDialogAction.SETPRO1, null);
		assertEquals("s1", introduction.targetNode());
		assertEquals(List.of(), introduction.conditions());
		assertEquals(List.of(), introduction.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), introduction.afterCommit());
		assertState(compiled, handled(compiled, snapshot(compiled, QuestStatus.START, 0, Map.of()),
			contract.worker(), QuestDialogAction.SETPRO1), QuestStatus.START, 1);

		assertPage(compiled, "s1", contract.worker(), QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		List<QuestDialogAction> information = List.of(QuestDialogAction.SELECT2_1, QuestDialogAction.SELECT2_2,
			QuestDialogAction.SELECT2_3);
		List<QuestDialogPage> informationPages = List.of(QuestDialogPage.SELECT2_1, QuestDialogPage.SELECT2_2,
			QuestDialogPage.SELECT2_3);
		for (int index = 0; index < contract.informationPages(); index++) {
			assertPage(compiled, "s1", contract.worker(), information.get(index), informationPages.get(index));
		}
		QuestTransition close = route(compiled, "s1", contract.worker(), QuestDialogAction.FINISH_DIALOG, null);
		assertEquals("s1", close.targetNode());
		assertEquals(List.of(), close.conditions());
		assertEquals(List.of(), close.actions());
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), close.afterCommit());
		assertPage(compiled, "s2", contract.worker(), QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertPage(compiled, "s2", contract.worker(), QuestDialogAction.SELECT3, QuestDialogPage.SELECT3);
		assertPage(compiled, "s2", contract.worker(), QuestDialogAction.SELECT3_1, QuestDialogPage.SELECT3_1);
		assertPage(compiled, "s2", contract.worker(), QuestDialogAction.SELECT3_1_1, QuestDialogPage.SELECT3_1_1);
	}

	@ParameterizedTest
	@MethodSource("contracts")
	void handoverRequiresEveryMaterialAndNeverConsumesItTwice(Contract contract) throws Exception {
		CompiledQuestDefinition compiled = definition(contract.questId());
		QuestTransition success = route(compiled, "s1", contract.worker(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 0);
		assertEquals("s2", success.targetNode());
		assertEquals(contract.materials().stream()
			.map(material -> new QuestCondition.HasItem(material.itemId(), material.count())).toList(), success.conditions());
		assertEquals(contract.materials().stream()
			.map(material -> new QuestAction.RemoveItem(material.itemId(), material.count())).toList(), success.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), success.afterCommit());
		QuestTransition failure = route(compiled, "s1", contract.worker(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, 1);
		assertEquals("s1", failure.targetNode());
		assertEquals(List.of(), failure.conditions());
		assertEquals(List.of(), failure.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			failure.afterCommit());
		for (Material material : contract.materials()) {
			Map<Integer, Integer> shortInventory = inventory(contract);
			shortInventory.put(material.itemId(), material.count() - 1);
			QuestMutationPlan rejected = handled(compiled,
				snapshot(compiled, QuestStatus.START, 1, shortInventory), contract.worker(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
			assertState(compiled, rejected, QuestStatus.START, 1);
			assertEquals(List.of(), rejected.requiredActions());
			assertEquals(failure.afterCommit(), rejected.afterCommit());
		}
		QuestMutationPlan handover = handled(compiled,
			snapshot(compiled, QuestStatus.START, 1, inventory(contract)), contract.worker(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertState(compiled, handover, QuestStatus.START, 2);
		assertEquals(success.actions(), handover.requiredActions());

		QuestTransition finish = route(compiled, "s2", contract.worker(), QuestDialogAction.SET_SUCCEED, null);
		assertEquals("reward", finish.targetNode());
		assertEquals(List.of(), finish.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(contract.workItem(), 1)), finish.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), finish.afterCommit());
		// 材料已在交付阶段扣完，收尾必须仍可执行。 / Finishing must work after the handover emptied the materials.
		QuestMutationPlan finished = handled(compiled, snapshot(compiled, QuestStatus.START, 2, Map.of()),
			contract.worker(), QuestDialogAction.SET_SUCCEED);
		assertState(compiled, finished, QuestStatus.REWARD, 3);
		assertEquals(finish.actions(), finished.requiredActions());
		assertTrue(plan(compiled, snapshot(compiled, QuestStatus.REWARD, 3, Map.of(contract.workItem(), 1)),
			contract.worker(), QuestDialogAction.SET_SUCCEED).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("contracts")
	void wrongOwnerOrOutOfOrderActionsCannotSkipTheMaterialPhase(Contract contract) throws Exception {
		CompiledQuestDefinition compiled = definition(contract.questId());
		for (int stage = 0; stage <= 2; stage++) {
			QuestSnapshot snapshot = snapshot(compiled, QuestStatus.START, stage, inventory(contract));
			for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_SELECT,
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, QuestDialogAction.SELECT_QUEST_REWARD,
				QuestDialogAction.SELECTED_QUEST_REWARD1)) {
				assertTrue(plan(compiled, snapshot, contract.issuer(), action).isEmpty(),
					() -> "issuer must not bypass the worker via " + action);
			}
			assertTrue(plan(compiled, snapshot, contract.worker(), QuestDialogAction.SELECT_QUEST_REWARD).isEmpty());
			if (stage != 0) {
				assertTrue(plan(compiled, snapshot, contract.worker(), QuestDialogAction.SETPRO1).isEmpty());
			}
			if (stage != 1) {
				assertTrue(plan(compiled, snapshot, contract.worker(), QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).isEmpty());
			}
			if (stage != 2) {
				assertTrue(plan(compiled, snapshot, contract.worker(), QuestDialogAction.SELECT3).isEmpty());
				assertTrue(plan(compiled, snapshot, contract.worker(), QuestDialogAction.SET_SUCCEED).isEmpty());
			}
		}
		assertTrue(plan(compiled, snapshot(compiled, QuestStatus.NONE, 0, Map.of()),
			contract.worker(), QuestDialogAction.QUEST_SELECT).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("contracts")
	void allSevenRewardChoicesBelongOnlyToTheIssuer(Contract contract) throws Exception {
		CompiledQuestDefinition compiled = definition(contract.questId());
		assertPage(compiled, "reward", contract.issuer(), QuestDialogAction.QUEST_SELECT, QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(compiled, "reward", contract.issuer(), QuestDialogAction.USE_OBJECT,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertPage(compiled, "reward", contract.issuer(), QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		for (int choice = 1; choice <= 7; choice++) {
			QuestDialogAction action = QuestDialogAction.valueOf("SELECTED_QUEST_REWARD" + choice);
			QuestTransition reward = route(compiled, "reward", contract.issuer(), action, null);
			QuestReward selected = compiled.definition().metadata().rewards().get(choice + 1);
			assertEquals("complete", reward.targetNode());
			assertEquals(List.of(), reward.conditions());
			assertEquals(List.of(new QuestAction.GrantReward("GOLD", 0, 1807920, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 8961038, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", selected.id(), selected.amount()),
				new QuestAction.CompleteQuest(0)), reward.actions());
			assertEquals(List.of(new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())), reward.afterCommit());
			QuestSnapshot snapshot = snapshot(compiled, QuestStatus.REWARD, 3, Map.of(contract.workItem(), 1));
			QuestMutationPlan completion = handled(compiled, snapshot, contract.issuer(), action);
			assertState(compiled, completion, QuestStatus.COMPLETE, 0);
			List<QuestAction> expected = new ArrayList<>(reward.actions());
			expected.add(new QuestAction.RemoveItem(contract.workItem(), QuestAction.RemoveItem.ALL));
			assertEquals(expected, completion.requiredActions());
			assertTrue(plan(compiled, snapshot, contract.worker(), action).isEmpty());
		}
	}

	private static void assertPage(CompiledQuestDefinition compiled, String source, int npcId,
								   QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(compiled, source, npcId, action, null);
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
		QuestNode node = compiled.definition().nodes().stream()
			.filter(candidate -> source.equals(candidate.label())).findFirst().orElseThrow();
		int stage = node.projection().variables().get("var0");
		QuestMutationPlan result = handled(compiled,
			snapshot(compiled, node.projection().status(), stage, Map.of()), npcId, action);
		assertState(compiled, result, node.projection().status(), stage);
	}

	@ParameterizedTest
	@MethodSource("repeatContracts")
	void repeatableVariantsDoNotChargeMaterialsAgainAfterHandover(Contract contract) throws Exception {
		CompiledQuestDefinition compiled = definition(contract.questId());
		QuestMutationPlan handover = handled(compiled,
			snapshot(compiled, QuestStatus.START, 1, inventory(contract)), contract.worker(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertState(compiled, handover, QuestStatus.START, 2);
		List<QuestAction> debits = new ArrayList<>();
		contract.materials().forEach(material -> debits.add(new QuestAction.RemoveItem(material.itemId(), material.count())));
		debits.add(new QuestAction.SetVariable("var0", 2));
		assertEquals(debits, handover.requiredActions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), handover.afterCommit());
		QuestTransition finish = route(compiled, "s2", contract.worker(), QuestDialogAction.SET_SUCCEED, null);
		assertEquals("reward", finish.targetNode());
		assertEquals(List.of(), finish.conditions());
		QuestMutationPlan result = handled(compiled, snapshot(compiled, QuestStatus.START, 2, Map.of()),
			contract.worker(), QuestDialogAction.SET_SUCCEED);
		assertEquals(List.of(new QuestAction.GiveItem(contract.workItem(), 1),
			new QuestAction.SetVariable("var0", 3)), finish.actions());
		assertEquals(finish.actions(), result.requiredActions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), result.afterCommit());
		assertState(compiled, result, QuestStatus.REWARD, 3);
	}

	private static QuestTransition route(CompiledQuestDefinition compiled, String source, int npcId,
										 QuestDialogAction action, Integer priority) {
		List<QuestTransition> matches = compiled.definition().transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.filter(candidate -> Objects.equals(priority, candidate.priority())).toList();
		assertEquals(1, matches.size(), () -> compiled.id() + " " + source + " " + npcId + " " + action);
		QuestTransition transition = matches.getFirst();
		if (priority == null) {
			assertNull(transition.priority());
		}
		return transition;
	}

	private static Optional<QuestMutationPlan> plan(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
													int npcId, QuestDialogAction action) {
		QuestEvent event = new QuestEvent.TalkToNpc(npcId, action.id());
		return compiled.definition().transitions().stream()
			.filter(candidate -> QuestEvent.matches(candidate.event(), event))
			.sorted(Comparator.comparingInt(candidate -> candidate.priority() == null ? 0 : candidate.priority()))
			.map(candidate -> QuestMutationPlanner.plan(compiled, snapshot, event, candidate))
			.flatMap(Optional::stream).findFirst();
	}

	private static QuestMutationPlan handled(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
											 int npcId, QuestDialogAction action) {
		Optional<QuestMutationPlan> result = plan(compiled, snapshot, npcId, action);
		assertTrue(result.isPresent(), () -> compiled.id() + " unhandled " + action + " at NPC " + npcId
			+ " in " + snapshot.status() + "/" + snapshot.packedVariables());
		return result.orElseThrow();
	}

	private static void assertState(CompiledQuestDefinition compiled, QuestMutationPlan plan,
									QuestStatus status, int stage) {
		assertEquals(status, plan.nextStatus());
		assertEquals(Map.of("var0", stage), compiled.definition().progressLayout().unpack(plan.nextPackedVariables()));
	}

	private static void assertNode(CompiledQuestDefinition compiled, String label, QuestStatus status, int stage) {
		QuestNode node = compiled.definition().nodes().stream()
			.filter(candidate -> label.equals(candidate.label())).findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", stage), node.projection().variables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status, int stage,
										  Map<Integer, Integer> inventory) {
		return new QuestSnapshot(7, compiled.id(), status,
			compiled.definition().progressLayout().pack(Map.of("var0", stage)), inventory, Map.of(),
			true, true, 65709, 65709, 110010000, 1, 0f, 0f, 0f, (byte) 0)
			.withStartEligibility(QuestStartEligibility.allowed()).withCompletedQuestIds(Set.of(compiled.id() - 1));
	}

	private static Map<Integer, Integer> inventory(Contract contract) {
		Map<Integer, Integer> inventory = new LinkedHashMap<>();
		contract.materials().forEach(material -> inventory.put(material.itemId(), material.count()));
		return inventory;
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = QuestDaevanionThreeStageFlowTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest " + questId);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static Stream<Contract> contracts() {
		return Stream.of(
			new Contract(15301, 805327, 805328, 182215859, 3, 0,
				List.of(new Material(182215829, 1), new Material(182215830, 1), new Material(182215831, 1))),
			new Contract(15302, 805327, 805329, 182215860, 1, 0, List.of(new Material(182215832, 70))),
			new Contract(15303, 805327, 805328, 182215861, 1, 3,
				List.of(new Material(152003017, 150), new Material(182215833, 40),
					new Material(182215883, 50), new Material(182215884, 50))),
			new Contract(15305, 805327, 805329, 182215863, 1, 2,
				List.of(new Material(152003019, 30), new Material(182215887, 80))),
			new Contract(25301, 805339, 805340, 182215871, 3, 0,
				List.of(new Material(182215844, 1), new Material(182215845, 1), new Material(182215846, 1))),
			new Contract(25302, 805339, 805341, 182215872, 1, 0, List.of(new Material(182215847, 70))),
			new Contract(25303, 805339, 805340, 182215873, 1, 3,
				List.of(new Material(152003018, 150), new Material(182215848, 40),
					new Material(182215890, 50), new Material(182215891, 50))),
			new Contract(25305, 805339, 805341, 182215875, 1, 2,
				List.of(new Material(152003019, 30), new Material(182215894, 80))));
	}

	private static Stream<Contract> repeatContracts() {
		return Stream.of(
			new Contract(15311, 805327, 805328, 182215865, 1, 0,
				List.of(new Material(182215829, 1), new Material(182215830, 1), new Material(182215831, 1))),
			new Contract(15312, 805327, 805329, 182215866, 1, 0, List.of(new Material(182215832, 70))),
			new Contract(15313, 805327, 805328, 182215867, 1, 3,
				List.of(new Material(152003017, 150), new Material(182215833, 40),
					new Material(182215885, 50), new Material(182215886, 50))),
			new Contract(15315, 805327, 805329, 182215869, 1, 2,
				List.of(new Material(152003019, 30), new Material(182215888, 80))),
			new Contract(25311, 805339, 805340, 182215877, 1, 0,
				List.of(new Material(182215844, 1), new Material(182215845, 1), new Material(182215846, 1))),
			new Contract(25312, 805339, 805341, 182215878, 1, 0, List.of(new Material(182215847, 70))),
			new Contract(25313, 805339, 805340, 182215879, 1, 3,
				List.of(new Material(152003018, 150), new Material(182215848, 40),
					new Material(182215892, 50), new Material(182215893, 50))),
			new Contract(25315, 805339, 805341, 182215881, 1, 2,
				List.of(new Material(152003019, 30), new Material(182215895, 80))));
	}

	/**
	 * 权威材料数量，不从待测 XML 推导。 / Authoritative material counts independent of the XML under test.
	 */
	private record Material(int itemId, int count) {
	}

	/**
	 * 任务的 NPC、道具及客户端页链合同。 / NPC, item and client-page contract for one quest.
	 */
	private record Contract(int questId, int issuer, int worker, int workItem, int introDepth,
							int informationPages, List<Material> materials) {
	}
}
