package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端任务书行数登记表（{@code quest_client_summary_rows.tsv}）只读视图。
 * <p>
 * 真端模板表只声明接取/报告 NPC 与物品、过场轴，**没有任务书行数**；而 task 的 {@code reward} 节点
 * {@code var0} 投影必须等于客户端任务书末行行号（memory-bank QE-051：领奖投影必须等于客户端任务书领奖行；
 * 客户端把行号 n 映射到 visible 槽位 {@code 3n}）。行数由
 * {@code .agents/summary/scriptdll-quest-driver/build_quest_client_summary_rows.py} 从 5.8 客户端
 * {@code Dialogs/** /quest_q<id>.html} 的 {@code quest_summary} 页烘焙成只读资源。
 * <p>
 * Read-only view of the client journal summary row counts; the reward projection is the last row index.
 */
public final class RetailClientSummaryRows {

	private final Map<Integer, Integer> rows;

	private RetailClientSummaryRows(Map<Integer, Integer> rows) {
		this.rows = Map.copyOf(rows);
	}

	/** 空登记表（用于不涉及任务书行数的场景）。 / An empty registry. */
	public static RetailClientSummaryRows empty() {
		return new RetailClientSummaryRows(Map.of());
	}

	/** 客户端任务书行数；未登记返回 0。 / Client journal row count, 0 when unknown. */
	public int rows(int questId) {
		return rows.getOrDefault(questId, 0);
	}

	/** 客户端任务书末行行号（= 行数 - 1，未登记按 0）。 / Index of the last client journal row. */
	public int lastRowIndex(int questId) {
		return Math.max(0, rows(questId) - 1);
	}

	public int size() {
		return rows.size();
	}

	/**
	 * 解析登记表：{@code quest_id \t rows}，{@code #} 开头为注释。
	 * Parses the registry: {@code quest_id \t rows}, {@code #} lines are comments.
	 */
	public static RetailClientSummaryRows load(InputStream input) throws IOException {
		Map<Integer, Integer> rows = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length < 2 || !parts[0].trim().chars().allMatch(Character::isDigit)) {
					continue;
				}
				int count = Integer.parseInt(parts[1].trim());
				if (count > 0) {
					rows.put(Integer.parseInt(parts[0].trim()), count);
				}
			}
		}
		return new RetailClientSummaryRows(rows);
	}
}
