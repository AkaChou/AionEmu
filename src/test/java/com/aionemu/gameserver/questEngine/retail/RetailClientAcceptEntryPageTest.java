package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接取入口页客户端契约门禁（{@link RetailClientAcceptEntryPage}）。
 * <p>
 * 判据：接取入口边（未接态 {@code QUEST_SELECT(31)} / {@code USE_ITEM}，以及重复任务的再开局别名）
 * 下发的页必须落在任务页 HTML 实际声明的页里；发客户端没有的页（例如只有 select_none 的行发真端
 * 接取窗页 4）会让客户端直接 {@code load fail}。80787 家族即此形（任务页只有 select_none(4762)）。
 * <p>
 * 断言：① 入口页选择规则逐 id 落页；② 1144 的 select1→select1_1 续页必须由服务端路由；
 * ③ 生产视图（真端 overlay，含修复）里每个真端 owner 行下发的接取入口页都在客户端任务页里 ——
 * 只有任务页连 4/4762/1011 都没有的行留在 {@code /quest/retail-accept-entry-page-gaps.tsv}
 * 冻结缺口表里（无据可改；新增或消失都必须显式改表，重生成用
 * {@code -Dretail.acceptEntryPage.gapOut=...}）；④ select1 首屏的客户端声明续页梯不得缺路由。
 * <p>
 * Gate for the accept entry page (see {@link RetailClientAcceptEntryPage}): the page popped by an
 * accept entry edge must be one the quest's task HTML declares, otherwise the client reports
 * {@code load fail} (the 80787 family declares select_none(4762) only). Only rows whose task HTML
 * declares none of 4/4762/1011 stay in the frozen gap baseline; a select1 entry must also route its
 * client-declared continuation ladder.
 */
class RetailClientAcceptEntryPageTest {

	/** 1144 的真端接取 NPC（Eradis）。 / The retail acquire NPC for quest 1144 (Eradis). */
	private static final int QUEST_1144_ACQUIRE_NPC = 203130;
	/**
	 * select1→1012/1013 可对话入口边冻结下限：静态契约命中的 56 行里，非 NPC 31 接取入口
	 * （系统/事件/自动发放等）不适用本梯。 / Frozen floor for dialog-entry select1 ladders; non-NPC-31
	 * acquisitions from the 56 static contract rows do not use this ladder.
	 */
	private static final int SELECT1_LADDER_COVERAGE_FLOOR = 34;
	/** 真端 owner 登记（owner=RETAIL_TABLE 的行由真端驱动，overlay 会替换 XML 与页）。 /
	 * Retail ownership registry (RETAIL_TABLE rows are retail-driven, so the overlay owns their pages). */
	private static final String RETENTION_REGISTRY =
		"/aion/data/static_data/quest/retail/retail-xml-retention.tsv";
	/**
	 * 冻结缺口表（重生成：{@code -Dretail.acceptEntryPage.gapOut=<path>}）。当前实测缺口 = 583 行：
	 * 全部是客户端页索引**完全缺登记**的真端 owner 行（无据判定，保持合成器页）；有页索引的行必须
	 * 下发客户端声明页，一行都不许留缺口。
	 * Frozen gap baseline. The measured gaps are the 583 retail-owned rows whose quest is missing from
	 * the client page index entirely (no evidence to follow); every row with a page index must emit a
	 * page the client declares.
	 */
	private static final String GAP_BASELINE = "/quest/retail-accept-entry-page-gaps.tsv";
	/**
	 * 自有但 fail-closed 的用物接取行（真端表行已退役，但声明的交付名 {@code magician_apprentice}
	 * 在客户端/名称索引里无唯一解，没有驱动定义可判入口页）。新增或消失都必须显式改本表。
	 * Owned-but-fail-closed item-use rows: retired retail rows whose declared hand-in name has no
	 * unique resolution, so there is no driver definition and no entry page to judge. The list is
	 * asserted in both directions so new or vanished rows must be an explicit edit.
	 */
	private static final Set<Integer> FAIL_CLOSED_NATIVE_ROWS = Set.of(30720, 30723);
	/** 已切换到原生车道的家族处理器。 / The families already switched to the native lane. */
	private static final SimpleTalkHandler TALK = NativeTalkFixture.handler();
	private static final SimpleHuntHandler HUNT = SimpleHuntHandler.instance();
	private static final SimpleSerialHuntHandler SERIAL = SimpleSerialHuntHandler.instance();
	private static final SimpleCollectItemHandler COLLECT = SimpleCollectItemHandler.instance();
	private static final SimpleUseItemHandler USE = SimpleUseItemHandler.instance();
	private static final SimpleItemPlayHandler PLAY = SimpleItemPlayHandler.instance();

	@Test
	void entryPageFollowsTheClientTaskPage() {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		// 80787 家族任务页只有 select_none(4762)。 / The 80787 family declares select_none(4762) only.
		assertEquals(QuestDialogPage.SELECT_NONE.id(),
			RetailClientAcceptEntryPage.entryPage(80787, contract));
		// 80324 声明了 ask_quest_accept(4) → 保持真端接取窗页。
		// 80324 declares ask_quest_accept(4) → the native ask window page stays.
		assertEquals(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
			RetailClientAcceptEntryPage.entryPage(80324, contract));
		// 无客户端页登记时保持合成器既有页。 / Unregistered quests keep the synthesizer's page.
		assertEquals(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
			RetailClientAcceptEntryPage.entryPage(999999, contract));
		assertTrue(contract.hasButtonPage(80787, QuestDialogPage.SELECT_NONE.id()),
			"client page registry must declare select_none for 80787");
	}

	@Test
	void select1EntryRegistersTheClientAcceptLadder() {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		assertTrue(contract.hasButtonPage(1144, RetailClientAcceptEntryPage.SELECT1_PAGE),
			"1144 must declare select1");
		assertTrue(contract.hasButtonPage(1144, QuestDialogPage.SELECT1_1.id()),
			"1144 must declare the select1 continuation");
		if (nativeLane(1144) != null) {
			// 1144 已切到原生车道：1011 首屏的 1012/1013 翻页由处理器原样回发（cab520 语义），
			// 客户端未声明的续页必须零路由（与 typed 分支同形断言）。
			// 1144 runs on the native lane now: the handler echoes the client-declared 1012/1013 page
			// turns on the 1011 entry screen, and undeclared continuations stay unanswered.
			int acquireNpc = nativeAcquireNpc(1144);
			assertTrue(nativeServesPage(1144, acquireNpc, QuestDialogPage.SELECT1_1.id()),
				"1144's 1011->1012 page turn must be served by the native lane");
			assertEquals(contract.hasButtonPage(1144, QuestDialogPage.SELECT1_1_1.id()),
				nativeServesPage(1144, acquireNpc, QuestDialogPage.SELECT1_1_1.id()),
				"1144 must not invent a page beyond the client contract");
			return;
		}
		CompiledQuestDefinition compiled = ProductionQuestDefinitions.definition(1144);
		assertTrue(continuationRoute(compiled.definition(), QUEST_1144_ACQUIRE_NPC, QuestDialogAction.SELECT1_1.id(),
				QuestDialogPage.SELECT1_1.id(), "unaccepted").isPresent(),
			"1144's 1011->1012 page turn must be server-routed");
		assertEquals(contract.hasButtonPage(1144, QuestDialogPage.SELECT1_1_1.id()),
			continuationRoute(compiled.definition(), QUEST_1144_ACQUIRE_NPC,
				QuestDialogAction.SELECT1_1_1.id(), QuestDialogPage.SELECT1_1_1.id(),
				"unaccepted").isPresent(),
			"1144 must not invent a page beyond the client contract");
	}

	@Test
	void everySelect1EntryFollowsTheClientAcceptLadder() throws Exception {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		List<String> missing = new ArrayList<>();
		Set<Integer> skipped = new TreeSet<>();
		int checked = 0;
		for (int questId : retailOwnedQuestIds()) {
			if (nativeLane(questId) != null) {
				// 原生车道：入口页 = 客户端声明页；select1(1011) 首屏的翻页动作由 native 处理器原样回发。
				// Native lane: the entry page is the client-declared page; select1 page turns are echoed.
				if (!nativeRoutes(questId)) {
					skipped.add(questId);
					continue;
				}
				Integer acquireNpc = nativeAcquireNpc(questId);
				if (acquireNpc == null || contract.acceptEntryPage(questId) != RetailClientAcceptEntryPage.SELECT1_PAGE) {
					continue;
				}
				for (int pageId : List.of(QuestDialogPage.SELECT1_1.id(), QuestDialogPage.SELECT1_1_1.id())) {
					if (!contract.hasButtonPage(questId, pageId)) {
						continue;
					}
					checked++;
					if (!nativeServesPage(questId, acquireNpc, pageId)) {
						missing.add(questId + "\t" + pageId);
					}
				}
				continue;
			}
			QuestDefinition definition = ProductionQuestDefinitions.definition(questId).definition();
			for (QuestTransition entry : acceptEntryEdges(definition)) {
				if (shownPage(entry) != RetailClientAcceptEntryPage.SELECT1_PAGE
						|| !(entry.event() instanceof QuestEvent.TalkToNpc talk)) {
					continue;
				}
				for (int pageId : List.of(QuestDialogPage.SELECT1_1.id(), QuestDialogPage.SELECT1_1_1.id())) {
					if (!contract.hasButtonPage(questId, pageId)) {
						continue;
					}
					checked++;
					if (continuationRoute(definition, talk.npcId(), pageId, pageId, entry.sourceNode())
							.isEmpty()) {
						missing.add(questId + "\t" + pageId);
					}
				}
			}
		}
		int coverage = checked;
		assertTrue(coverage >= SELECT1_LADDER_COVERAGE_FLOOR,
			() -> "select1 continuation coverage too small: " + coverage);
		assertTrue(missing.isEmpty(), () -> "select1 accept ladder missing routes: " + missing);
		assertEquals(FAIL_CLOSED_NATIVE_ROWS, skipped,
			"fail-closed 原生行集合漂移 / fail-closed native rows drifted");
	}

	/**
	 * 原生车道是否服务该 select1 翻页动作：假玩家 + 真端接取 NPC 驱动处理器，断言原样回发该页。
	 * Whether the native lane serves the select1 page turn: drive the handler with a fake player on the
	 * retail acquire NPC and assert the page is echoed.
	 */
	private static boolean nativeServesPage(int questId, int acquireNpc, int pageId) {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.clearPackets(player);
		QuestEnv env = NativeTalkFixture.dialog(player, acquireNpc, questId, pageId);
		boolean handled = TALK.routes(questId) ? TALK.onDialog(env)
			: HUNT.routes(questId) ? HUNT.onDialog(env)
			: SERIAL.routes(questId) ? SERIAL.onDialog(env)
			: COLLECT.routes(questId) ? COLLECT.onDialog(env) : PLAY.routes(questId) && PLAY.onDialog(env);
		return handled && NativeTalkFixture.dialogPages(player).equals(List.of(pageId));
	}

	/** 已切到原生车道的家族名（Talk / Hunt / SerialHunt / CollectItem / UseItem / ItemPlay）；未切换返回 null。 /
	 * The native family owning the row, or null when the row still runs on the typed IR lane. */
	private static String nativeLane(int questId) {
		if (TALK.owns(questId)) {
			return "SimpleTalk";
		}
		if (HUNT.owns(questId)) {
			return "SimpleHunt";
		}
		if (SERIAL.owns(questId)) {
			return "SimpleSerialHunt";
		}
		if (COLLECT.owns(questId)) {
			return "SimpleCollectItem";
		}
		if (USE.owns(questId)) {
			return "SimpleUseItem";
		}
		if (PLAY.owns(questId)) {
			return "SimpleItemPlay";
		}
		return null;
	}

	/** 原生车道是否有该行的驱动定义（own 但未 route 的行是 fail-closed 冻结行）。 /
	 * Whether any native family routes the row (owned but unrouted rows are the fail-closed freeze). */
	private static boolean nativeRoutes(int questId) {
		return TALK.routes(questId) || HUNT.routes(questId) || SERIAL.routes(questId)
			|| COLLECT.routes(questId) || USE.routes(questId) || PLAY.routes(questId);
	}

	/** 原生家族声明的接取 NPC（用物接取族无 NPC 入口，返回 null）。 /
	 * The acquire NPC the native family declares; the item-use family has no NPC entry. */
	private static Integer nativeAcquireNpc(int questId) {
		if (TALK.routes(questId)) {
			return TALK.acquireNpc(questId);
		}
		if (HUNT.routes(questId)) {
			return HUNT.acquireNpc(questId);
		}
		if (SERIAL.routes(questId)) {
			return SERIAL.acquireNpc(questId);
		}
		if (COLLECT.routes(questId)) {
			return COLLECT.acquireNpc(questId);
		}
		return PLAY.routes(questId) ? PLAY.acquireNpc(questId) : null;
	}

	/** 同 NPC/动作/来源且确实下发客户端声明页的续页路由。 / A same-owner page-turn route showing the page. */
	private static Optional<QuestTransition> continuationRoute(QuestDefinition definition, int npcId,
			int actionId, int pageId, String sourceNode) {
		return definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Objects.equals(talk.dialogId(), actionId)
				&& Objects.equals(transition.sourceNode(), sourceNode)
				&& transition.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(pageId)))
			.findFirst();
	}

	@Test
	void everyRetailOwnedAcceptEntryPageIsClientLoadable() throws Exception {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		List<String> gaps = new ArrayList<>();
		Set<Integer> skipped = new TreeSet<>();
		int checked = 0;
		for (int questId : retailOwnedQuestIds()) {
			if (nativeLane(questId) != null) {
				// 原生车道：接取入口 = 接取 NPC 的 QUEST_SELECT → 客户端契约页；无 NPC 接取入口的行
				// （系统/事件发放、接取名未解）没有入口页可判。
				// Native lane: the entry is the acquire NPC's QUEST_SELECT → the client contract page.
				if (!nativeRoutes(questId)) {
					skipped.add(questId);
					continue;
				}
				Integer acquireNpc = nativeAcquireNpc(questId);
				if (acquireNpc == null) {
					if (USE.routes(questId)) {
						// 用物接取族：入口页由 UseItem 事件下发（真端无主接取形），同样必须客户端可渲染。
						// Item-use family: the entry page is popped by the UseItem event, and must be
						// client-renderable just the same.
						checked++;
						int usePage = SimpleUseItemHandler.PAGE_ASK_ACCEPT;
						if (!contract.hasButtonPage(questId, usePage)) {
							gaps.add(questId + "\t" + usePage);
						}
					}
					continue;
				}
				checked++;
				int page = contract.acceptEntryPage(questId);
				if (!contract.hasButtonPage(questId, page)) {
					gaps.add(questId + "\t" + page);
				}
				continue;
			}
			CompiledQuestDefinition definition = ProductionQuestDefinitions.definition(questId);
			for (QuestTransition transition : acceptEntryEdges(definition.definition())) {
				checked++;
				int page = shownPage(transition);
				if (!contract.hasButtonPage(questId, page)) {
					gaps.add(questId + "\t" + page);
				}
			}
		}
		List<String> observed = new ArrayList<>(new TreeSet<>(gaps));
		String freezeOut = System.getProperty("retail.acceptEntryPage.gapOut");
		if (freezeOut != null) {
			Files.writeString(Path.of(freezeOut), gapBaselineText(observed));
		}
		List<String> frozen = loadGapBaseline();
		assertEquals(frozen, observed,
			() -> "接取入口页与客户端任务页失同步 / accept entry pages out of sync with the client task HTML: "
				+ "新增=" + difference(observed, frozen) + " 需删登记=" + difference(frozen, observed));
		assertEquals(FAIL_CLOSED_NATIVE_ROWS, skipped,
			"fail-closed 原生行集合漂移 / fail-closed native rows drifted");
		int edgeCount = checked;
		assertTrue(edgeCount > 1500,
			() -> "接取入口边覆盖过少，门禁失效 / accept-entry-edge coverage too small: " + edgeCount);
	}

	private static String gapBaselineText(List<String> gaps) {
		StringBuilder text = new StringBuilder("# 接取入口页冻结缺口（真端 owner 行）：客户端任务页既无 ask_quest_accept(4)、\n"
			+ "# 也无 select_none(4762)/select1(1011)，入口页没有可依的客户端证据，保持合成器页。\n"
			+ "# 重生成：-Dretail.acceptEntryPage.gapOut=<path>\n"
			+ "# quest_id\tentry_page\n");
		for (String gap : gaps) {
			text.append(gap).append('\n');
		}
		return text.toString();
	}

	private static List<String> loadGapBaseline() throws Exception {
		InputStream input = RetailClientAcceptEntryPageTest.class.getResourceAsStream(GAP_BASELINE);
		assertNotNull(input, "missing gap baseline resource " + GAP_BASELINE);
		List<String> rows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				rows.add(line);
			}
		}
		return rows;
	}

	private static List<String> difference(List<String> left, List<String> right) {
		List<String> result = new ArrayList<>(left);
		result.removeAll(new HashSet<>(right));
		return result;
	}

	/**
	 * 接取入口边：未接态（NONE 投影）的接取触发边，或重复任务的再开局别名（COMPLETE 投影 +
	 * StartEligible，编译期从同一条未接态边复制）。
	 * Accept entry edges: NONE-source accept-trigger edges plus the repeat quest's reopen alias
	 * (COMPLETE source carrying StartEligible).
	 */
	private static List<QuestTransition> acceptEntryEdges(QuestDefinition definition) {
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		List<QuestTransition> edges = new ArrayList<>();
		for (QuestTransition transition : definition.transitions()) {
			boolean entry = statuses.get(transition.sourceNode()) == QuestStatus.NONE
				|| statuses.get(transition.sourceNode()) == QuestStatus.COMPLETE
					&& transition.conditions().stream()
						.anyMatch(QuestCondition.StartEligible.class::isInstance);
			if (entry && isAcceptTrigger(transition.event())) {
				edges.add(transition);
			}
		}
		return edges;
	}

	/** 接取触发：NPC 任务列表选择（31）或使用任务起始道具。 /
	 * The accept trigger: the npc quest-list selection (31) or the quest-start item use. */
	private static boolean isAcceptTrigger(QuestEvent event) {
		return event instanceof QuestEvent.UseItem
			|| event instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id();
	}

	private static int shownPage(QuestTransition transition) {
		return transition.afterCommit().stream()
			.filter(AfterCommitAction.ShowQuestDialog.class::isInstance)
			.map(AfterCommitAction.ShowQuestDialog.class::cast)
			.mapToInt(AfterCommitAction.ShowQuestDialog::dialogId)
			.findFirst()
			.orElse(0);
	}

	/** 真端 owner 行（owner=RETAIL_TABLE）。 / The retail-owned rows (owner=RETAIL_TABLE). */
	private static List<Integer> retailOwnedQuestIds() throws Exception {
		InputStream input = RetailClientAcceptEntryPageTest.class.getResourceAsStream(RETENTION_REGISTRY);
		assertNotNull(input, "missing retention registry " + RETENTION_REGISTRY);
		Set<Integer> owned = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length >= 2 && "RETAIL_TABLE".equals(parts[1])) {
					owned.add(Integer.parseInt(parts[0]));
				}
			}
		}
		List<Integer> ids = new ArrayList<>(owned);
		ids.sort(Integer::compareTo);
		return ids;
	}
}
