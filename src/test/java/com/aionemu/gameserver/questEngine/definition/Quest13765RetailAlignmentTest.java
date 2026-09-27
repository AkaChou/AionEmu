package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退役行 13765（周常 Levinshor 狩猎）的真端对齐：该行已由真端模板表 + quest.xml 元数据合成，
 * 生产 XML 已删除（版本可回溯），因此断言锚在生产视图（真端优先 overlay）而不是 XML 形态上。
 * 元数据面与客户端击杀门控面对齐：显示名 id、等级、类别、种族、重复上限、奖励与
 * "击杀 235357 共 5 次"（真端 IR 的击杀台阶即客户端 SECTION_1&lt;5 的落点）。
 * <p>
 * Retail-anchored alignment for the retired row 13765 (the weekly Levinshor hunt): the row is now
 * synthesized from the retail template tables plus quest.xml metadata and its production XML is gone,
 * so the assertions anchor on the production view (the retail-first overlay) instead of XML shapes.
 * The metadata face and the client kill gate stay aligned: display-name id, level, category, races,
 * repeat ceiling, rewards and "235357 killed five times" (the retail IR's ladder is the client
 * SECTION_1&lt;5 landing).
 */
class Quest13765RetailAlignmentTest {
	private static final String PREVIOUS_SWITCH = System.getProperty("aion.quest.retailDriver");
	private static final Set<Integer> REPORT_NPCS = Set.of(805272, 805273, 805274);

	@BeforeAll
	static void enableRetailFirstProduction() {
		System.setProperty("aion.quest.retailDriver", "true");
	}

	@AfterAll
	static void restoreSwitch() {
		if (PREVIOUS_SWITCH == null) {
			System.clearProperty("aion.quest.retailDriver");
		} else {
			System.setProperty("aion.quest.retailDriver", PREVIOUS_SWITCH);
		}
	}

	@Test
	void preservesWeeklyMetadataKillTargetAndThreeGuardNpcs() {
		CompiledQuestDefinition compiled = ProductionQuestDefinitions.catalog()
			.findExecutable(13765).orElseThrow();
		QuestMetadata metadata = compiled.definition().metadata();
		// 真端元数据的 name 是开发名（dev_name），玩家可见名走 name_id → 客户端词典。
		// The retail metadata's name is the dev name; the player-visible name travels name_id → client.
		assertEquals("Q13765", metadata.name());
		assertEquals(1801283, metadata.displayNameId());
		assertEquals(65, metadata.minLevel());
		assertEquals("SEEN_MARKER", metadata.category());
		assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
		assertEquals(255, metadata.repeatPolicy().maxRepeatCount());
		assertEquals(List.of(new QuestReward("EXP", 0, 3618881), new QuestReward("ITEM", 186000236, 5)),
			metadata.rewards());
		// 客户端 quest_monster.csv SECTION_1<5、data_driven value0_progress_ 与 13841 族同侧的
		// "客户端计数为权威"裁定一致：真端 IR 的击杀台阶共 5 级，只打 235357。
		// The client SECTION_1<5 gate and the client-count adjudication (the same canon as the 13841
		// family) land on a five-rung retail ladder that only hunts 235357.
		assertEquals(5, QuestKillCounterSimulator.requiredKills(compiled), "13765 requires five kills");
		assertEquals(Set.of("var0"), QuestKillCounterSimulator.killCounterFields(compiled),
			"13765's retail ladder counts on one field");
		Set<Integer> hunted = QuestKillCounterSimulator.killEvents(compiled).stream()
			.filter(event -> event instanceof QuestEvent.KillNpc)
			.map(event -> ((QuestEvent.KillNpc) event).npcId())
			.collect(Collectors.toSet());
		assertEquals(Set.of(235357), hunted, "13765 hunts one npc family");
		// 报告页（dialog 1009）只落在三位守备队 NPC 上；声明式 <kills> 是 XML 侧展示通道，
		// 真端元数据不带（该轴的分歧在 QuestKillCounterRetailGateTest 的人口护栏里显式对拍）。
		// The report page (dialog 1009) lands on the three guard npcs only; the display-only <kills>
		// declarations live on the XML side, and the retail metadata carries none (that axis is checked
		// explicitly by the population guard in QuestKillCounterRetailGateTest).
		assertTrue(metadata.kills().isEmpty(), "retail metadata declares no display-only kill steps");
		Set<Integer> reportNpcs = compiled.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 1009)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(Collectors.toSet());
		assertEquals(REPORT_NPCS, reportNpcs);
	}
}
