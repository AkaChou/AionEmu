package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 51019 [活动/每日]巧克力塔的装饰（魔族版）：与已迁移同族 50019 同构（Asmodian 镜像）。
 * nameId 1150066 来自客户端字符串表（STR_QUEST_NAME_Q51019），真端 quest.xml 51019 块
 * （min 9/collect quest_51011a x3=182215178/work quest_51013a=182215180/前置 Q51010:1/奖励三件套），
 * Quest.pak quest_monster.csv 掉落 valentineevent_brownie_solo，legacy event.xml start_npc_ids=202549。
 */
class Quest51019RetailAlignmentTest {
	@Test
	void preservesRetailMetadataItemsPrerequisiteAndNpcRoute() throws Exception {
		QuestDefinition definition = load();
		QuestMetadata metadata = definition.metadata();

		assertEquals(51019, definition.id());
		assertEquals(1150066, metadata.displayNameId());
		assertEquals(9, metadata.minLevel());
		assertEquals(Integer.MAX_VALUE, metadata.maxLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertEquals(new RepeatPolicy(255, 0, true, false), metadata.repeatPolicy());
		assertEquals(Set.of("ALL"), metadata.repeatCycles());
		assertEquals(Set.of(51010), metadata.prerequisites());
		assertEquals(List.of(new QuestItemRequirement(182215178, 3)), metadata.itemRequirements());
		assertEquals(List.of(new QuestItemRequirement(182215180, 1)), metadata.questWorkItems());
		assertEquals(List.of(new QuestReward("ITEM", 188051769, 1),
			new QuestReward("ITEM", 160010208, 1),
			new QuestReward("ITEM", 160010209, 1)), metadata.rewards());

		assertTrue(hasStartRoute(definition, 1002));
		assertTrue(hasStartRoute(definition, 20000));
		assertTrue(hasStartRoute(definition, 10000));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"started".equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 202549
				&& Integer.valueOf(39).equals(talk.dialogId())
				&& transition.conditions().contains(new QuestCondition.HasItem(182215178, 3))
				&& transition.actions().contains(new QuestAction.RemoveItem(182215178, 3))));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 202549
				&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 188051769, 1))
				&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 160010208, 1))
				&& transition.actions().contains(new QuestAction.GrantReward("ITEM", 160010209, 1))
				&& transition.actions().contains(new QuestAction.CompleteQuest(0))));
	}

	private static boolean hasStartRoute(QuestDefinition definition, int dialogId) {
		return definition.transitions().stream().anyMatch(transition ->
			"unaccepted".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(202549, dialogId))
				&& transition.conditions().contains(new QuestCondition.StartEligible()));
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(
			Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/51019.xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
