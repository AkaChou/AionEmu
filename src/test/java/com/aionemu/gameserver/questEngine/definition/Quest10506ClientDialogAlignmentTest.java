package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 10506 的客户端页面链与进攻回廊传送门契约。
 * Verifies quest 10506 client dialog chain and invasion corridor portal contract.
 */
class Quest10506ClientDialogAlignmentTest {
	private static final int DIALOG_NPC = 804710;
	private static final int CORRIDOR_ENTRANCE = 702666;
	private static final int CORRIDOR_EXIT = 702667;

	@Test
	void keepsTheSelect8ChainAndSetSucceedCompletionRoute() {
		QuestDefinition definition = load().definition();

		assertPage(definition, "s7", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT8);
		assertPage(definition, "s7", QuestDialogAction.SELECT8_1, QuestDialogPage.SELECT8_1);
		assertFalse(hasRoute(definition, "s7", QuestDialogAction.SELECT8_1_1));

		QuestTransition completeStep = route(definition, "s7", QuestDialogAction.SET_SUCCEED);
		assertEquals("reward", completeStep.targetNode());
		assertEquals(List.of(
			new QuestAction.GiveItem(182215613, 1),
			new QuestAction.SetVariable("var0", 8)), completeStep.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), completeStep.afterCommit());
	}

	@Test
	void corridorPortalsOnlyAvailableInStep5AndStep6() {
		QuestDefinition definition = load().definition();

		// 步骤 3 (s2: 去找布里特拉进攻回廊) 尚未消灭守卫参谋兵，禁止提前交互与传送
		assertFalse(hasTalkRoute(definition, "s2", CORRIDOR_ENTRANCE, QuestDialogAction.USE_OBJECT));
		assertFalse(hasTalkRoute(definition, "s2", CORRIDOR_EXIT, QuestDialogAction.USE_OBJECT));
		assertFalse(hasCanActRoute(definition, "s2", CORRIDOR_ENTRANCE));
		assertFalse(hasCanActRoute(definition, "s2", CORRIDOR_EXIT));

		// 步骤 5 (s4: 通过布里特拉进攻回廊追踪布里特拉) 允许使用 702666 传送进入回廊内部
		assertTrue(hasCanActRoute(definition, "s4", CORRIDOR_ENTRANCE));
		assertTrue(hasCanActRoute(definition, "s4", CORRIDOR_EXIT));
		QuestTransition s4Enter = talkRoute(definition, "s4", CORRIDOR_ENTRANCE, QuestDialogAction.USE_OBJECT);
		assertEquals("s4", s4Enter.targetNode());
		assertEquals(List.of(new AfterCommitAction.TeleportPlayer(210070000, 2837.0f, 2991.0f, 680.0f, (byte) 67)),
			s4Enter.afterCommit());

		// 步骤 6 (s5: 是陷阱！逃出布里特拉进攻回廊，然后和普诺埃对话) 允许使用 702667 传送离开回廊返回外界
		assertTrue(hasCanActRoute(definition, "s5", CORRIDOR_ENTRANCE));
		assertTrue(hasCanActRoute(definition, "s5", CORRIDOR_EXIT));
		QuestTransition s5Exit = talkRoute(definition, "s5", CORRIDOR_EXIT, QuestDialogAction.USE_OBJECT);
		assertEquals("s5", s5Exit.targetNode());
		assertEquals(List.of(new AfterCommitAction.TeleportPlayer(210070000, 1894.7863f, 2455.1982f, 336.875f, (byte) 109)),
			s5Exit.afterCommit());
	}

	private static boolean hasTalkRoute(QuestDefinition definition, String source, int npcId, QuestDialogAction action) {
		return definition.transitions().stream()
			.anyMatch(transition -> transition.sourceNode().equals(source)
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())));
	}

	private static QuestTransition talkRoute(QuestDefinition definition, String source, int npcId, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst().orElseThrow();
	}

	private static boolean hasCanActRoute(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.anyMatch(transition -> transition.sourceNode().equals(source)
				&& transition.event().equals(new QuestEvent.CanAct(npcId, "ACTION_ITEM_USE")));
	}

	private static void assertPage(QuestDefinition definition, String source,
		QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(definition, source, action);
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static boolean hasRoute(QuestDefinition definition, String source, QuestDialogAction action) {
		return definition.transitions().stream()
			.anyMatch(transition -> transition.sourceNode().equals(source)
				&& transition.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC, action.id())));
	}

	private static QuestTransition route(QuestDefinition definition, String source, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC, action.id())))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/10506.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest10506ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
