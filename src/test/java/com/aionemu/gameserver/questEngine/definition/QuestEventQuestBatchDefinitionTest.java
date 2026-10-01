package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 80008/80009（Cake 系：用物接取 + NPC 交付）与 80028/80031/80032（Fayrefolk 系：接取/交付同主对话）：
 * 两族都已随 P5/P3 切到 native 直驱（真端表行 + 真端 {@code quest.xml} + 客户端页契约），断言面只写真端
 * 事实——用物开接取窗页 4、无主 1002 建档、交付动作直翻领奖态并下发奖励窗、无中继步、无工作物品门。
 * <p>
 * 已退场的旧 IR 合成语义（真端无据，随切换批删除，边界见 {@code p5/P5-REPORT.zh-CN.md}）：
 * ① 由 {@code quest_work_item1} 反推的 {@code HasItem} 车位门与 {@code RemoveItem} 代扣——真端记录开关
 * 是 {@code item_check}，这两行均为 false（P3-STEP2 §3 的开关口径）；② {@code LevelUp} +
 * {@code EventActive(false)} → {@code AbandonQuest} 的过期活动弃任——活动状态轴的数据
 * （真端 {@code event_quest.xml}）全域缺失，不可实证，登记为情报缺口而不合成。
 * <p>
 * The cake and fayrefolk event rows run on the native lanes; every assertion is a retail row, retail
 * {@code quest.xml} or client page-contract fact. The two synthesized legacy semantics (a work-item
 * gate derived from {@code quest_work_item1} despite {@code item_check=false}, and a level-up abandon
 * driven by an event-active axis whose retail data is missing entirely) retired with the switch batch
 * and are registered as evidence boundaries instead of being re-invented.
 */
class QuestEventQuestBatchDefinitionTest {

	/** Cake 系真端行（用物接取）。 / The cake rows (item-use accept). */
	private static final int[] CAKE_QUESTS = {80008, 80009};
	/** Fayrefolk 系真端行（对话接取/交付）。 / The fayrefolk rows (talk accept and hand-in). */
	private static final int[] FAYREFOLK_QUESTS = {80028, 80031, 80032};
	/** 真端接取窗页（两族客户端任务页都声明 {@code ask_quest_accept}）。 / The retail ask window page. */
	private static final int ASK_WINDOW_PAGE = QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
	/** 客户端信页：极简信页 select_none(4762) / 对话入口页 select1(1011)。 / Client letter pages. */
	private static final int SELECT_NONE_PAGE = QuestDialogPage.SELECT_NONE.id();
	private static final int SELECT1_PAGE = QuestDialogPage.SELECT1.id();

	private static final SimpleUseItemHandler USE = SimpleUseItemHandler.instance();
	private static final SimpleTalkHandler TALK = NativeTalkFixture.handler();

	@Test
	void cakeRowsMatchRetailRecordAndQuestXml() throws Exception {
		assertCake(80008, "Q80008", "quest_80008a", 798415, "pc_light", 10);
		assertCake(80009, "Q80009", "quest_80009a", 798417, "pc_dark", 10);
	}

	@Test
	void fayrefolkRowsMatchRetailRecordAndQuestXml() throws Exception {
		assertFayrefolk(80028, "Q80028", 799766, "pc_light", 10);
		assertFayrefolk(80031, "Q80031", 799781, "pc_dark", 10);
		assertFayrefolk(80032, "Q80032", 799781, "pc_dark", 15);
	}

	@Test
	void cakeAcceptAndHandInRunOnTheItemUseLane() {
		for (int questId : CAKE_QUESTS) {
			Player player = NativeTalkFixture.player(laneRace(questId), PlayerClass.WARRIOR, 20);
			Integer itemId = USE.useItemId(questId);
			int rewardNpc = USE.rewardNpcs(questId).getFirst();

			NativeTalkFixture.clearPackets(player);
			assertTrue(USE.onItemUse(player, itemId), "用物必须开接取窗: " + questId);
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_ASK_ACCEPT);

			NativeTalkFixture.clearPackets(player);
			assertTrue(USE.onDialog(NativeTalkFixture.dialog(player, 0, questId, 1002)),
				"无主 1002 必须建档: " + questId);
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus());

			NativeTalkFixture.clearPackets(player);
			assertTrue(USE.onDialog(NativeTalkFixture.dialog(player, rewardNpc, questId, 31)),
				"交付动作 31 必须直翻领奖态: " + questId);
			assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(questId).getStatus());
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_REWARD_WINDOW);
		}
	}

	@Test
	void fayrefolkQuestsReportIntoRewardWithoutCollectibles() {
		for (int questId : FAYREFOLK_QUESTS) {
			Player player = NativeTalkFixture.player(laneRace(questId), PlayerClass.WARRIOR, 20);
			int npcId = TALK.acquireNpc(questId);

			NativeTalkFixture.clearPackets(player);
			assertTrue(TALK.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 31)), "接取问询: " + questId);
			// 真端入口页 = 信页（select1 1011）；页 4 只由页动作 1007 打开（独立复算偏好序）。
			// Retail entry page = the letter page; page 4 is reachable only through action 1007.
			int expectedEntry = QuestDialogContract.loadDefault().hasButtonPage(questId, SELECT_NONE_PAGE)
				? SELECT_NONE_PAGE
				: QuestDialogContract.loadDefault().hasButtonPage(questId, SELECT1_PAGE)
					? SELECT1_PAGE : ASK_WINDOW_PAGE;
			NativeTalkFixture.assertOnlyDialogPage(player, expectedEntry);

			NativeTalkFixture.clearPackets(player);
			assertTrue(TALK.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1007)),
				"页动作 1007（ASK_QUEST_ACCEPT）必须打开接取窗: " + questId);
			NativeTalkFixture.assertOnlyDialogPage(player, ASK_WINDOW_PAGE);

			NativeTalkFixture.clearPackets(player);
			assertTrue(TALK.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1002)), "接取确认: " + questId);
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus());

			// 真端 SimpleTalk 报告链是 1009（cabb10 → mgr+0x1c8 奖励窗）；8..23/108/110..124 是领奖态
			// 的结算按钮（23 = SELECTED_QUEST_NOREWARD），不是 START 态的报告口。
			// The retail report link is 1009; the 8..23 button range belongs to the reward window.
			NativeTalkFixture.clearPackets(player);
			assertTrue(TALK.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1009)),
				"报告动作 1009 必须进领奖态: " + questId);
			assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(questId).getStatus());
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
		}
	}

	/**
	 * 旧 IR 的两处合成语义必须随切换批退场：真端记录既无 {@code item_check} 开关（无工作物品门/代扣），
	 * 也无 event-active 轴（无过期弃任）。任一断言变化都说明真端行或契约发生漂移。
	 * <p>
	 * The two synthesized legacy semantics must stay retired: the retail record switches off the
	 * item_check channel and carries no event-active axis at all.
	 */
	@Test
	void eventRowsCarryNoSynthesizedWorkItemGateOrEventActiveAxis() throws Exception {
		for (int questId : CAKE_QUESTS) {
			assertTrue(USE.routes(questId), "cake 行必须由 native 车道服务: " + questId);
			assertTrue(USE.gateItems(questId).isEmpty(), "真端 item_check=false ⇒ 无工作物品门: " + questId);
			assertTrue(USE.relayNpcs(questId).isEmpty(), "真端无 talk_npc 列 ⇒ 无中继步: " + questId);
			assertNoEventActiveAxis(questId);
		}
		for (int questId : FAYREFOLK_QUESTS) {
			assertTrue(TALK.routes(questId), "fayrefolk 行必须由 native 车道服务: " + questId);
			assertTrue(TALK.workItems(questId).isEmpty(), "真端 item_check=false ⇒ 无工作物品门: " + questId);
			assertEquals(0, TALK.relayCount(questId), "真端无 talk_npc 列 ⇒ 无中继步: " + questId);
			assertNoEventActiveAxis(questId);
		}
	}

	// ------------------------------------------------------------------ 事实层

	private static void assertCake(int questId, String name, String workItemSymbol, int rewardNpc,
			String permittedRace, int minLevel) throws Exception {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().require(questId);
		assertEquals(name, row.text("name"), "真端 quest.xml 任务名");
		assertEquals(minLevel, row.integer("minlevel_permitted"), "真端 quest.xml 等级轴");
		assertEquals("event", row.text("category1"), "真端 quest.xml 类别");
		assertTrue(row.list("race_permitted").contains(permittedRace), "真端 quest.xml 种族轴");
		assertEquals(10, row.integer("max_repeat_count"), "真端 quest.xml 重复上限");
		assertEquals(List.of(workItemSymbol), row.list("quest_work_item1"),
			"真端工作物品列（quest.xml 原名形；记录开关 item_check=false ⇒ 不设门）");

		assertTrue(USE.routes(questId), "行必须路由到 native 车道");
		assertEquals(List.of(rewardNpc), USE.rewardNpcs(questId), "真端 reward_npc_name 解析");
		assertTrue(USE.relayNpcs(questId).isEmpty(), "真端无 talk_npc 列");
		assertTrue(USE.gateItems(questId).isEmpty(), "真端 item_check=false");
		Integer itemId = USE.useItemId(questId);
		assertNotNull(itemId, "真端 use_item_name 必须解析: " + workItemSymbol);
		assertTrue(USE.acceptQuestIdsForItem(itemId).contains(questId), "接取道具必须指回本行");
		assertTrue(QuestDialogContract.loadDefault().hasButtonPage(questId, ASK_WINDOW_PAGE),
			"客户端任务页必须声明接取窗页 ask_quest_accept");
		assertEquals(laneRace(questId), questRace(permittedRace), "测试玩家种族必须匹配真端种族轴");

		RetailQuestMetadataCompiler.Outcome compiled = metadata(questId);
		assertTrue(compiled.clean(), "真端奖励符号必须全部解析: " + compiled.unresolved());
		assertEquals(List.of(new QuestReward("GOLD", 0, 20000L), new QuestReward("EXP", 0, 10000L),
			new QuestReward("ITEM", 160010100, 5L), new QuestReward("ITEM", 164002019, 3L)),
			compiled.metadata().rewards(), "真端 quest.xml 奖励列");
		// 旧的 HasItem 门正是从这个工作物品列反推的合成语义（记录开关关着 ⇒ 退场）。
		// The retired HasItem gate was derived from exactly this work-item column.
		assertEquals(List.of(new QuestItemRequirement(itemId, 1)), compiled.metadata().questWorkItems(),
			"真端工作物品 = 用物品本体");
	}

	private static void assertFayrefolk(int questId, String name, int npcId, String permittedRace,
			int minLevel) throws Exception {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().require(questId);
		assertEquals(name, row.text("name"), "真端 quest.xml 任务名");
		assertEquals(minLevel, row.integer("minlevel_permitted"), "真端 quest.xml 等级轴");
		assertEquals("event", row.text("category1"), "真端 quest.xml 类别");
		assertTrue(row.list("race_permitted").contains(permittedRace), "真端 quest.xml 种族轴");
		assertEquals(1, row.integer("max_repeat_count"), "真端 quest.xml 重复上限");

		assertTrue(TALK.routes(questId), "行必须路由到 native 车道");
		assertEquals(npcId, TALK.acquireNpc(questId), "真端 acquired_npc_name 解析");
		assertEquals(npcId, TALK.rewardNpc(questId), "真端 reward_npc_name 解析（同主对话形）");
		assertEquals(0, TALK.relayCount(questId), "真端无 talk_npc 列");
		assertTrue(TALK.workItems(questId).isEmpty(), "真端 item_check=false（无收集物）");
		assertTrue(QuestDialogContract.loadDefault().hasButtonPage(questId, ASK_WINDOW_PAGE),
			"客户端任务页必须声明接取窗页 ask_quest_accept");
		assertEquals(laneRace(questId), questRace(permittedRace), "测试玩家种族必须匹配真端种族轴");

		RetailQuestMetadataCompiler.Outcome compiled = metadata(questId);
		assertTrue(compiled.clean(), "真端奖励符号必须全部解析: " + compiled.unresolved());
		assertEquals(questId == 80032
				? List.of(new QuestReward("ITEM", 188051133, 10L))
				: List.of(new QuestReward("ITEM", 169610036, 1L)),
			compiled.metadata().rewards(), "真端 quest.xml 奖励列");
	}

	/** 真端 quest.xml 行是否带 event-active 轴（现状：全域无，故活动弃任不可实证）。 /
	 * Whether the retail row carries an event-active axis (none today, so the abandon is unprovable). */
	private static void assertNoEventActiveAxis(int questId) {
		assertFalse(NativeQuestXmlTable.instance().require(questId).fields().keySet().stream()
			.anyMatch(key -> key.contains("active")),
			"真端 quest.xml 无 event-active 轴可依: " + questId);
	}

	/** 该行真端种族轴对应的测试玩家种族（80008/80028 天族，其余魔族）。 /
	 * Test-player race per the retail race axis (light for 80008/80028, dark for the rest). */
	private static Race laneRace(int questId) {
		return questId == 80008 || questId == 80028 ? Race.ELYOS : Race.ASMODIANS;
	}

	private static Race questRace(String permittedRace) {
		return "pc_light".equals(permittedRace) ? Race.ELYOS : Race.ASMODIANS;
	}

	private static RetailQuestMetadataCompiler.Outcome metadata(int questId) throws Exception {
		return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId).orElseThrow();
	}
}
