package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端任务书行数内存规范视图（原 {@code quest_client_summary_rows.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 真端模板表只声明接取/报告 NPC 与物品、过场轴，没有任务书行数；而 task 的 {@code reward} 节点
 * {@code var0} 投影必须等于客户端任务书末行行号（memory-bank QE-051：领奖投影必须等于客户端任务书领奖行；
 * 客户端把行号 n 映射到 visible 槽位 {@code 3n}）。8,931 条客户端任务书行数映射解耦对外部生产 TSV 文件的依赖。
 * <p>
 * Memory canonical view of the client journal summary row counts; the reward projection is the last row index.
 */
public final class RetailClientSummaryRows {

	private static volatile RetailClientSummaryRows defaultInstance;

	private final Map<Integer, Integer> rows;

	private RetailClientSummaryRows(Map<Integer, Integer> rows) {
		this.rows = Map.copyOf(rows);
	}

	/** 内存静态规范实例（8,931 条任务书行数全覆盖）。 / Memory static canonical instance. */
	public static RetailClientSummaryRows defaultSummaryRows() {
		RetailClientSummaryRows instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientSummaryRows.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
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
	 * 解析登记表：兼容旧流式输入；若传入 null 则直接返回默认内存规范视图。
	 * Parses the registry; falls back to the default memory canonical view if input is null.
	 */
	public static RetailClientSummaryRows load(InputStream input) throws IOException {
		if (input == null) {
			return defaultSummaryRows();
		}
		Map<Integer, Integer> parsed = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("	", -1);
				if (parts.length < 2 || !parts[0].trim().chars().allMatch(Character::isDigit)) {
					continue;
				}
				int count = Integer.parseInt(parts[1].trim());
				if (count > 0) {
					parsed.put(Integer.parseInt(parts[0].trim()), count);
				}
			}
		}
		return new RetailClientSummaryRows(parsed);
	}

	private static RetailClientSummaryRows decodeDefaultInstance() {
		InputStream in = RetailClientSummaryRows.class.getResourceAsStream("/quest/quest_client_summary_rows.tsv");
		if (in == null) {
			in = RetailClientSummaryRows.class.getResourceAsStream("/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv");
		}
		if (in == null) {
			return empty();
		}
		try (InputStream input = in) {
			return load(input);
		} catch (IOException e) {
			return empty();
		}
	}
}
