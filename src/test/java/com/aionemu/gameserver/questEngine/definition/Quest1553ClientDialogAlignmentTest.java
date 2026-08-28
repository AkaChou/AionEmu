package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 1553 将起始接取、中间说话镜子注入魔力、佩兰托军团长询问与皮埃拉完成交付限定在各自正规 NPC。
 * Verifies quest 1553 confines its start, talking mirror magic infusion, Perento inquiry, and Piera reward completion to their retail NPC owners.
 */
class Quest1553ClientDialogAlignmentTest {
	private static final int START_NPC = 203786;
	private static final int TALKING_MIRROR_NPC = 730051;
	private static final int PERENTO_NPC = 204500;
	private static final int PIERA_NPC = 204584;

	private static final int INITIAL_MIRROR_ITEM = 182201794;
	private static final int INFUSED_MIRROR_ITEM = 182201795;

	@Test
	void verifiesQuest1553DefinitionContractAndOwnerIsolation() {
		QuestDefinition definition = load().definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "stage1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		// 1. 迪亚娜 (203786) 为唯一的接任务 NPC
		QuestTransition startDialog = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(startDialog, "unaccepted", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())));

		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(INITIAL_MIRROR_ITEM, 1)), accept.actions());

		QuestTransition inProgressDiana = route(definition, "started", START_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(inProgressDiana, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())));
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());

		// 2. 会说话的镜子 (730051) 仅作为第 1 步 (started) 交互对象，严禁作为 Start / Complete NPC
		assertTrue(routes(definition, "unaccepted", TALKING_MIRROR_NPC).isEmpty());
		assertTrue(routes(definition, "stage1", TALKING_MIRROR_NPC).isEmpty());
		assertTrue(routes(definition, "reward", TALKING_MIRROR_NPC).isEmpty());

		QuestTransition mirrorSelect2 = route(definition, "started", TALKING_MIRROR_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(mirrorSelect2, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));

		QuestTransition mirrorSelect21 = route(definition, "started", TALKING_MIRROR_NPC, QuestDialogAction.SELECT2_1);
		assertContract(mirrorSelect21, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())));

		QuestTransition mirrorSetpro1 = route(definition, "started", TALKING_MIRROR_NPC, QuestDialogAction.SETPRO1);
		assertEquals("stage1", mirrorSetpro1.targetNode());
		assertEquals(List.of(
			new QuestAction.RemoveItem(INITIAL_MIRROR_ITEM, 1),
			new QuestAction.GiveItem(INFUSED_MIRROR_ITEM, 1)
		), mirrorSetpro1.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()
		), mirrorSetpro1.afterCommit());

		// 3. 佩兰托 (204500) 仅作为第 2 步 (stage1) 交互对象，严禁作为 Start / Complete NPC
		assertTrue(routes(definition, "unaccepted", PERENTO_NPC).isEmpty());
		assertTrue(routes(definition, "started", PERENTO_NPC).isEmpty());
		assertTrue(routes(definition, "reward", PERENTO_NPC).isEmpty());

		QuestTransition perentoSelect3 = route(definition, "stage1", PERENTO_NPC, QuestDialogAction.QUEST_SELECT);
		assertContract(perentoSelect3, "stage1", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())));

		QuestTransition perentoSelect31 = route(definition, "stage1", PERENTO_NPC, QuestDialogAction.SELECT3_1);
		assertContract(perentoSelect31, "stage1", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())));

		QuestTransition perentoSetpro2 = route(definition, "stage1", PERENTO_NPC, QuestDialogAction.SETPRO2);
		assertEquals("reward", perentoSetpro2.targetNode());
		assertEquals(List.of(), perentoSetpro2.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()
		), perentoSetpro2.afterCommit());

		// 4. 皮埃拉 (204584) 仅作为第 3 步奖励交付 NPC，严禁作为 Start NPC
		assertTrue(routes(definition, "unaccepted", PIERA_NPC).isEmpty());
		assertTrue(routes(definition, "started", PIERA_NPC).isEmpty());
		assertTrue(routes(definition, "stage1", PIERA_NPC).isEmpty());

		QuestTransition pieraSelect5 = route(definition, "reward", PIERA_NPC, QuestDialogAction.USE_OBJECT);
		assertContract(pieraSelect5, "reward", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())));

		QuestTransition pieraPreview = route(definition, "reward", PIERA_NPC, QuestDialogAction.SELECT_QUEST_REWARD);
		assertContract(pieraPreview, "reward", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));

		List<QuestTransition> completionRoutes = routes(definition, "reward", PIERA_NPC).stream()
			.filter(transition -> {
				Integer dialogId = ((QuestEvent.TalkToNpc) transition.event()).dialogId();
				return dialogId != null && dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& dialogId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
			})
			.toList();
		assertEquals(16, completionRoutes.size());
		for (QuestTransition completion : completionRoutes) {
			assertContract(completion, "complete", List.of(
				new QuestAction.GrantReward("EXP", 0, 2954681, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("AP", 0, 200, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)
			), List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())
			));
		}
	}

	private static void assertContract(QuestTransition transition, String target,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
		assertNull(transition.priority());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size(), "quest 1553 " + source + " " + npcId + " " + action);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/1553.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest1553ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
