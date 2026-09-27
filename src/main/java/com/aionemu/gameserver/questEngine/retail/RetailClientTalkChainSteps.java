package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SimpleTalk wave A 链式路由登记（quest_client_talk_chain_steps.tsv，生成器
 * {@code build_quest_client_talk_chain_steps.py}）只读视图。
 * <p>
 * 四类记录：N 节点（label/status/var0 逐字转写）、R TalkToNpc 路由（conditions/actions/
 * after-commit 逐字编码，页面经客户端 CSV 交叉核验，NPC 绑定按真端表权威留证）、
 * B 规范块参数（NPC_START/NPC_REPORT 的 npc/source/target/extra；npc-complete 由编译器
 * 按元数据规范合成，不转写）、I item_check 门（{@code npc-item-report} 糖元素的逐字转写，
 * 编译器展开成 39/20002 各成功/失败四条路由，判例 QE-059）。编译器只读本表，不读退役 XML。
 * <p>
 * Read-only view of the SimpleTalk wave-A chain registry; the chain compiler replays it.
 */
public final class RetailClientTalkChainSteps {

	/** N 记录：节点投影（var0 可空 = varless 变体节点，回放空 projection）。 / Node record. */
	public record NodeRecord(String label, String status, Integer var0) {
	}

	/** R/C 记录：TALK=TalkToNpc 事件；CAN_ACT=CanAct 事件（npcId 列为 templateId）。 */
	public record RouteRecord(String eventType, int seq, int npcId, String action, String source,
			String target, String conditions, String actions, String afterCommits, String npcCheck,
			String priority) {
	}

	/** B 记录：规范块参数。 / Canonical block parameters. */
	public record BlockRecord(String block, int npcId, String source, String target, String extra) {
	}

	/**
	 * I 记录：item_check 门（编译器级糖元素 {@code npc-item-report} 的逐字转写）。{@code removeCount}
	 * 为原始 token（{@code -} = 取 required，{@code ALL} = 整堆栈，数字 = 必须等于 required）；
	 * {@code failurePage} 为原始符号（{@code -} = 缺省 SELECT6，{@code CLOSE} = 关窗，其余 =
	 * {@code QuestDialogPage} 常量名）。
	 * Item-check gate record transcribing the compiler-level {@code npc-item-report} sugar.
	 */
	public record ItemReportRecord(int npcId, String source, String target, int itemId, int required,
			String removeCount, String failurePage) {
	}

	private static final String REGISTRY =
		"/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv";

	/** P 记录：进度布局（offset/width/min/max/persistence/scope 逐字转写）。 */
	public record LayoutRecord(int offset, int width, int min, int max, String persistence,
			String scope) {
	}

	private final Map<Integer, List<NodeRecord>> nodes;
	private final Map<Integer, List<RouteRecord>> routes;
	private final Map<Integer, List<BlockRecord>> blocks;
	private final Map<Integer, LayoutRecord> layouts;
	private final Map<Integer, List<ItemReportRecord>> itemReports;

	private RetailClientTalkChainSteps(Map<Integer, List<NodeRecord>> nodes,
			Map<Integer, List<RouteRecord>> routes, Map<Integer, List<BlockRecord>> blocks,
			Map<Integer, LayoutRecord> layouts, Map<Integer, List<ItemReportRecord>> itemReports) {
		this.nodes = nodes;
		this.routes = routes;
		this.blocks = blocks;
		this.layouts = layouts;
		this.itemReports = itemReports;
	}

	public Optional<LayoutRecord> layout(int questId) {
		return Optional.ofNullable(layouts.get(questId));
	}

	public boolean has(int questId) {
		return routes.containsKey(questId);
	}

	public List<NodeRecord> nodes(int questId) {
		return nodes.getOrDefault(questId, List.of());
	}

	public List<RouteRecord> routes(int questId) {
		return routes.getOrDefault(questId, List.of());
	}

	public Optional<BlockRecord> block(int questId, String kind) {
		return blocks.getOrDefault(questId, List.of()).stream()
			.filter(record -> record.block().equals(kind))
			.findFirst();
	}

	/** 同类全部规范块（多变体任务可有多个 NPC_START/NPC_REPORT）。 */
	public java.util.List<BlockRecord> blocks(int questId, String kind) {
		return blocks.getOrDefault(questId, List.of()).stream()
			.filter(record -> record.block().equals(kind))
			.toList();
	}

	/** I 记录：item_check 门（无记录 = 该行没有检查按钮门）。 / Item-check gates, empty when none. */
	public List<ItemReportRecord> itemReports(int questId) {
		return itemReports.getOrDefault(questId, List.of());
	}

	public int questCount() {
		return routes.size();
	}

	public java.util.Set<Integer> questIds() {
		return routes.keySet();
	}

	public static RetailClientTalkChainSteps load() throws IOException {
		try (InputStream input = RetailClientTalkChainSteps.class.getResourceAsStream(REGISTRY)) {
			if (input == null) {
				throw new IOException("missing registry " + REGISTRY);
			}
			return load(input);
		}
	}

	/** priority 列容错读取（旧快照无该列时为 '-'）。 / Tolerant priority column read. */
	private static String routePriority(String[] p, int index) {
		return index < p.length ? p[index] : "-";
	}

	public static RetailClientTalkChainSteps load(InputStream input) throws IOException {
		Map<Integer, List<NodeRecord>> nodes = new HashMap<>();
		Map<Integer, List<RouteRecord>> routes = new HashMap<>();
		Map<Integer, List<BlockRecord>> blocks = new HashMap<>();
		Map<Integer, LayoutRecord> layouts = new HashMap<>();
		Map<Integer, List<ItemReportRecord>> itemReports = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			List<String> lines = reader.lines().toList();
			for (String line : lines) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] p = line.split("\t", -1);
				int questId = Integer.parseInt(p[0]);
				switch (p[1]) {
					case "N" -> nodes.computeIfAbsent(questId, key -> new java.util.ArrayList<>())
						.add(new NodeRecord(p[2], p[3], "-".equals(p[4]) ? null : Integer.parseInt(p[4])));
					case "R" -> routes.computeIfAbsent(questId, key -> new java.util.ArrayList<>())
						.add(new RouteRecord("TALK", Integer.parseInt(p[2]), Integer.parseInt(p[3]),
							p[4], p[5], p[6], p[7], p[8], p[9], p[10], routePriority(p, 12)));
					case "C" -> routes.computeIfAbsent(questId, key -> new java.util.ArrayList<>())
						.add(new RouteRecord("CAN_ACT", Integer.parseInt(p[2]), Integer.parseInt(p[3]),
							p[4], p[5], p[6], p[7], p[8], p[9], "-", routePriority(p, 11)));
					// Q：无目标 QUEST_ACTION 事件（QuestEvent.QuestDialog），npcId 无意义记 0。
					case "Q" -> routes.computeIfAbsent(questId, key -> new java.util.ArrayList<>())
						.add(new RouteRecord("QUEST_ACTION", Integer.parseInt(p[2]), 0,
							p[3], p[4], p[5], p[6], p[7], p[8], "-", routePriority(p, 9)));
					// E：EnterWorld 无源事件（QE-051 奖励行自愈边），source=null 由编译器回放。
					case "E" -> routes.computeIfAbsent(questId, key -> new java.util.ArrayList<>())
						.add(new RouteRecord("ENTER_WORLD", 0, 0, "-", "-", p[2], p[3], p[4], p[5], "-", "-"));
					case "B" -> blocks.computeIfAbsent(questId, key -> new java.util.ArrayList<>())
						.add(new BlockRecord(p[2], Integer.parseInt(p[3]), p[4], p[5], p[6]));
					// I：item_check 门（npc-item-report 糖元素）——removeCount/failurePage 保留原始
					// token，语义解析在编译器（与 QuestXmlBlockExpander 同口径）。
					case "I" -> itemReports.computeIfAbsent(questId, key -> new java.util.ArrayList<>())
						.add(new ItemReportRecord(Integer.parseInt(p[2]), p[3], p[4],
							Integer.parseInt(p[5]), Integer.parseInt(p[6]), p[7], p[8]));
					case "P" -> layouts.put(questId, new LayoutRecord(Integer.parseInt(p[2]),
						Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]),
						p[6], p[7]));
					default -> throw new IOException("unknown record kind: " + line);
				}
			}
		}
		return new RetailClientTalkChainSteps(Map.copyOf(nodes), Map.copyOf(routes), Map.copyOf(blocks),
			Map.copyOf(layouts), Map.copyOf(itemReports));
	}
}
