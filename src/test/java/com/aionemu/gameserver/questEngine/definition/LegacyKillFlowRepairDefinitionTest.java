package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyKillFlowRepairDefinitionTest {
	@Test
	void killMilestonesRemainStartedUntilTheLegacyHandoff() {
		for (int questId : List.of(2620, 4210, 18302, 18303, 18510, 23702, 23703, 23705)) {
			if (RetiredQuestIds.contains(questId)) {
				// 23702/23703/23705 自 P0c-6/P0c-8c 起走原版 SimpleHunt 网格合成器、已随族采纳退役
				// （XML 删除、生产视图无 IR）：IR 测试退役三式（QE-146）下改锚 native 注册面——「报告行 =
				// 计数满段节点」「路由不带第二计数器门控」等网格形状语义归 SimpleHunt 族门承担。
				// 23702/23703/23705 are grid-composed since P0c-6/P0c-8c and retired with the SimpleHunt
				// adoption (XML deleted, no production IR): under the QE-146 triage they are re-anchored to
				// the native registration surface; the grid-report shape lives in the SimpleHunt gate.
				assertRetiredOwnedBySimpleHuntLane(questId);
				continue;
			}
			CompiledQuestDefinition definition = load(questId);
			assertFalse(definition.definition().transitions().stream()
				.anyMatch(transition -> isKill(transition.event()) && transition.targetNode().equals("reward")),
				() -> questId + " must not become reward-ready directly from its ordinary kill milestone");
		}

		QuestTransition phagrasulReport = talk(load(2620), "s1", 204787, 1009, "reward");
		assertTrue(phagrasulReport.conditions().contains(new QuestCondition.VariableAtLeast("var1", 5)));
		assertTrue(phagrasulReport.conditions().contains(new QuestCondition.VariableAtLeast("var2", 5)));

		assertEquals("reward", talk(load(18302), "started", 730375, 10255, "reward").targetNode());
		assertEquals("reward", talk(load(18303), "started", 700980, -1, "reward").targetNode());
		/* 原 23702/23703/23705 网格报告行断言已随退役改锚 native（见上方循环与 helper，QE-146）：
		 * 报告行 a4 等形状由 SimpleHunt 网格族门锁定，本类不再对退役 id 取 IR。
		 * The former 23702/23703/23705 grid-report assertions were re-anchored to the native surface
		 * (see the loop above and the helper, QE-146): the grid-report shapes are locked by the
		 * SimpleHunt family gate; this class no longer loads IR for retired ids. */
	}

	@Test
	void independentLegacyKillBitsRemainIndependent() {
		CompiledQuestDefinition missingHaorunerk = load(4210);
		assertEquals(Set.of("var1", "var2"), fieldNames(missingHaorunerk, "var1", "var2"));
		assertSetsBit(kill(missingHaorunerk, "s1", 215056), "var1");
		assertSetsBit(kill(missingHaorunerk, "s1", 215080), "var2");

		CompiledQuestDefinition fate = load(4502);
		assertEquals(Set.of("var1", "var2", "var3"), fieldNames(fate, "var1", "var2", "var3"));
		assertSetsBit(kill(fate, "s2", 214895), "var1");
		assertSetsBit(kill(fate, "s2", 214896), "var2");
		assertSetsBit(kill(fate, "s2", 214897), "var3");
		QuestTransition itemReport = talk(fate, "s2", 204837, 39, "reward");
		assertTrue(itemReport.conditions().contains(new QuestCondition.HasItem(182204534, 1)));
		assertTrue(itemReport.actions().contains(new QuestAction.RemoveItem(182204534, 1)));

		CompiledQuestDefinition destroyingWeapons = load(2633);
		assertEquals("s2", talk(destroyingWeapons, "s1", 700296, -1, "s2").targetNode());
		assertEquals("reward", kill(destroyingWeapons, "s2", 213933).targetNode());
	}

	@Test
	void missionKillChainsPreserveTheirDialogZoneAndItemStages() {
		CompiledQuestDefinition totem = load(24015);
		assertEquals("s2", totem.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "s1")
				&& transition.event().equals(new QuestEvent.EnterZone("BLACK_CLAW_OUTPOST_220030000")))
			.findFirst().orElseThrow().targetNode());
		assertEquals(List.of("s3", "s4", "reward"), totem.definition().transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.KillNpc(700099)))
			.map(QuestTransition::targetNode).toList());
		assertTrue(totem.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "reward")
				&& transition.targetNode().equals("complete"))
			.allMatch(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId() == 203557));

		CompiledQuestDefinition frozenCity = load(24052);
		List<QuestTransition> itemUses = frozenCity.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.UseItem).toList();
		assertEquals(9, itemUses.size());
		assertTrue(itemUses.stream().allMatch(transition ->
			transition.conditions().contains(new QuestCondition.ZoneIs("DF3_ITEMUSEAREA_Q2056"))
				&& transition.actions().contains(new QuestAction.BlockDefaultItemUse())));
		assertEquals(3, itemUses.stream().filter(transition -> transition.targetNode().equals("s4")
			&& transition.afterCommit().stream().anyMatch(AfterCommitAction.SpawnNpc.class::isInstance)
			&& transition.afterCommit().stream().anyMatch(AfterCommitAction.StartQuestTimer.class::isInstance)).count());
		assertEquals("reward", kill(frozenCity, "s4", 233864).targetNode());

		CompiledQuestDefinition crisis = load(24054);
		assertEquals("s5", kill(crisis, "s2", 702041).targetNode());
		assertEquals("s6", kill(crisis, "s5", 233865).targetNode());
		assertEquals("reward", talk(crisis, "s6", 204701, 10255, "reward").targetNode());

		CompiledQuestDefinition umkata = load(24114);
		QuestTransition spirit = umkata.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)
				&& npcIds.equals(Set.of(210722, 210588))).findFirst().orElseThrow();
		assertEquals("started", spirit.targetNode());
		QuestTransition boss = kill(umkata, "started", 210752);
		assertEquals("reward", boss.targetNode());
		assertEquals(Set.of(182215474, 182215475, 182215476), boss.actions().stream()
			.filter(QuestAction.RemoveItem.class::isInstance)
			.map(QuestAction.RemoveItem.class::cast).map(QuestAction.RemoveItem::itemId)
			.collect(Collectors.toSet()));
	}

	/* 15304/15306（达伊瓦尼恩混合链/五段 hunt）的 IR 形断言已随两任务退役删除（IR 测试退役三式 QE-146，
	 * 式①整主语退役 ⇒ 删法）：15304 自 DD_TALK_COLLECT_HUNT_CHAIN、15306 自五段 hunt 采纳后 XML 退役、
	 * 生产视图无 IR，链形/段计数行为由 DataDriven 族门承担（QuestDaevanionLeggingsProductionFlowTest
	 * 与 DD 族门），本类不再保留任何 15304/15306 的 snapshot/apply 机器。
	 * The IR-shape assertions of 15304/15306 (the Daevanion mixed chain / five-stage hunt) were deleted with
	 * both retirements (QE-146 triage form 1: delete when the whole subject is retired): after the
	 * DD_TALK_COLLECT_HUNT_CHAIN and five-stage-hunt adoptions their XMLs are gone and the production view
	 * has no IR, so the chain/stage behavior lives in the DataDriven family gates
	 * (QuestDaevanionLeggingsProductionFlowTest and the DD gates); this class keeps no 15304/15306
	 * snapshot/apply machinery. */

	/**
	 * IR 测试退役三式（QE-146，2026-10-05 清扫批口径）：退役行（retention owner=RETAIL_TABLE）生产视图无
	 * IR，凡 find/compile 其定义的路由断言必红——本类对退役主语改锚 native 注册面：先守卫「确属退役且
	 * 无定义」，再要求 SimpleHunt 族 handler owns。行为语义归 SimpleHunt 族门承担；方法名保留。
	 * QE-146 IR-test retirement triage: a retired row (RETAIL_TABLE owner) has no production IR, so its
	 * route assertions are re-anchored to the native registration surface: guard that the row is really
	 * retired with no definition, then require the SimpleHunt family handler. Behavior semantics stay with
	 * the SimpleHunt gate; method names are kept.
	 */
	private static void assertRetiredOwnedBySimpleHuntLane(int questId) {
		assertTrue(RetiredQuestIds.contains(questId),
			() -> "quest " + questId + " must really be retired before its IR assertion is re-anchored");
		assertTrue(ProductionQuestDefinitions.catalog().find(questId).isEmpty(),
			() -> "quest " + questId + " is retired but the production view still exposes a definition");
		assertTrue(SimpleHuntHandler.instance().owns(questId),
			() -> "quest " + questId + " must be owned by the SimpleHunt native lane");
	}

	private static Set<String> fieldNames(CompiledQuestDefinition definition, String... names) {
		return java.util.Arrays.stream(names)
			.filter(name -> definition.definition().progressLayout().field(name) != null)
			.collect(Collectors.toSet());
	}

	private static void assertSetsBit(QuestTransition transition, String field) {
		assertTrue(transition.actions().contains(new QuestAction.SetVariable(field, 1)));
	}

	private static boolean isKill(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc || event instanceof QuestEvent.KillNpcSet;
	}

	private static QuestTransition kill(CompiledQuestDefinition definition, String source, int npcId) {
		return transition(definition, source, transition -> QuestEvent.matches(transition.event(), new QuestEvent.KillNpc(npcId)));
	}

	private static QuestTransition talk(CompiledQuestDefinition definition, String source, int npcId,
			int dialogId, String target) {
		return transition(definition, source, transition -> transition.targetNode().equals(target)
			&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)));
	}

	private static QuestTransition transition(CompiledQuestDefinition definition, String source,
			Predicate<QuestTransition> predicate) {
		return definition.definition().transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), source) && predicate.test(transition))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId)  {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
