package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest30311RetailAlignmentTest {
	@Test
	void everyTurnInRequiresAndConsumesTheCollectedItem() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest/definitions/quests/30311.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			List<QuestTransition> turnIns = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(1009).equals(talk.dialogId()))
				.toList();

			// 物件 owner 收口（2026-10-08，QE-052 同型批次）：符文宝珠 730275 的对话由 RiftOrbAI2 承担
			// （原版注册面挂 riftorb AI，legacy 注册与对话块整体注释禁用），交付对象只剩任务书末行的
			// Herka 799322；形状与姊妹任务 30313 一致。
			// Object-owner trim (2026-10-08, QE-052 machine-variant batch): the orb 730275 is owned by
			// RiftOrbAI2 (retail AI registration; the legacy registration and dialog block are commented
			// out), so the only item turn-in is Herka 799322, the journal's reward row, as in sibling 30313.
			assertEquals(Set.of(799322), turnIns.stream()
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
				.collect(Collectors.toSet()));
			for (QuestTransition turnIn : turnIns) {
				assertEquals(List.of(new QuestCondition.HasItem(182209714, 1)), turnIn.conditions());
				assertEquals(List.of(new QuestAction.RemoveItem(182209714, 1)), turnIn.actions());
			}
		}
	}
}
