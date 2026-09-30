package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 enter-zone 迁移任务必须保留 legacy 接取 owner 的合同；QE-109 起 15322/25322 的 owner 换成
 * 真端受理形（同一 NPC 的客户端受理手势）。
 * Locks migrated enter-zone quests to their start-owner contract; since QE-109 the 15322/25322 owner is
 * the retail accept shape (the client's accept gesture at the same npc).
 */
class QuestEnterZoneStartOwnerRegressionTest {
	private static final Map<Integer, QuestEvent> START_ROUTES = Map.ofEntries(
		Map.entry(1393, new QuestEvent.TalkToNpc(204041, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(14123, new QuestEvent.TalkToNpc(203933, QuestDialogAction.QUEST_ACCEPT_1.id())),
		// 15322/25322 自 QE-109 起由真端多胞感官区 owner 驱动：接取仍是同一 NPC，但走客户端受理手势
		// （QUEST_ACCEPT_SIMPLE=20000），遗留的「走到附近即接取」（AtDistance）随壳退役。
		// Since QE-109 quests 15322/25322 are retail-owned (multi-cell sensory areas): the accept keeps the
		// same npc but uses the client's accept gesture (QUEST_ACCEPT_SIMPLE=20000); the legacy
		// walk-nearby accept (AtDistance) is retired with the shell.
		Map.entry(15322, new QuestEvent.TalkToNpc(805330, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())),
		Map.entry(16800, new QuestEvent.TalkToNpc(806075, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())),
		Map.entry(17500, new QuestEvent.TalkToNpc(806262, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())),
		Map.entry(18300, new QuestEvent.TalkToNpc(804699, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(21080, new QuestEvent.TalkToNpc(799231, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(25322, new QuestEvent.TalkToNpc(805342, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())),
		Map.entry(27500, new QuestEvent.TalkToNpc(806264, QuestDialogAction.QUEST_ACCEPT_1.id())),
		Map.entry(28300, new QuestEvent.TalkToNpc(801904, QuestDialogAction.QUEST_ACCEPT_1.id())));

	@Test
	void affectedQuestsDoNotAutoStartOnEnterZone() throws Exception {
		for (int questId : START_ROUTES.keySet()) {
			QuestDefinition definition = load(questId).definition();
			List<QuestTransition> autoStarts = definition.transitions().stream()
				.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
				.filter(candidate -> candidate.event() instanceof QuestEvent.EnterZone)
				.filter(candidate -> targetStatus(definition, candidate.targetNode()) == QuestStatus.START)
				.toList();
			assertTrue(autoStarts.isEmpty(),
				() -> "quest " + questId + " still auto-starts on enter-zone: " + autoStarts);
		}
	}

	@Test
	void affectedQuestsExposeTheLegacyStartOwnerRoute() throws Exception {
		for (Map.Entry<Integer, QuestEvent> entry : START_ROUTES.entrySet()) {
			QuestDefinition definition = load(entry.getKey()).definition();
			QuestTransition start = definition.transitions().stream()
				.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
				.filter(candidate -> "started".equals(candidate.targetNode()))
				.filter(candidate -> entry.getValue().equals(candidate.event()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("quest " + entry.getKey()
					+ " is missing its legacy start route " + entry.getValue()));
			if (entry.getKey() == 21080) {
				assertTrue(start.actions().contains(new QuestAction.GiveItem(182207939, 1)),
					"quest 21080 start must grant the windstream letter");
			}
		}
	}

	private static QuestStatus targetStatus(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst()
			.orElseThrow()
			.projection()
			.status();
	}

	/**
	 * 生产视图：XML 目录 + 真端 overlay（已退役任务的 XML 只在 git 历史里，直读文件会在退役后失效）。
	 * Production view: the XML directory plus the retail overlay, so a later retirement cannot break this lock.
	 */
	private static CompiledQuestDefinition load(int questId) throws Exception {
		return ProductionQuestDefinitions.definition(questId);
	}
}
