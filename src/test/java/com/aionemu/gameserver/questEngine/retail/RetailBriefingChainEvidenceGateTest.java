package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 常设证据门：简报链覆盖不变量（W5-g4 起由本门承担）。
 * <p>
 * 背景：{@code quest_client_briefing_chains.tsv}（3225 行）随 W5-g4 退役——生产侧不再读它，两处受理门
 * 子句（{@code RETAIL_BRIEFING_CHAIN_MISSING} / {@code RETAIL_BRIEFING_TERMINAL_UNEXPECTED} /
 * {@code RETAIL_BRIEFING_CHAIN_UNREGISTERED}）一并删除。原门保护的**结构不变量**在此冻结为构建期断言：
 * 真端模板表里每个非空 {@code talk_npc1} 行（消费该列的两族 = SimpleHunt / SimpleCollectItem）都必须有
 * 一条客户端登记链，且链末跳是 SETPRO 家族终点（{@code pageId=0}，清 SECTION_5 标志位并关窗）。数据修订
 * 若新增简报行而客户端没有对应链，本门立刻变红——生产侧的 fail-closed 防线移到构建期。
 * <p>
 * Permanent evidence gate carrying the briefing-chain coverage invariant after the registry retired in
 * W5-g4: every non-blank {@code talk_npc1} row of the two consuming families must have a frozen client
 * chain whose terminal hop is a SETPRO flag-clearing action with {@code pageId=0}. Cardinalities are
 * frozen too, so dropping a briefing row from the retail table also fails until reviewed.
 */
class RetailBriefingChainEvidenceGateTest {

	/** 冻结登记（原 {@code quest_client_briefing_chains.tsv} 的只读快照）。 / Frozen registry snapshot. */
	private static final Path FROZEN_REGISTRY =
		Path.of("src/test/resources/quest/retail-client-briefing-chains.tsv");
	/** 真端模板表目录。 / The retail template-table directory. */
	private static final Path RETAIL_DIR = Path.of("src/main/resources/aion/data/static_data/quest_retail");
	/** 冻结基数：登记行数 / SimpleHunt 简报行数 / SimpleCollectItem 简报行数。 / Frozen cardinalities. */
	private static final int FROZEN_REGISTRY_SIZE = 3225;
	private static final int HUNT_BRIEFING_ROWS = 47;
	private static final int COLLECT_BRIEFING_ROWS = 5;
	/** 真端模板行块与简报列。 / Template row blocks and the briefing column. */
	private static final Pattern ROW = Pattern.compile("<id id=\"(\\d+)\">(.*?)</id>", Pattern.DOTALL);
	private static final Pattern TALK_NPC = Pattern.compile("<talk_npc1>(.*?)</talk_npc1>", Pattern.DOTALL);

	@Test
	void everyTalkNpcRowCarriesASetproTerminalChainInTheFrozenRegistry() throws Exception {
		Map<Integer, String> registry = loadRegistry();
		assertEquals(FROZEN_REGISTRY_SIZE, registry.size(),
			() -> "冻结登记行数漂移（登记表被改动？）：" + registry.size());
		List<String> problems = new ArrayList<>();
		int hunt = audit(registry, "Quest_SimpleHunt.xml", problems);
		int collect = audit(registry, "Quest_SimpleCollectItem.xml", problems);
		assertEquals(HUNT_BRIEFING_ROWS, hunt, () -> "SimpleHunt 简报行数漂移：" + hunt);
		assertEquals(COLLECT_BRIEFING_ROWS, collect,
			() -> "SimpleCollectItem 简报行数漂移：" + collect);
		assertTrue(problems.isEmpty(), () -> "简报链覆盖不变量被破坏：" + problems);
	}

	/** 逐行审计一族的简报行：登记存在 + 末跳为 SETPRO 终点。 / Audits one family; returns the row count. */
	private static int audit(Map<Integer, String> registry, String tableName, List<String> problems)
			throws IOException {
		String text = Files.readString(RETAIL_DIR.resolve(tableName), StandardCharsets.UTF_8);
		Matcher rows = ROW.matcher(text);
		int count = 0;
		while (rows.find()) {
			int questId = Integer.parseInt(rows.group(1));
			Matcher talk = TALK_NPC.matcher(rows.group(2));
			if (!talk.find() || talk.group(1).isBlank()) {
				continue;
			}
			count++;
			String chain = registry.get(questId);
			if (chain == null) {
				problems.add(tableName + ":" + questId + " 无冻结登记");
				continue;
			}
			String[] hops = chain.trim().split("\\s+");
			String[] terminal = hops[hops.length - 1].split(":");
			String action;
			try {
				action = QuestDialogAction.fromId(Integer.parseInt(terminal[0])).name();
			} catch (RuntimeException unknownAction) {
				action = "unknown:" + terminal[0];
			}
			if (terminal.length != 2 || !"0".equals(terminal[1]) || !action.startsWith("SETPRO")) {
				problems.add(tableName + ":" + questId + " 末跳非 SETPRO 终点：" + hops[hops.length - 1]);
			}
		}
		return count;
	}

	/** 冻结登记装载（quest_id → hops 串）。 / Loads the frozen registry. */
	private static Map<Integer, String> loadRegistry() throws IOException {
		Map<Integer, String> chains = new LinkedHashMap<>();
		for (String line : Files.readAllLines(FROZEN_REGISTRY, StandardCharsets.UTF_8)) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			assertEquals(3, parts.length, () -> "冻结登记列数异常：" + line);
			chains.put(Integer.parseInt(parts[0]), parts[2]);
		}
		return chains;
	}
}
