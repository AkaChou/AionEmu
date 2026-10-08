package com.aionemu.gameserver.questEngine.tablelane;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.retail.RetailLedgerXml;

/**
 * DD 链式接取发放边表（`category_acquire_=none` 行的自动发放面，2026-10-08 缺口修复）。
 * <p>
 * 数据 = 本仓生成物 {@code quest/retail/quest_chain_acquire_edges.xml}（生成器
 * {@code .agents/summary/quest-chain-acquire-native/build_quest_chain_acquire_edges.py}；禁止手改）。
 * 每行 = 一个后继 quest + 其全部前序 + 证据轴（真端 {@code finished_quest_cond} / 退役定义 XML /
 * 退役 Java handler 的 {@code defaultOnLvlUpEvent}）。本类只做装载与良构校验，不碰语义：
 * 运行期只注册「被本车道接管 ∧ 可路由 ∧ 接取类别 none ∧ 非冻结」的后继（过滤在
 * {@link DataDrivenNativeRuntime} 内逐行裁定，理由面见门禁 {@code QuestChainAcquireResourceGateTest}）。
 * <p>
 * The DD chain-acquire edge table (the auto-grant face of {@code acquire=none} rows). The file is a
 * generated in-repo resource; one row = a successor quest plus all of its predecessors and the
 * evidence axis. This loader only parses and validates shape; the runtime filter (owned ∧ routed ∧
 * acquire kind none ∧ unfrozen) stays with the runtime, and per-row reasons are pinned by the gate.
 */
public final class ChainAcquireEdges {

	/** 发放边资源路径。 / The chain-acquire edge table resource. */
	public static final String TABLE_RESOURCE =
		"/aion/data/static_data/quest/retail/quest_chain_acquire_edges.xml";

	/** 发放边证据轴（生成器写入；未知取值 fail-closed）。 / The provenance axis (unknown token fails closed). */
	public enum Source {
		/** 真端 {@code quest.xml finished_quest_condN}。 / The retail finished-quest condition column. */
		RETAIL_FINISHED_COND,
		/** 退役/存活任务定义 XML 的 {@code <prerequisites>} / {@code finished} 起始条件。 */
		RETIRED_XML,
		/** 退役 Java handler 的 {@code defaultOnLvlUpEvent(env, <前序>, true)}。 */
		RETIRED_HANDLER;

		/**
		 * 词法 → 枚举（未知取值返回 {@code null}，由装载器转 MALFORMED）。
		 * Lexical token to enum ({@code null} on unknown tokens; the loader turns it into MALFORMED).
		 */
		public static Source of(String token) {
			if (token == null) {
				return null;
			}
			return switch (token.trim()) {
				case "retail-finished-cond" -> RETAIL_FINISHED_COND;
				case "retired-xml" -> RETIRED_XML;
				case "retired-handler" -> RETIRED_HANDLER;
				default -> null;
			};
		}
	}

	/**
	 * 一条发放边（后继 + 前序 + 证据）。 / One grant edge (successor, predecessors, provenance).
	 */
	public record Edge(int successor, List<Integer> predecessors, Source source, String evidence) {
		public Edge {
			predecessors = List.copyOf(predecessors);
		}
	}

	private static volatile ChainAcquireEdges instance;

	private final Map<Integer, Edge> bySuccessor;

	private ChainAcquireEdges(Map<Integer, Edge> bySuccessor) {
		this.bySuccessor = bySuccessor;
	}

	/** 生产单例（类路径表；缺表 = 装载失败）。 / The production singleton (missing table = load failure). */
	public static ChainAcquireEdges instance() {
		ChainAcquireEdges local = instance;
		if (local == null) {
			synchronized (ChainAcquireEdges.class) {
				local = instance;
				if (local == null) {
					local = load();
					instance = local;
				}
			}
		}
		return local;
	}

	static ChainAcquireEdges load() {
		try (InputStream input = ChainAcquireEdges.class.getResourceAsStream(TABLE_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MISSING: " + TABLE_RESOURCE);
			}
			return load(input);
		} catch (IOException e) {
			throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_UNREADABLE: " + TABLE_RESOURCE, e);
		}
	}

	/** 测试缝：从任意输入流装载（形状校验与生产同轴）。 / Test seam: loads from any stream, same shape rules. */
	static ChainAcquireEdges load(InputStream input) throws IOException {
		Map<Integer, Edge> bySuccessor = new LinkedHashMap<>();
		for (Element row : RetailLedgerXml.rows(RetailLedgerXml.parse(input), "edge")) {
			String successorText = RetailLedgerXml.text(row, "successor");
			String predecessorsText = RetailLedgerXml.text(row, "predecessors");
			String sourceText = RetailLedgerXml.text(row, "source");
			String evidence = RetailLedgerXml.text(row, "evidence");
			if (successorText == null || predecessorsText == null || sourceText == null || evidence == null
					|| evidence.isBlank()) {
				throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED: successor=" + successorText);
			}
			int successor;
			try {
				successor = Integer.parseInt(successorText);
			} catch (NumberFormatException e) {
				throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED: successor=" + successorText, e);
			}
			List<Integer> predecessors = new java.util.ArrayList<>();
			for (String token : predecessorsText.trim().split("\\s+")) {
				if (token.isBlank()) {
					continue;
				}
				int predecessor;
				try {
					predecessor = Integer.parseInt(token);
				} catch (NumberFormatException e) {
					throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED: successor=" + successor
						+ " predecessor=" + token, e);
				}
				if (predecessor <= 0 || predecessor == successor) {
					// 自环与非法 id 都按表数据破损 fail-closed（生成器侧同样拒绝）。
					// Self edges and illegal ids fail closed as broken table data (the generator rejects them too).
					throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED: successor=" + successor
						+ " predecessor=" + predecessor);
				}
				predecessors.add(predecessor);
			}
			if (successor <= 0 || predecessors.isEmpty()) {
				throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED: successor=" + successorText);
			}
			Source source = Source.of(sourceText);
			if (source == null) {
				throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED: successor=" + successor
					+ " source=" + sourceText);
			}
			if (bySuccessor.put(successor, new Edge(successor, predecessors, source, evidence)) != null) {
				throw new IllegalStateException("QUEST_CHAIN_ACQUIRE_TABLE_MALFORMED: duplicate successor="
					+ successor);
			}
		}
		return new ChainAcquireEdges(Map.copyOf(bySuccessor));
	}

	/** 按后继查发放边（无行 = Optional.empty）。 / The edge by successor (absent row = Optional.empty). */
	public Optional<Edge> edge(int successor) {
		return Optional.ofNullable(bySuccessor.get(successor));
	}

	/** 全表（门禁断言面）。 / All rows (the gate-assertion face). */
	public Map<Integer, Edge> all() {
		return bySuccessor;
	}

	/** 全部后继 id。 / All successor ids. */
	public Set<Integer> successors() {
		return bySuccessor.keySet();
	}
}
