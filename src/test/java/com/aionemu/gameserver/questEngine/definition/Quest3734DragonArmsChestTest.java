package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Quest3734DragonArmsChestTest {
	private static final int DRAGON_ARMS_CHEST = 700415;

	@Test
	void dragonArmsChestUsesTheObjectRouteOnlyAfterAcceptance() {
		CompiledQuestDefinition definition = load(3734);
		QuestEvent event = new QuestEvent.CanAct(
			DRAGON_ARMS_CHEST, "ACTION_ITEM_USE");
		QuestTransition gate = route(definition, "started", "started", event);

		QuestNode startedNode = definition.definition().nodes().stream()
			.filter(node -> node.label().equals("started"))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.START, startedNode.projection().status());
		assertEquals(Map.of("var0", 0), startedNode.projection().variables());
		assertEquals(List.of(), gate.conditions());
		assertEquals(List.of(), gate.actions());
		assertEquals(List.of(), gate.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("unaccepted") && transition.event().equals(event)));
	}

	private static QuestTransition route(CompiledQuestDefinition definition,
		String source, String target, QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target)
				&& transition.event().equals(event))
			.findFirst().orElseThrow();
	}

	/**
	 * 生产零售视图：3734 已退役 XML，定义由真端 SimpleCollectItem 表合成（M5-b3 裁定）。
	 * Production retail view: 3734 has no XML anymore and resolves through the retail table.
	 */
	private static CompiledQuestDefinition load(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}
}
