package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真端 EnterWorld PVP 阵营守护任务契约测试（15596 阿斯特拉 / 25596 诺斯佩拉）。
 * <p>
 * 验证：
 * 1. category_acquire=EnterWorld 的地图 ID（210100000 / 220110000）不被解析为 NPC，不生成 TalkToNpc 接取流；
 * 2. 接取边为 QuestEvent.EnterWorld + StartEligible + WorldIs；
 * 3. 领奖 NPC 为对应的司令官真实模板（806114 LF6_Ilisia_E / 806116 DF6_Reinhard_E）。
 */
class QuestEnterWorldPvpRetailContractTest {

	private static RetailQuestDriver driver;

	@BeforeAll
	static void setUp() {
		System.setProperty("aion.quest.retailDriver", "true");
		RetailQuestDriver.overlay(ImmutableQuestCatalog.fromEntries(List.of()));
		driver = RetailQuestDriver.current().orElseThrow();
	}

	@Test
	void quest15596EsterraEnterWorldAcquireAndRealNpc() {
		int questId = 15596;
		int esterraWorldId = 210100000;
		int rewardNpcId = 806114;

		assertTrue(driver.definition(questId).isPresent(), "15596 应可由 RetailQuestDriver 驱动");
		QuestDefinition definition = driver.definition(questId).get().definition();

		Set<Integer> talkNpcIds = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(e -> ((QuestEvent.TalkToNpc) e).npcId())
			.collect(Collectors.toSet());

		assertFalse(talkNpcIds.contains(esterraWorldId), "15596 严禁将地图 ID 210100000 作为 TalkToNpc 注册");
		assertEquals(Set.of(rewardNpcId), talkNpcIds, "15596 的对话 NPC 集合应仅包含领奖司令官 806114");

		List<QuestTransition> enterWorldTransitions = definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.EnterWorld)
			.toList();
		assertEquals(1, enterWorldTransitions.size(), "15596 应恰好有 1 条 EnterWorld 接取边");

		QuestTransition enterWorld = enterWorldTransitions.get(0);
		assertEquals("unaccepted", enterWorld.sourceNode());
		assertEquals("a0", enterWorld.targetNode());
		assertTrue(enterWorld.conditions().stream().anyMatch(QuestCondition.StartEligible.class::isInstance),
			"15596 EnterWorld 必须包含 StartEligible 条件");
		assertTrue(enterWorld.conditions().stream().anyMatch(c -> c instanceof QuestCondition.WorldIs w && w.worldId() == esterraWorldId && w.expected()),
			"15596 EnterWorld 必须包含 WorldIs(210100000) 条件");
	}

	@Test
	void quest25596NosraEnterWorldAcquireAndRealNpc() {
		int questId = 25596;
		int nosraWorldId = 220110000;
		int rewardNpcId = 806116;

		assertTrue(driver.definition(questId).isPresent(), "25596 应可由 RetailQuestDriver 驱动");
		QuestDefinition definition = driver.definition(questId).get().definition();

		Set<Integer> talkNpcIds = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(e -> ((QuestEvent.TalkToNpc) e).npcId())
			.collect(Collectors.toSet());

		assertFalse(talkNpcIds.contains(nosraWorldId), "25596 严禁将地图 ID 220110000 作为 TalkToNpc 注册");
		assertEquals(Set.of(rewardNpcId), talkNpcIds, "25596 的对话 NPC 集合应仅包含领奖司令官 806116");

		List<QuestTransition> enterWorldTransitions = definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.EnterWorld)
			.toList();
		assertEquals(1, enterWorldTransitions.size(), "25596 应恰好有 1 条 EnterWorld 接取边");

		QuestTransition enterWorld = enterWorldTransitions.get(0);
		assertEquals("unaccepted", enterWorld.sourceNode());
		assertEquals("a0", enterWorld.targetNode());
		assertTrue(enterWorld.conditions().stream().anyMatch(QuestCondition.StartEligible.class::isInstance),
			"25596 EnterWorld 必须包含 StartEligible 条件");
		assertTrue(enterWorld.conditions().stream().anyMatch(c -> c instanceof QuestCondition.WorldIs w && w.worldId() == nosraWorldId && w.expected()),
			"25596 EnterWorld 必须包含 WorldIs(220110000) 条件");
	}
}
