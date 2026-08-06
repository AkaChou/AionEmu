package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 50093 [事件]攻击库穆基巢穴（大师）：nameId 1803560 来自客户端字符串表（STR_QUEST_NAME_Q50093），
 * 真端 quest.xml 50093 块（min 999/EXP 20000000/ITEM 162001058 x3/双 race），
 * data_driven_quest.xml:29855-29868（Talk event_npc_idsolo_s4 → Hunt IDEvent_Solo_Saam_45_N x15），
 * Quest.pak quest_monster.csv（50093,SECTION_1<15,idevent_solo_saam_45_n），
 * 旧 handler _50093.java（835680/835681 接取与报告）。
 */
class Quest50093RetailAlignmentTest {
	@Test
	void preservesRetailMetadataAndFifteenKillHuntChain() throws Exception {
		QuestDefinition definition = load();
		QuestMetadata metadata = definition.metadata();

		assertEquals(50093, definition.id());
		assertEquals(1803560, metadata.displayNameId());
		assertEquals(999, metadata.minLevel());
		assertEquals(999, metadata.maxLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of("ELYOS", "ASMODIANS"), metadata.permittedRaces());
		assertEquals(new RepeatPolicy(255, 0, false, false), metadata.repeatPolicy());
		assertEquals(List.of(new QuestReward("EXP", 0, 20000000),
			new QuestReward("ITEM", 162001058, 3)), metadata.rewards());

		List<QuestTransition> kills = definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpc)
			.toList();
		assertEquals(15, kills.size(), "must hunt 15x IDEvent_Solo_Saam_45_N");
		for (QuestTransition kill : kills) {
			assertEquals(246326, ((QuestEvent.KillNpc) kill.event()).npcId());
		}

		Set<Integer> reportNpcs = new HashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if (transition.targetNode().equals("reward")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(1009).equals(talk.dialogId())
				&& !"reward".equals(transition.sourceNode())) {
				reportNpcs.add(talk.npcId());
			}
		}
		assertEquals(Set.of(835680, 835681), reportNpcs);

		List<List<QuestAction>> completions = definition.transitions().stream()
			.filter(t -> t.targetNode().equals("complete"))
			.map(QuestTransition::actions).toList();
		assertEquals(32, completions.size(), "dual report npc x 16 dialog routes");
		for (List<QuestAction> path : completions) {
			assertTrue(path.contains(new QuestAction.GrantReward("EXP", 0, 20000000,
				QuestRewardAmountMode.QUEST_BASE)));
			assertTrue(path.contains(new QuestAction.GrantReward("ITEM", 162001058, 3)));
			assertTrue(path.contains(new QuestAction.CompleteQuest(0)));
		}
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(
			Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/50093.xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
