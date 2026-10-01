package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 早期天族任务回归：已随真端表切换到 native 车道的行（1117/1118/1131/1141/1156/1158/1414/1691）
 * 只按真端表行 + quest.xml + 客户端页契约断言；其余任务仍走 IR 车道（1311/1647/1371/1561/1612/1626/
 * 1114/1464/1111/1162），断言面不变。
 * <p>
 * P3 re-anchor (plan §8.9): the rows that switched to the native lane assert retail-row, quest.xml and
 * client-page facts only; the remaining quests keep their IR assertions.
 */
class EarlyElyosQuestRegressionTest {
	/**
	 * 1118（폴리니아의 연고）：真端 cab520 只在 20000 分支发放 {@code give_item}
	 * （{@code ITEM_QUEST_1118A} ×1），1002 仅建档；交付门由表的 {@code item_check} 声明，
	 * 该行未声明 ⇒ 中继交还不回收工作物品。
	 * 1118: the retail accept branch grants the work item on 20000 only, and the row declares no
	 * {@code item_check}, so the hand-in neither gates nor consumes it.
	 */
	@Test
	void ointmentAcceptanceGrantsTheWorkItemOnTheRetailAcceptAction() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);

		assertEquals(203059, handler.acquireNpc(1118), "接取 NPC（Polinia）");
		assertEquals(new SimpleTalkHandler.ItemStack(182200224, 1), handler.acceptGiveItem(1118),
			"真端 give_item = ITEM_QUEST_1118A ×1");
		assertEquals(1, handler.relayCount(1118), "中继步数 = 1（Kustanon 203070）");
		assertTrue(handler.workItems(1118).isEmpty(), "真端行未声明 item_check：交付门不生效");

		// 1002（QUEST_ACCEPT_1）：只建档，不发放。
		Player plainAccept = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);
		NativeTalkFixture.clearPackets(plainAccept);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(plainAccept, 203059, 1118, 1002)), "1002 接取");
		assertEquals(QuestStatus.START, plainAccept.getQuestStateList().getQuestState(1118).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(plainAccept, SimpleTalkHandler.PAGE_ACCEPTED);
		assertEquals(List.of(), inventory.calls(), "1002 不发放");

		// 20000：建档 + 发放工作物品（真端 cab520 的 give_item 分支）。
		inventory.clear();
		Player itemAccept = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);
		NativeTalkFixture.clearPackets(itemAccept);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(itemAccept, 203059, 1118, 20000)), "20000 接取");
		assertEquals(QuestStatus.START, itemAccept.getQuestStateList().getQuestState(1118).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(itemAccept, SimpleTalkHandler.PAGE_ACCEPTED);
		assertEquals(List.of("give:182200224:1"), inventory.calls(), "20000 发放工作物品");
	}

	/**
	 * 1118 的交付段：中继步 1（Kustanon）推进到步 1，交付 NPC Melpone(203079) 的 1009 报告在
	 * 中继全满后翻 REWARD 并下发奖励窗；真端行无 item_check ⇒ 报告不校验也不扣除工作物品。
	 * 1118's hand-in: relay step 1 advances, then the report at Melpone(203079) flips REWARD with the
	 * reward window; the row has no item_check, so the report neither checks nor consumes the item.
	 */
	@Test
	void ointmentDeliveryWalksTheRetailRelayChainIntoTheRewardWindow() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(new NativeTalkFixture.RecordingInventory());
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);

		assertEquals(203079, handler.rewardNpc(1118), "交付 NPC（Melpone）");
		assertTrue(handler.relaysForNpc(203070).stream()
				.anyMatch(relay -> relay.questId() == 1118 && relay.step() == 1),
			"中继步 1 挂在 Kustanon 203070");
		// 20000 分支会经物品端口发放，走假背包处理器（真端 cab520 give_item 分支）。
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 203059, 1118, 20000)), "接取");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 203070, 1118, 10000)), "中继步 1");
		assertEquals(1, player.getQuestStateList().getQuestState(1118).getQuestVars().getQuestVars(), "步号 = 1");
		NativeTalkFixture.assertOnlyDialogPage(player, 1352);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 203079, 1118, 1009)), "交付报告");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1118).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	/**
	 * 1131（요새 내부 대화 퀘스트）：接取 Hyacinte(203097) 发 ITEM_QUEST_1131A(182200506)，
	 * 中继 Shugo_LF1a_01(799093) 的 10000 换手（发 DOC_QUEST_1131B 182200507、扣回 1131A），
	 * 交付 Nadaelo(203101) 报告翻 REWARD；con_quest 链式接取窗 = 1132。
	 * 1131: Hyacinte grants 1131A on accept, the Shugo relay swaps it for 1131B, Nadaelo hands in.
	 */
	@Test
	void armourTransferFollowsTheRetailRelayStepChannels() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);

		assertEquals(203097, handler.acquireNpc(1131), "接取 NPC（Hyacinte）");
		assertEquals(203101, handler.rewardNpc(1131), "交付 NPC（Nadaelo）");
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.acceptGiveItem(1131), "接取发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182200507, 1), handler.stepGiveItem(1131, 1), "步内发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.stepRemoveItem(1131, 1), "步内扣除");
		assertEquals(1132, handler.conQuest(1131), "链式接取窗下一环");

		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 203097, 1131, 20000)), "接取");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);
		assertEquals(List.of("give:182200506:1"), inventory.calls(), "接取发放 1131A");

		inventory.clear();
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 799093, 1131, 10000)), "中继换手");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(1131).getStatus(),
			"换手步不翻领奖态");
		NativeTalkFixture.assertOnlyDialogPage(player, 1352);
		assertEquals(List.of("give:182200507:1", "remove:182200506:1"), inventory.calls(), "步内先发后扣");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 203101, 1131, 1009)), "交付报告");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1131).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	/**
	 * 1156（톨바스 마을 도난 사건 &lt;2&gt;）：接取 Santenius(203128)，中继 BrownieLump_Q43(700003) 步骤 1，
	 * 交付 Gapir(798003)；真端行无 give/remove、无 item_check ⇒ 全程无物品通道；con_quest = 1157。
	 * 1156: Santenius acquires, the Brownie relays, Gapir hands in; the row declares no item channels.
	 */
	@Test
	void stolenSealChainKeepsTheBrownieRelayAndGapirHandIn() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();

		assertTrue(handler.routes(1156), "1156 必须由 native 车道路由");
		assertEquals(203128, handler.acquireNpc(1156), "接取 NPC（Santenius）");
		assertEquals(798003, handler.rewardNpc(1156), "交付 NPC（Gapir）");
		assertTrue(handler.relaysForNpc(700003).stream()
				.anyMatch(relay -> relay.questId() == 1156 && relay.step() == 1),
			"中继步 1 挂在 BrownieLump_Q43 700003");
		assertNull(handler.acceptGiveItem(1156), "接取无发放");
		assertNull(handler.stepGiveItem(1156, 1), "步内无发放");
		assertNull(handler.stepRemoveItem(1156, 1), "步内无扣除");
		assertTrue(handler.workItems(1156).isEmpty(), "无 item_check 门");
		assertFalse(handler.unresolvedGate(1156), "非 item_check 行无门");
		assertEquals(1157, handler.conQuest(1156), "链式接取窗下一环");
	}

	/**
	 * 1158（톨바스 마을 도난 사건 &lt;4&gt;）：与 1156 互为反向主（接取 Gapir 798003 / 交付 Santenius 203128），
	 * 中继 BrownieLump_Q43(700003) 步骤 1 发放 ITEM_QUEST_1158A(182200502)；quest.xml 前置 Q1157。
	 * 1158 mirrors 1156 (Gapir acquires, Santenius hands in); the relay grants 1158A; prerequisite Q1157.
	 */
	@Test
	void recoveredSealChainGrantsTheSealOnTheBrownieRelay() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);

		assertEquals(798003, handler.acquireNpc(1158), "接取 NPC（Gapir）");
		assertEquals(203128, handler.rewardNpc(1158), "交付 NPC（Santenius）");
		assertEquals(new SimpleTalkHandler.ItemStack(182200502, 1), handler.stepGiveItem(1158, 1), "步内发放印章");
		assertNull(handler.stepRemoveItem(1158, 1), "步内无扣除");
		assertTrue(handler.workItems(1158).isEmpty(), "真端行未声明 item_check：交付门不生效");
		assertEquals("Q1157", NativeQuestXmlTable.instance().require(1158).text("finished_quest_cond1"),
			"真端前置轴");

		NativeTalkFixture.completePrerequisites(player, 1157);
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 798003, 1158, 1002)), "接取");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);

		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 700003, 1158, 10000)), "中继步 1");
		NativeTalkFixture.assertOnlyDialogPage(player, 1352);
		assertEquals(List.of("give:182200502:1"), inventory.calls(), "步内发放印章");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 203128, 1158, 1009)), "交付报告");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1158).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	/**
	 * 1141（고고하스 대화퀘）：接取 Scarecrow_Nola(730001) / 交付 LF1a_Barrel(700122)，零中继、无交付门；
	 * 领奖窗由交付 NPC 自己承担（页与选择按钮同 owner）；quest.xml 前置 Q1143。
	 * 1141: Scarecrow_Nola acquires, the barrel object hands in; no relay, no gate, prerequisite Q1143.
	 */
	@Test
	void wineBarrelHandInUsesTheClientDeclaredReportOwner() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);

		assertTrue(handler.routes(1141), "1141 必须由 native 车道路由");
		assertEquals(730001, handler.acquireNpc(1141), "接取 NPC（Scarecrow_Nola）");
		assertEquals(700122, handler.rewardNpc(1141), "交付 NPC（LF1a_Barrel 物件）");
		assertEquals(0, handler.relayCount(1141), "无中继步");
		assertTrue(handler.workItems(1141).isEmpty(), "无 item_check 门");
		assertNull(handler.acceptGiveItem(1141), "接取无发放");
		assertEquals("Q1143", NativeQuestXmlTable.instance().require(1141).text("finished_quest_cond1"),
			"真端前置轴");

		NativeTalkFixture.completePrerequisites(player, 1143);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 730001, 1141, 31)), "接取问询");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(1141));
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 730001, 1141, 1002)), "接取确认");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);

		// 报告：零中继 + 空门 ⇒ 直接翻 REWARD 并下发奖励窗（页 5 与选择按钮同 owner）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 700122, 1141, 1009)), "酒桶报告");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1141).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	@Test
	void weddingRingRestoresTheBarrelAndBothCollectorRewards() {
		CompiledQuestDefinition definition = load(1162);

		assertObjectGate(definition, "started", 700005);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(3739)),
			route(definition, "started", "started", new QuestEvent.TalkToNpc(700005, -1)).afterCommit());
		QuestTransition ring = route(definition, "started", "ring-found",
			new QuestEvent.TalkToNpc(700005, 10000));
		assertTrue(ring.conditions().contains(new QuestCondition.HasItem(182200563, 1, false)));
		assertTrue(ring.actions().contains(new QuestAction.GiveItem(182200563, 1)));
		assertEquals(new AfterCommitAction.CloseDialog(), ring.afterCommit().getLast());

		QuestTransition main = route(definition, "ring-found", "reward-main",
			new QuestEvent.TalkToNpc(203095, 39));
		QuestTransition alternate = route(definition, "ring-found", "reward-alternate",
			new QuestEvent.TalkToNpc(203093, 39));
		for (QuestTransition collector : List.of(main, alternate)) {
			assertTrue(collector.conditions().contains(new QuestCondition.HasItem(182200563, 1)));
			assertTrue(collector.actions().contains(new QuestAction.RemoveItem(182200563, 1)));
		}
		assertTrue(main.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(5)));
		assertTrue(alternate.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(6)));
		assertEquals(2, definition.definition().metadata().rewardGroups().size());
		assertTrue(route(definition, "reward-main", "complete", new QuestEvent.TalkToNpc(203095, 8))
			.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(route(definition, "reward-alternate", "complete", new QuestEvent.TalkToNpc(203093, 8))
			.actions().contains(new QuestAction.CompleteQuest(1)));
		assertNoUnacceptedObjectRoute(definition, 700005);
	}

	/**
	 * 1311（진균 재배지）仍在 IR 车道（{@code definitions/quests/1311.xml}）：物件门 + 工作物品回收。
	 * 1414 已随 P3 移出 IR，见下方 native 用例。
	 * 1311 stays on the IR lane; 1414 left it with P3 (native case below).
	 */
	@Test
	void germObjectRequiresAndConsumesItsWorkItem() {
		CompiledQuestDefinition definition = load(1311);
		int startNpc = 203997;
		int objectNpc = 700164;
		int workItem = 182201305;

		assertTrue(definition.definition().transitions().stream().anyMatch(transition ->
			Objects.equals(transition.sourceNode(), "unaccepted") && transition.targetNode().equals("started")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == startNpc
				&& transition.actions().contains(new QuestAction.GiveItem(workItem, 1))));
		assertObjectGate(definition, "started", objectNpc);
		QuestTransition use = route(definition, "started", "reward",
			new QuestEvent.TalkToNpc(objectNpc, -1));
		assertTrue(use.conditions().contains(new QuestCondition.HasItem(workItem, 1)));
		assertTrue(use.actions().contains(new QuestAction.RemoveItem(workItem, 1)));
		assertTrue(use.afterCommit().contains(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));
		assertNoUnacceptedObjectRoute(definition, objectNpc);
	}

	/**
	 * 1414（카이단 괴멸작전 시작）：接取/交付同主 Aeolus(203989)，中继 LF2_Gear_Q1414(700175) 步 1 发
	 * ITEM_QUEST_1414A(182201349)；quest.xml 前置 Q1413 未完成时真端拒接（native fail-closed）。
	 * 1414: same-NPC owners, one relay that grants 1414A; acquisition fails closed without Q1413.
	 */
	@Test
	void gearRelayGrantsTheQuestItemAndFailsClosedWithoutThePrerequisite() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 40);

		assertTrue(handler.routes(1414), "1414 必须由 native 车道路由");
		assertEquals(203989, handler.acquireNpc(1414), "接取 NPC（Aeolus）");
		assertEquals(203989, handler.rewardNpc(1414), "交付 NPC（真端同主）");
		assertEquals(1, handler.relayCount(1414), "中继步数 = 1");
		assertTrue(handler.relaysForNpc(700175).stream()
				.anyMatch(relay -> relay.questId() == 1414 && relay.step() == 1),
			"中继步 1 挂在 LF2_Gear_Q1414");
		assertEquals(new SimpleTalkHandler.ItemStack(182201349, 1), handler.stepGiveItem(1414, 1), "步内发放");
		assertTrue(handler.workItems(1414).isEmpty(), "真端行未声明 item_check：交付门不生效");
		assertEquals("Q1413", NativeQuestXmlTable.instance().require(1414).text("finished_quest_cond1"),
			"真端前置轴");

		// 真端 quest.xml 声明 bm_restrict_category=1：128 位地图位集语义未坐实 ⇒ native fail-closed，
		// 该行当前不可接取（阻塞项登记于计划 §10.3；接线前不得放行兜底）。
		// The retail row declares bm_restrict_category=1; the 128-bit map bitset is not decoded yet, so the
		// native lane fails closed and this row cannot be acquired (registered blocker in plan §10.3).
		assertEquals("1", NativeQuestXmlTable.instance().require(1414).text("bm_restrict_category"),
			"真端 bm 轴");
		NativeTalkFixture.clearPackets(player);
		assertEquals(NativeQuestStartPort.Outcome.BM_RESTRICT_UNRESOLVED,
			NativeQuestStartPort.instance().evaluateNpcAcquire(player, 1414).outcome(), "bm 轴 fail-closed");
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, 203989, 1414, 1002)), "bm 未坐实不得建档");
		assertNull(player.getQuestStateList().getQuestState(1414), "拒接不得落库");
		assertEquals(List.of(), NativeTalkFixture.dialogPages(player), "拒接不发页");

		// 接取轴外的形状（中继发物 → 报告领奖）用直建 START 行验证：机械面必须与真端行一致。
		// The relay/report machinery is verified on a seeded START row (the acquire axis above is blocked).
		NativeTalkFixture.start(player, 1414);
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 700175, 1414, 10000)), "中继步 1");
		NativeTalkFixture.assertOnlyDialogPage(player, 1352);
		assertEquals(List.of("give:182201349:1"), inventory.calls(), "步内发放");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 203989, 1414, 1009)), "交付报告");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1414).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	@Test
	void flowerDeliveryUnlocksTheShrineAndMiserChestCanReopenAtReward() {
		CompiledQuestDefinition flowers = load(1371);
		QuestTransition check = route(flowers, "started", "started",
			new QuestEvent.TalkToNpc(203949, 39));
		assertTrue(check.conditions().contains(new QuestCondition.HasItem(152000601, 5)));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1353)), check.afterCommit());
		QuestTransition delivery = route(flowers, "started", "flowers-delivered",
			new QuestEvent.TalkToNpc(203949, 10000));
		assertTrue(delivery.actions().contains(new QuestAction.RemoveItem(152000601, 5)));
		assertObjectGate(flowers, "flowers-delivered", 730039);
		route(flowers, "flowers-delivered", "reward", new QuestEvent.TalkToNpc(730039, -1));
		assertNoUnacceptedObjectRoute(flowers, 730039);

		CompiledQuestDefinition map = load(1561);
		// W6 尾（quest-native-dispatch）：1561 迁 SimpleUseItem 规范形——宝箱 700188 的
		// QUEST_SELECT(31) 提交边从 started 直翻领奖态并下发奖励窗，旧 CanAct 物件门与
		// -1→2375 页随物件规范形退场；"领奖态宝箱可重开"由 -1/1009 → 窗 5 的自环保留。
		// W6 tail: 1561 moved to the SimpleUseItem canonical — the chest's QUEST_SELECT(31)
		// submission edge flips REWARD from started with the reward window; the legacy CanAct
		// object gates and the -1->2375 page retire, while "the chest reopens at reward" survives
		// as the -1/1009 -> window-5 self-loops.
		QuestTransition chestDelivery = route(map, "started", "reward",
			new QuestEvent.TalkToNpc(700188, 31));
		assertEquals(List.of(), chestDelivery.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)), chestDelivery.afterCommit());
		for (int dialogId : List.of(-1, 1009)) {
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(5)),
				route(map, "reward", "reward", new QuestEvent.TalkToNpc(700188, dialogId)).afterCommit());
		}
		assertEquals(new AfterCommitAction.CloseDialog(),
			route(map, "reward", "complete", new QuestEvent.TalkToNpc(700188, 108)).afterCommit().getLast());
	}

	@Test
	void lepharistObjectRequiresFourUsesAndRespawnsBetweenUses() {
		CompiledQuestDefinition definition = load(1612);
		List<String> sources = List.of("started", "used1", "used2", "used3");
		List<String> targets = List.of("used1", "used2", "used3", "reward");
		for (int i = 0; i < sources.size(); i++) {
			assertObjectGate(definition, sources.get(i), 700352);
			QuestTransition use = route(definition, sources.get(i), targets.get(i),
				new QuestEvent.TalkToNpc(700352, -1));
			assertEquals(new AfterCommitAction.DeleteInteractionNpc(true), use.afterCommit().getLast());
			QuestStateSyncMode mode = i == sources.size() - 1
				? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
			assertTrue(use.afterCommit().contains(new AfterCommitAction.SyncQuestState(mode)));
		}
		assertNoUnacceptedObjectRoute(definition, 700352);
	}

	@Test
	void pathLightsMustBeActivatedInOrderWithTheQuestItem() {
		CompiledQuestDefinition definition = load(1626);
		List<String> sources = List.of("started", "lit1", "lit2", "lit3", "lit4", "lit5", "lit6");
		List<String> targets = List.of("lit1", "lit2", "lit3", "lit4", "lit5", "lit6", "reward");
		for (int i = 0; i < sources.size(); i++) {
			int objectNpc = 700221 + i;
			assertObjectGate(definition, sources.get(i), objectNpc);
			QuestTransition use = route(definition, sources.get(i), targets.get(i),
				new QuestEvent.TalkToNpc(objectNpc, -1));
			assertTrue(use.conditions().contains(new QuestCondition.HasItem(182201788, 1)));
			QuestStateSyncMode mode = i == sources.size() - 1
				? QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH : QuestStateSyncMode.PACKET_ONLY;
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(mode)), use.afterCommit());
			assertNoUnacceptedObjectRoute(definition, objectNpc);
		}
	}

	/** 1647（볼빅 동상）仍在 IR 车道：物件门 + 装备门，交付不需工作物品。 /
	 * 1647 stays on the IR lane: object gate plus equipped-item gates. */
	@Test
	void bollvigStatueRestoresItsLegacyGates() {
		CompiledQuestDefinition bollvig = load(1647);
		assertObjectGate(bollvig, "started", 700272);
		QuestTransition statue = route(bollvig, "started", "reward",
			new QuestEvent.TalkToNpc(700272, -1));
		assertTrue(statue.conditions().contains(new QuestCondition.HasItem(182201783, 1)));
		assertTrue(statue.conditions().contains(new QuestCondition.EquippedItem(110100150)));
		assertTrue(statue.conditions().contains(new QuestCondition.EquippedItem(113100144)));
		assertNoUnacceptedObjectRoute(bollvig, 700272);
	}

	/**
	 * 1691（내가 니 딸이다1）：接取/交付同主 Harmone(798386)，三段中继 Noiyus(790005) →
	 * Harmone(798386) → LF3_FOBJ_Q1691(700563)，第 3 步发 ITEM_QUEST_1691A(182201826)；
	 * quest.xml 前置 Q1932，con_quest 链式接取窗 = 1692。
	 * 1691: three-step relay ladder with the 1691A grant on step 3; prerequisite Q1932.
	 */
	@Test
	void leatherSlipperChainFollowsTheThreeStepRetailLadder() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 40);

		assertTrue(handler.routes(1691), "1691 必须由 native 车道路由");
		assertEquals(798386, handler.acquireNpc(1691), "接取 NPC（Harmone）");
		assertEquals(798386, handler.rewardNpc(1691), "交付 NPC（真端同主）");
		assertEquals(3, handler.relayCount(1691), "中继步数 = 3");
		int[][] ladder = {{1, 790005}, {2, 798386}, {3, 700563}};
		for (int[] step : ladder) {
			assertTrue(handler.relaysForNpc(step[1]).stream()
					.anyMatch(relay -> relay.questId() == 1691 && relay.step() == step[0]),
				"第 " + step[0] + " 步必须挂在 " + step[1]);
		}
		assertNull(handler.stepGiveItem(1691, 1), "第 1 步无发放");
		assertNull(handler.stepGiveItem(1691, 2), "第 2 步无发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182201826, 1), handler.stepGiveItem(1691, 3), "第 3 步发放");
		assertEquals(1692, handler.conQuest(1691), "链式接取窗下一环");
		assertEquals("Q1932", NativeQuestXmlTable.instance().require(1691).text("finished_quest_cond1"),
			"真端前置轴");

		// 真端 quest.xml 声明 bm_restrict_category=1：128 位地图位集未坐实 ⇒ native fail-closed（计划 §10.3）。
		// The row declares bm_restrict_category=1; the 128-bit map bitset is not decoded yet ⇒ fail closed.
		assertEquals("1", NativeQuestXmlTable.instance().require(1691).text("bm_restrict_category"),
			"真端 bm 轴");
		assertEquals(NativeQuestStartPort.Outcome.BM_RESTRICT_UNRESOLVED,
			NativeQuestStartPort.instance().evaluateNpcAcquire(player, 1691).outcome(), "bm 轴 fail-closed");
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, 798386, 1691, 1002)), "bm 未坐实不得建档");
		assertNull(player.getQuestStateList().getQuestState(1691), "拒接不得落库");

		// 直建 START 行验证三段阶梯与报告领奖的机械面。 / Seed START and walk the three-step ladder.
		NativeTalkFixture.start(player, 1691);
		for (int index = 0; index < ladder.length; index++) {
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, ladder[index][1], 1691,
				10000 + index)), "第 " + (index + 1) + " 步推进");
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.pageForStep(index + 1));
		}
		assertEquals(List.of("give:182201826:1"), inventory.calls(), "只有第 3 步发放");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, 798386, 1691, 1009)), "交付报告");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1691).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	/**
	 * 范围外红（登记不修）：1137 属 P4 SimpleCollectItem 族的真端行，其生产定义已由族编译器改为
	 * 交付规范形，本用例仍按旧 IR 边的形状断言 ⇒ 随 P4 切换批重锚（不属 P3 步骤 5）。
	 * Out-of-scope red (registered, not fixed): 1137 belongs to the P4 SimpleCollectItem family; its
	 * production shape moved with the family compiler, so this IR-edge assertion re-anchors with P4.
	 */
	@Test
	void fossilCollectionPublishesProgressAndFinalNpcConsumesOnlyTheCollectedItem() {
		CompiledQuestDefinition definition = load(1137);
		QuestTransition collection = definition.definition().transitions().stream()
			.filter(route -> Objects.equals(route.sourceNode(), "started")
				&& route.targetNode().equals("started")
				&& route.event().equals(new QuestEvent.CollectItem(182200513, 1)))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			collection.afterCommit());

		QuestTransition report = route(definition, "started", "reward",
			new QuestEvent.TalkToNpc(203111, 39));
		assertTrue(report.conditions().contains(new QuestCondition.HasItem(182200513, 1)));
		assertFalse(report.conditions().contains(new QuestCondition.HasItem(182200512, 1)));
		assertTrue(report.actions().contains(new QuestAction.RemoveItem(182200513, 1)));
		assertFalse(report.actions().contains(new QuestAction.RemoveItem(182200512, 1)));
	}

	@Test
	void nymphGownItemStartAndFullLifecycleAlignsWithLiveClientTrace() {
		CompiledQuestDefinition definition = load(1114);

		QuestTransition useItem = route(definition, "unaccepted", "unaccepted",
			new QuestEvent.UseItem(182200214));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4)), useItem.afterCommit());

		QuestTransition accept = route(definition, "unaccepted", "v0",
			new QuestEvent.QuestDialog(QuestDialogAction.QUEST_ACCEPT_1.id()));
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(182200226, 1)));
		assertTrue(accept.actions().contains(new QuestAction.RemoveItem(182200214, 1)));

		QuestTransition gown = route(definition, "v1", "v2",
			new QuestEvent.TalkToNpc(700008, -1));
		assertTrue(gown.actions().contains(new QuestAction.GiveItem(182200217, 1)));

		QuestTransition handoff = route(definition, "v2", "v3",
			new QuestEvent.TalkToNpc(203075, 2375));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(2375)), handoff.afterCommit());

		QuestTransition rewardPreview = route(definition, "v3", "reward4",
			new QuestEvent.TalkToNpc(203075, 1009));
		assertTrue(rewardPreview.actions().contains(new QuestAction.RemoveItem(182200217, 1)));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(6)), rewardPreview.afterCommit());
	}

	private static QuestTransition route(CompiledQuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(route -> Objects.equals(route.sourceNode(), source) && route.targetNode().equals(target)
				&& route.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertObjectGate(CompiledQuestDefinition definition, String source, int npcId) {
		QuestTransition gate = route(definition, source, source,
			new QuestEvent.CanAct(npcId, "ACTION_ITEM_USE"));
		assertEquals(List.of(), gate.actions());
		assertEquals(List.of(), gate.afterCommit());
	}

	private static void assertNoUnacceptedObjectRoute(CompiledQuestDefinition definition, int npcId) {
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			Objects.equals(transition.sourceNode(), "unaccepted")
				&& ((transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
					|| (transition.event() instanceof QuestEvent.CanAct canAct
						&& canAct.templateId() == npcId))));
	}

	/** 交付窗页（与 RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage 同口径：档位查表，零奖励组回落窗 1）。 */
	private static int deliveryWindowPage(QuestMetadata metadata) {
		return metadata.rewardGroups().isEmpty()
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1).orElseThrow().id();
	}

	private static CompiledQuestDefinition load(int questId) {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
	@Test
	void insomniaMedicineAcceptanceOpensAskAcceptWindowNotRefusePage() {
		CompiledQuestDefinition definition = load(1111);
		QuestTransition askAccept = route(definition, "unaccepted", "unaccepted",
			new QuestEvent.TalkToNpc(203075, 1007));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4)), askAccept.afterCommit());

		QuestTransition accept = route(definition, "unaccepted", "started",
			new QuestEvent.TalkToNpc(203075, 1002));
		assertTrue(accept.conditions().contains(new QuestCondition.StartEligible()));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), accept.afterCommit());
	}

	/**
	 * 1117（스파키의 발광체）：接取/交付同主 Pranoa(203074)，表行 {@code item_check=1} 且
	 * quest.xml {@code collect_item1 = check_item1_1 = quest_1117a 3} ⇒ 交付门 = 工作物品 ×3；
	 * 报告时未集齐保持 START（进行中页 10），集齐才翻 REWARD 并扣整组。
	 * 1117: same-NPC acquisition and hand-in with the retail item_check gate (3 items).
	 */
	@Test
	void singleItemCollection1117UsesTheRetailItemCheckGate() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 20);

		assertEquals(203074, handler.acquireNpc(1117), "接取 NPC（Pranoa）");
		assertEquals(203074, handler.rewardNpc(1117), "交付 NPC（真端同主）");
		assertEquals(0, handler.relayCount(1117), "无中继步");
		assertTrue(NativeTalkFixture.row(1117).itemCheck(), "表行声明 item_check");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(182200208, 3)), handler.workItems(1117),
			"交付门 = quest.xml collect_item1 ×3");
		assertFalse(handler.unresolvedGate(1117), "交付门必须可解");
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().require(1117);
		assertEquals("quest_1117a 3", row.text("collect_item1"), "真端收集列");
		assertEquals("quest_1117a 3", row.text("check_item1_1"), "真端交付门列");

		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 203074, 1117, 1002)), "接取");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);

		// 未集齐：报告门保持 START。 / Without the items the report gate holds.
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 203074, 1117, 1009)), "报告被受理");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(1117).getStatus(),
			"未集齐必须保持 START");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_IN_PROGRESS);

		inventory.hold(182200208, 3);
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, 203074, 1117, 1009)), "交付报告");
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(1117).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
		assertEquals(List.of("remove:182200208:3"), inventory.calls(), "交付门按真端扣除整组");
	}

	@Test
	void spyGathering1464RequiresFifteenTheoniaBeforeRewardAtJinus() {
		CompiledQuestDefinition definition = load(1464);
		QuestTransition startSelect = route(definition, "started", "started",
			new QuestEvent.TalkToNpc(204424, 31));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)), startSelect.afterCommit());

		QuestTransition itemDelivery = route(definition, "started", "reward",
			new QuestEvent.TalkToNpc(204424, 39));
		assertTrue(itemDelivery.conditions().contains(new QuestCondition.HasItem(152000455, 15)));
		assertTrue(itemDelivery.actions().contains(new QuestAction.RemoveItem(152000455, 15)));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(10000)), itemDelivery.afterCommit());

		QuestTransition rewardReport = route(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(203755, 31));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10002)), rewardReport.afterCommit());
	}
}
