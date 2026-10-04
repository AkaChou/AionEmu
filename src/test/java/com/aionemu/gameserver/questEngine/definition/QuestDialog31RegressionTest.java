package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 迁移批保留的对话路由门禁：仍由 XML/IR 车道拥有的任务按 IR 形状断言，已切 native 车道的行按真端表行
 * 与族级页阶梯断言（P3 重锚，计划 §8.9）。
 * <p>
 * SimpleTalk 切换批把 {@code 1131/21033/21455/80038/80039/1963/1964/11106} 移入 native 车道后，
 * 旧 IR 断言（{@code s1→s1} 的 2375 接取窗、{@code started→started} 的 1352 中继页、80038/80039 的
 * complete 态 1011 页）随 typed 定义一并退出生产视图，故逐条改成真端表事实。
 * Dialog-route gate for the migrated batches: rows still owned by the XML/IR lane are asserted on their IR
 * shape, while rows that moved to the native lane are asserted on their retail row and the family page
 * ladder (P3 re-anchor, plan §8.9).
 */
class QuestDialog31RegressionTest {

	@Test
	void migratedQuestHandlersKeepLegacyStartDialogRoutes() throws Exception {
		assertDialog("14010.xml", "started", "started", 203098, 1011);
		assertDialog("14020.xml", "started", "started", 203901, 1011);
		assertDialog("14040.xml", "started", "started", 278501, 10002);
		assertDialog("14050.xml", "started", "started", 204500, 10002);
		assertDialog("14014.xml", "s3", "s3", 802045, 2034,
			new QuestCondition.QuestVariableIs("var0", 3));
		assertDialog("24010.xml", "started", "started", 203557, 1011);
		assertDialog("24020.xml", "started", "started", 204301, 1011);
		assertDialog("24040.xml", "started", "started", 278001, 10002);
		assertDialog("24050.xml", "started", "started", 204702, 10002);
		assertDialog("26823.xml", "s2", "s2", 806289, 1694);
		// 30565 的客户端 HTML 只有 select_none/select_success（item_order 自动接取任务），
		// 无 1011/2375 页——旧客户端形状的两行断言移除（start 批已按契约删除该对话入口）。
		// 30565's client HTML only has select_none/select_success (item_order auto-start);
		// the stale 1011/2375 page assertions were removed when the contract-driven start
		// cleanup dropped that dialog entry.
		assertDialog("1900.xml", "started", "started", 203739, 1352);
		assertDialog("1900.xml", "s1", "s1", 203766, 1693);
		assertDialog("1900.xml", "s2", "s2", 203797, 2034);
		assertDialog("1900.xml", "s3", "s3", 203795, 2375);
	}

	/**
	 * 已切 native 车道的行：真端表行（接取/交付/中继 NPC 与物品通道）+ 族级页阶梯。
	 * Native rows: the retail row (acquire/reward/relay NPCs and item channels) plus the family page ladder.
	 */
	@Test
	void nativeRowsKeepTheirRetailRelayAndAcceptWindows() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		// 真端页阶梯（族级常量）：接取问询 4 → 中继 1352/1693/2034 → 奖励窗 5；领奖收尾 =
		// 回选择对话页 10（真端 npc-complete finish=SELECTION_DIALOG，由族门/领奖门覆盖，非族级常量）。
		// Retail page ladder: accept ask 4 -> relay 1352/1693/2034 -> reward window 5; the claim tail
		// returns to the selection dialog (10) and is covered by the family/claim gates.
		assertEquals(4, SimpleTalkHandler.PAGE_ASK_ACCEPT);
		assertEquals(1352, SimpleTalkHandler.pageForStep(1));
		assertEquals(1693, SimpleTalkHandler.pageForStep(2));
		assertEquals(2034, SimpleTalkHandler.pageForStep(3));
		assertEquals(5, SimpleTalkHandler.PAGE_REWARD_WINDOW);

		// 1963/1964：接取与交付同为 Polyidus(203726)，中继 Phokas(203851)/Noris(203776)。
		assertNativeChain(handler, 1963, 203726, 203726, 203851);
		assertNativeChain(handler, 1964, 203726, 203726, 203776);
		// 1131：接取 Hyacinte(203097) / 交付 Nadaelo(203101)，中继 Shugo_LF1a_01(203101 交付侧同表行)；
		// 接取发 182200506、第 1 步换 182200507（真端 give_item/remove_item 列）。
		// 1131: accept Hyacinte(203097) / reward Nadaelo(203101); the accept grants 182200506 and step 1
		// swaps it for 182200507.
		assertNativeChain(handler, 1131, 203097, 203101, 0);
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.acceptGiveItem(1131));
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.stepRemoveItem(1131, 1));
		assertEquals(new SimpleTalkHandler.ItemStack(182200507, 1), handler.stepGiveItem(1131, 1));
		// 21033：接取与交付同 Carabel(799256)，中继 Horu，接取发 182207829、第 1 步换 182207830。
		// 21033: one NPC for accept and reward (Carabel 799256); step 1 swaps 182207829 for 182207830.
		assertNativeChain(handler, 21033, 799256, 799256, 0);
		assertEquals(new SimpleTalkHandler.ItemStack(182207829, 1), handler.acceptGiveItem(21033));
		assertEquals(new SimpleTalkHandler.ItemStack(182207830, 1), handler.stepGiveItem(21033, 1));
		// 21455：接取 Tree_NoMove_Miener(799404) / 交付 Unset(799244)，中继 Schiemann(799240)。
		assertNativeChain(handler, 21455, 799404, 799244, 0);
		// 11106：接取 Geta(798976) / 交付 Dimos(203832)，两个中继步（旧 XML 的 798978/798979 与真端行
		// 不同，按用户口径以真端表为准）。
		// 11106: accept Geta(798976) / reward Dimos(203832) with two relay steps; the retail table wins
		// over the legacy XML's 798978/798979.
		assertEquals(798976, handler.acquireNpc(11106));
		assertEquals(203832, handler.rewardNpc(11106));
		assertEquals(2, handler.relayCount(11106));
		assertEquals(new SimpleTalkHandler.ItemStack(182206780, 1), handler.acceptGiveItem(11106));
		assertEquals(new SimpleTalkHandler.ItemStack(182206781, 1), handler.stepGiveItem(11106, 1));
		// 80038/80039：接取与交付同 event_Druike(799780)，单步 + 整组交付门；旧 IR 的 complete 态 1011 页
		// 断言随 native 交付门退场（真端把该条件表达为 collect_item 整组门）。
		// 80038/80039: accept and reward at event_Druike(799780), single step with the whole collect group
		// as the hand-in gate.
		assertEquals(799780, handler.acquireNpc(80038));
		assertEquals(799780, handler.rewardNpc(80038));
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(164002017, 5)), handler.workItems(80038));
		assertEquals(799780, handler.acquireNpc(80039));
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(164002018, 1)), handler.workItems(80039));
	}

	/**
	 * 断言一行 native 任务的接取/交付 NPC 与第 1 中继步归属；{@code relayNpc} 为 0 时只断言中继数。
	 * Asserts the acquire/reward NPC and the first relay step; a zero {@code relayNpc} skips the NPC check.
	 */
	private static void assertNativeChain(SimpleTalkHandler handler, int questId, int acquireNpc, int rewardNpc,
			int relayNpc) {
		assertEquals(acquireNpc, handler.acquireNpc(questId), "quest " + questId + " 接取 NPC");
		assertEquals(rewardNpc, handler.rewardNpc(questId), "quest " + questId + " 交付 NPC");
		assertEquals(1, handler.relayCount(questId), "quest " + questId + " 中继步数");
		if (relayNpc != 0) {
			assertTrue(handler.relaysForNpc(relayNpc).stream()
				.anyMatch(step -> step.questId() == questId && step.step() == 1),
				"quest " + questId + " 第 1 中继步必须挂在该 NPC 上");
		}
	}

	@Test
	void migratedQuestHandlersKeepLegacyTurnInDialogRoutes() throws Exception {
		assertDialog("10525.xml", "s4", "s4", 806134, 2375,
			new QuestCondition.QuestVariableIs("var0", 4));
		assertDialog("20525.xml", "s4", "s4", 806135, 2375,
			new QuestCondition.QuestVariableIs("var0", 4));
		assertDialogAction("10525.xml", "s5", "s5", 806134, 2716, 2716);
		assertDialogAction("20525.xml", "s5", "s5", 806135, 2716, 2716);
	}

	private static void assertDialog(String file, String source, String target, int npcId, int page,
			QuestCondition... conditions) throws Exception {
		assertDialogAction(file, source, target, npcId, 31, page, conditions);
	}

	private static void assertDialogAction(String file, String source, String target, int npcId, int action,
			int page, QuestCondition... conditions) throws Exception {
		CompiledQuestDefinition compiled = definition(file);
		assertTrue(compiled.definition().transitions().stream().anyMatch(transition ->
			source.equals(transition.sourceNode())
				&& transition.targetNode().equals(target)
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action))
				&& transition.conditions().containsAll(List.of(conditions))
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(page))),
			"missing dialog route: " + file + " " + source + " npc=" + npcId + " action=" + action
				+ " page=" + page);
	}

	private static CompiledQuestDefinition definition(String file) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(Integer.parseInt(file.replace(".xml", "")));
	}
}
