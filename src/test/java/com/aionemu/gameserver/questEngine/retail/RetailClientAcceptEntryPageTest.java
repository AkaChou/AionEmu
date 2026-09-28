package com.aionemu.gameserver.questEngine.retail;

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
 * 两条断言：① 入口页选择规则逐 id 落页；② 生产视图（真端 overlay，含修复）里每个真端 owner 行
 * 下发的接取入口页都在客户端任务页里 —— 只有任务页连 4/4762/1011 都没有的行留在
 * {@code /quest/retail-accept-entry-page-gaps.tsv} 冻结缺口表里（无据可改；新增或消失都必须
 * 显式改表，重生成用 {@code -Dretail.acceptEntryPage.gapOut=...}）。
 * <p>
 * Gate for the accept entry page (see {@link RetailClientAcceptEntryPage}): the page popped by an
 * accept entry edge must be one the quest's task HTML declares, otherwise the client reports
 * {@code load fail} (the 80787 family declares select_none(4762) only). Only rows whose task HTML
 * declares none of 4/4762/1011 stay in the frozen gap baseline.
 */
class RetailClientAcceptEntryPageTest {

	/** 真端 owner 登记（owner=RETAIL_TABLE 的行由真端驱动，overlay 会替换 XML 与页）。 /
	 * Retail ownership registry (RETAIL_TABLE rows are retail-driven, so the overlay owns their pages). */
	private static final String RETENTION_REGISTRY =
		"/aion/data/static_data/quest_retail/retail-xml-retention.tsv";
	/**
	 * 冻结缺口表（重生成：{@code -Dretail.acceptEntryPage.gapOut=<path>}）。当前实测缺口 = 583 行：
	 * 全部是客户端页索引**完全缺登记**的真端 owner 行（无据判定，保持合成器页）；有页索引的行必须
	 * 下发客户端声明页，一行都不许留缺口。
	 * Frozen gap baseline. The measured gaps are the 583 retail-owned rows whose quest is missing from
	 * the client page index entirely (no evidence to follow); every row with a page index must emit a
	 * page the client declares.
	 */
	private static final String GAP_BASELINE = "/quest/retail-accept-entry-page-gaps.tsv";

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
	void everyRetailOwnedAcceptEntryPageIsClientLoadable() throws Exception {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		List<String> gaps = new ArrayList<>();
		int checked = 0;
		for (int questId : retailOwnedQuestIds()) {
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
