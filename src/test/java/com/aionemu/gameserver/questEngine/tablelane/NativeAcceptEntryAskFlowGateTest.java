package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 跨族接取页链门：原版入口页表（信页/阶段页）与页动作 {@code 1007} 的 ask 流。
 * <p>
 * 原版入口选择器（{@code fun_731}）对未接态只回 {@code select_none}(4762)、阶段页 {@code select1}(1011)
 * 或 {@code default_success}(10002)，**从不回接取窗页 4**；页 4 由页动作 {@code 1007}
 * （{@code ASK_QUEST_ACCEPT} → {@code mgr+0x1a0}）打开。故本门冻结两件事：① 入口页必须等于客户端任务页
 * 声明中偏好最高的信页，绝不发明页；② 声明页 4 的行由 {@code 1007} 打开页 4，未声明页 4 的行必须
 * fail-closed（无包下发）。
 * <p>
 * Cross-family gate for the accept page chain: the retail entry selector sends a letter/stage page and
 * page 4 is reachable only through the {@code 1007} page action. The gate freezes (1) the entry page never
 * being an undeclared page and (2) the ask action opening page 4 only where the client declares it.
 */
class NativeAcceptEntryAskFlowGateTest {

	private static final String CONTRACT_RESOURCE = "aion/definitions/quest_dialog/client_dialog_contract.tsv";
	private static final String HUNT_RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleHunt.xml";
	private static final String TALK_RESOURCE = "aion/data/static_data/quest/retail/Quest_SimpleTalk.xml";

	/** 原版 ask 流挂在过场上的行（hunt 3 + talk 2）：quest → movie。 */
	private static final Map<Integer, Integer> ASK_CUTSCENE_ROWS = Map.of(
		3016, 362, 4007, 391, 4014, 393, 3020, 363, 4056, 403);

	/** 客户端未声明页 4 的样例（hunt 1346：只有 select1 阶梯）。 */
	private static final int NO_ASK_WINDOW_QUEST = 1346;

	private static final int SELECT_NONE = QuestDialogPage.SELECT_NONE.id();
	private static final int SELECT1 = QuestDialogPage.SELECT1.id();
	private static final int ASK_WINDOW = QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();

	/**
	 * 入口页规则：客户端声明优先序 {@code select_none(4762) → select1(1011) → 页 4 兜底}，且除「三类都未声明」
	 * 的兜底行外，下发的入口页必须在客户端声明集合内（不许发明页）。
	 */
	@Test
	void nativeEntryPagesStayInsideTheClientDeclaredPageSet() throws Exception {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		Map<Integer, List<Integer>> pages = contractPages();
		assertFalse(pages.isEmpty(), "客户端页契约不得为空");

		List<Integer> invented = new ArrayList<>();
		int fallbackRows = 0;
		for (Map.Entry<Integer, List<Integer>> entry : pages.entrySet()) {
			int questId = entry.getKey();
			List<Integer> declared = entry.getValue();
			int expected = declared.contains(SELECT_NONE) ? SELECT_NONE
				: declared.contains(SELECT1) ? SELECT1
				: declared.contains(ASK_WINDOW) ? ASK_WINDOW : ASK_WINDOW;
			int actual = contract.retailEntryPage(questId);
			assertEquals(expected, actual, "原版入口页偏好序（信页 → 阶段页 → 页 4 兜底）: " + questId);
			if (!declared.contains(actual)) {
				// 兜底：客户端三类页都未声明 ⇒ 仍回原版接取窗页 4（不发明新页号）。
				if (declared.contains(SELECT_NONE) || declared.contains(SELECT1) || declared.contains(ASK_WINDOW)) {
					invented.add(questId);
				} else {
					assertEquals(ASK_WINDOW, actual, "无页登记的兜底只能回页 4: " + questId);
					fallbackRows++;
				}
			}
		}
		assertTrue(invented.isEmpty(), () -> "入口页不得落在客户端未声明的页上: " + invented);
				assertTrue(fallbackRows > 0, "客户端三类页全缺的兜底行必须存在（否则规则退化）: " + fallbackRows);
	}

	/** 具体样本：信页优先与页 4 兜底两条形态各取一行冻结。 */
	@Test
	void entryPageSamplesMatchTheRetailRule() {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		assertEquals(SELECT1, contract.retailEntryPage(3016), "3016 客户端声明 select1 ⇒ 入口 = 1011");
		assertEquals(SELECT_NONE, contract.retailEntryPage(1430), "1430 声明 select_none ⇒ 入口 = 4762");
		assertEquals(ASK_WINDOW, contract.retailEntryPage(5000), "5000 无客户端页登记 ⇒ 兜底页 4");
		assertEquals(ASK_WINDOW, contract.askWindowPage(3016), "3016 声明 ask_quest_accept ⇒ 1007 可开页 4");
		assertEquals(-1, contract.askWindowPage(NO_ASK_WINDOW_QUEST), "未声明 ask_quest_accept ⇒ fail-closed");
	}

	/**
	 * 过场行可达性：5 行（hunt 3016/4007/4014 + talk 3020/4056）声明 {@code cs1_haction = 1007}，
	 * 客户端都声明页 4 ⇒ 在接取 NPC 处下发 {@code 1007} 必须被本行服务（下发页 4），过场随之可达。
	 */
	@Test
	void cutsceneRowsBecomeReachableThroughTheAskAction() throws Exception {
		Map<Integer, String> acquires = acquireNpcs();
		SimpleHuntHandler hunt = SimpleHuntHandler.instance();
		SimpleTalkHandler talk = SimpleTalkHandler.instance();
		for (int questId : ASK_CUTSCENE_ROWS.keySet()) {
			Player player = NativeTalkFixture.player();
			String npcName = acquires.get(questId);
			assertEquals(SELECT1, NativeTalkFixture.clientEntryPage(questId),
				"过场行的入口页 = 客户端信页 select1: " + questId);
			assertEquals(ASK_WINDOW, NativeTalkFixture.askWindowPage(questId),
				"过场行的客户端任务页声明 ask_quest_accept: " + questId);
			int npcId = npcIdOf(hunt, talk, questId, npcName);
			NativeTalkFixture.clearPackets(player);
			boolean handled = hunt.owns(questId)
				? hunt.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1007))
				: talk.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1007));
			assertTrue(handled, "页动作 1007（ASK_QUEST_ACCEPT）必须由本行服务: " + questId);
			NativeTalkFixture.assertOnlyDialogPage(player, ASK_WINDOW);
		}
	}

	/** 未声明页 4 的行：{@code 1007} 不得被服务（无包下发，禁止发明页）。 */
	@Test
	void askActionFailsClosedWithoutAClientAskWindowPage() {
		SimpleHuntHandler hunt = SimpleHuntHandler.instance();
		assertTrue(hunt.routes(NO_ASK_WINDOW_QUEST), "样例行必须由本族路由: " + NO_ASK_WINDOW_QUEST);
		int npcId = hunt.acquireNpc(NO_ASK_WINDOW_QUEST);
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.clearPackets(player);
		assertFalse(hunt.onDialog(NativeTalkFixture.dialog(player, npcId, NO_ASK_WINDOW_QUEST, 1007)),
			"未声明 ask_quest_accept ⇒ 1007 不服务");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "不服务的动作不得下发任何页");
		assertEquals(SELECT1, NativeTalkFixture.clientEntryPage(NO_ASK_WINDOW_QUEST),
			"该行入口仍是客户端信页 select1");
	}

	// ---------------------------------------------------------------- 事实源

	/** 客户端页契约：quest id → 声明页集合（直接读资源，不复用生产解析路径）。 */
	private static Map<Integer, List<Integer>> contractPages() throws Exception {
		Map<Integer, List<Integer>> pages = new LinkedHashMap<>();
		for (String line : readResource(CONTRACT_RESOURCE).split("\n")) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			String[] cells = line.split("\t");
			pages.computeIfAbsent(Integer.parseInt(cells[0]), key -> new ArrayList<>())
				.add(Integer.parseInt(cells[1]));
		}
		return pages;
	}

	/** 原版表行的接取 NPC 名（独立重解析）。 / The retail rows' acquire npc names (independent re-parse). */
	private static Map<Integer, String> acquireNpcs() throws Exception {
		Map<Integer, String> acquires = new LinkedHashMap<>();
		for (String resource : List.of(HUNT_RESOURCE, TALK_RESOURCE)) {
			String text = readResource(resource);
			for (String block : text.split("<id id=\"")) {
				int quote = block.indexOf('"');
				if (quote <= 0) {
					continue;
				}
				String id = block.substring(0, quote);
				if (!id.chars().allMatch(Character::isDigit)) {
					continue;
				}
				String npc = cell(block, "acquired_npc_name");
				if (npc != null) {
					acquires.put(Integer.parseInt(id), npc);
				}
			}
		}
		return acquires;
	}

	private static String cell(String block, String tag) {
		String open = "<" + tag + ">";
		int start = block.indexOf(open);
		if (start < 0) {
			return null;
		}
		int end = block.indexOf("</" + tag + ">", start);
		return end < 0 ? null : block.substring(start + open.length(), end).trim();
	}

	private static int npcIdOf(SimpleHuntHandler hunt, SimpleTalkHandler talk, int questId, String npcName)
			throws Exception {
		int npcId = hunt.owns(questId) ? hunt.acquireNpc(questId) : talk.acquireNpc(questId);
		assertTrue(npcId > 0, "原版行必须有可解析的接取 NPC: " + questId + " (" + npcName + ")");
		return npcId;
	}

	private static String readResource(String path) throws Exception {
		try (InputStream input = NativeAcceptEntryAskFlowGateTest.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
