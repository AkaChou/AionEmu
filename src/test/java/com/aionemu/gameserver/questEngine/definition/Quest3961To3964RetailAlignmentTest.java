package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 3961-3964（副角色育成，天族四档）的原版灵符链与整组交付门。
 * Locks quests 3961-3964's retail charm chain and whole-set hand-in gate.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed 节点/转换金标随迁移退场，按计划 §8.9（P3 重锚口径）
 * 改锚原版表行（接取 Flora → erdos 步 1 交灵符 → Flora 交付、item_check 整组门）、quest.xml 检查物与奖励。
 * 门语义：检查按钮（39）未持满整组 → 客户端声明的失败页 2716；持满 → 扣整组 + REWARD + 奖励窗（页 5）。
 * <p>
 * The retired typed gold standard is re-anchored (plan §8.9) to the retail rows (accept Flora → erdos
 * step 1 → Flora hand-in, whole-set item_check gate), the quest.xml check items and rewards. Gate: the
 * check button (39) without the whole set shows the declared fail page 2716; holding it removes the whole
 * set and flips REWARD with the reward window (page 5).
 */
class Quest3961To3964RetailAlignmentTest {
	private static final int FLORA = 798384;
	private static final int ERDOS = 203740;
	/** 原版金币符号 gold = 182400001。 / The retail gold (kinah) item. */
	private static final int GOLD = 182400001;
	private static final List<Spec> SPECS = List.of(
		new Spec(3961, 35, 182206108, 169621001, List.of(new WorkItem(GOLD, 40000))),
		new Spec(3962, 40, 182206109, 169621002,
			List.of(new WorkItem(186000088, 1), new WorkItem(GOLD, 50000))),
		new Spec(3963, 45, 182206110, 169621003,
			List.of(new WorkItem(186000089, 1), new WorkItem(GOLD, 70000))),
		new Spec(3964, 50, 182206111, 169621004,
			List.of(new WorkItem(186000090, 1), new WorkItem(GOLD, 90000)))
	);

	@Test
	void retailRowsKeepTheCharmChainAndTheWholeGateSet() throws Exception {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		for (Spec spec : SPECS) {
			assertTrue(RetiredQuestIds.contains(spec.questId()), spec.questId() + " 必须在保留清单内");
			assertTrue(handler.routes(spec.questId()), spec.questId() + " 必须由 native 车道执行");
			assertFalse(ProductionQuestDefinitions.catalog().findExecutable(spec.questId()).isPresent(),
				spec.questId() + " 退役后 typed 目录不得再持有");

			assertEquals("Flora", handler.requireRow(spec.questId()).acquiredNpcName(), "接取 owner 名");
			assertEquals(List.of("erdos"), handler.requireRow(spec.questId()).talkNpcNames(), "中继链");
			assertEquals("Flora", handler.requireRow(spec.questId()).rewardNpcName(), "交付 owner 名");
			assertEquals(FLORA, NativeNpcNameResolver.instance().resolve("Flora").npcIds().get(0));
			assertEquals(ERDOS, NativeNpcNameResolver.instance().resolve("erdos").npcIds().get(0));
			assertEquals(FLORA, handler.acquireNpc(spec.questId()), "接取 owner = Flora");
			assertEquals(FLORA, handler.rewardNpc(spec.questId()), "交付 owner = Flora");
			assertEquals(1, handler.relayCount(spec.questId()), "单步中继行");
			assertTrue(handler.relaysForNpc(ERDOS).contains(
				new SimpleTalkHandler.RelayStep(spec.questId(), 1, ERDOS)), "步 1 = erdos");
			assertEquals(new SimpleTalkHandler.ItemStack(spec.acceptItem(), 1),
				handler.acceptGiveItem(spec.questId()), "接取发放灵符");
			assertEquals(new SimpleTalkHandler.ItemStack(spec.acceptItem(), 1),
				handler.stepRemoveItem(spec.questId(), 1), "步 1 扣除灵符");
			assertTrue(handler.requireRow(spec.questId()).itemCheck(), "原版行声明 item_check 整组门");
			assertEquals(spec.workItems().stream()
				.map(item -> new SimpleTalkHandler.ItemStack(item.itemId(), item.count())).toList(),
				handler.workItems(spec.questId()), "整组检查物（顺序 = check_item1_K）");

			QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
				.retailMetadataOf(spec.questId()).orElseThrow().metadata();
			assertEquals(spec.minLevel(), metadata.minLevel(), "原版 minlevel_permitted");
			assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
			List<QuestReward> rewards = metadata.rewards();
			assertTrue(rewards.contains(new QuestReward("ITEM", spec.xpBoostItem(), 5)),
				() -> spec.questId() + " 奖励 " + rewards);
		}
	}

	@Test
	void itemGateForcesTheWholeSetAndTheCharmChainFaces() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler handler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 50);

		for (Spec spec : SPECS) {
			inventory.clear();
			NativeTalkFixture.clearPackets(player);

			// 未接取：任务行打开客户端声明的入口页（信页 select1=1011，带任务上下文）。
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, FLORA, spec.questId(), 31)));
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
				NativeTalkFixture.clientEntryPage(spec.questId()), spec.questId());

			// 中继（erdos）：任务行打开该步页（select2=1352）；推进 = var0=1 + 扣灵符 + 关窗。
			NativeTalkFixture.add(player, spec.questId(), QuestStatus.START, 0);
			inventory.hold(spec.acceptItem(), 1);
			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, ERDOS, spec.questId(), 31)));
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, spec.questId());

			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, ERDOS, spec.questId(), 10000)));
			assertEquals(1, player.getQuestStateList().getQuestState(spec.questId())
				.getQuestVars().getQuestVars(), "步 1 推进必须写 var0=1");
			NativeTalkFixture.assertCloseDialog(player);

			// 交付面：中继满后任务行发报告确认页（select5=2375）。
			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, FLORA, spec.questId(), 31)));
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, spec.questId());

			// 门未过：检查按钮（39）在未持满整组时只发客户端声明的失败页 2716，状态保持 START。
			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, FLORA, spec.questId(), 39)));
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2716, spec.questId());
			assertEquals(QuestStatus.START,
				player.getQuestStateList().getQuestState(spec.questId()).getStatus(),
				"门未过不得推进状态");

			// 门通过：持满整组后检查按钮扣整组 + 推进 REWARD + 奖励窗（页 5）。
			List<String> expectedRemovals = new ArrayList<>();
			for (WorkItem item : spec.workItems()) {
				inventory.hold(item.itemId(), item.count());
				expectedRemovals.add("remove:" + item.itemId() + ":" + item.count());
			}
			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, FLORA, spec.questId(), 39)));
			assertEquals(QuestStatus.REWARD,
				player.getQuestStateList().getQuestState(spec.questId()).getStatus(),
				"门通过必须推进到 REWARD");
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, spec.questId());
			assertTrue(inventory.calls().containsAll(expectedRemovals),
				() -> spec.questId() + " 必须扣除整组检查物，实际 " + inventory.calls());
		}
	}

	/** 一条原版行的锚点事实。 / The anchor facts of one retail row. */
	private record Spec(int questId, int minLevel, int acceptItem, int xpBoostItem, List<WorkItem> workItems) {
	}

	/** 原版 check_item 组员。 / One check_item member. */
	private record WorkItem(int itemId, int count) {
	}
}
