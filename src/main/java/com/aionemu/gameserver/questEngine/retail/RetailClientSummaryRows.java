package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 客户端任务书行数内存规范视图（原 {@code quest_client_summary_rows.tsv} 退役后转为紧凑内联静态规范）。
 * <p>
 * 真端模板表只声明接取/报告 NPC 与物品、过场轴，没有任务书行数；而 task 的 {@code reward} 节点
 * {@code var0} 投影必须等于客户端任务书末行行号（memory-bank QE-051：领奖投影必须等于客户端任务书领奖行；
 * 客户端把行号 n 映射到 visible 槽位 {@code 3n}）。8,931 条客户端任务书行数映射采用变长差分与 GZIP
 * 紧凑存储在内存规范视图中，解耦对外部 TSV 文件的依赖。
 * <p>
 * Memory canonical view of the client journal summary row counts; the reward projection is the last row index.
 */
public final class RetailClientSummaryRows {

	private static final String[] COMPACT_DATA_CHUNKS = new String[] {
		"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"
	};

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
					instance = new RetailClientSummaryRows(decodeCompactData());
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

	private static Map<Integer, Integer> decodeCompactData() {
		StringBuilder sb = new StringBuilder();
		for (String chunk : COMPACT_DATA_CHUNKS) {
			sb.append(chunk);
		}
		byte[] compressed = Base64.getDecoder().decode(sb.toString());
		Map<Integer, Integer> map = new HashMap<>(8931);
		try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
			int curQid = 0;
			int b;
			while ((b = gz.read()) != -1) {
				int shift = 7;
			int delta = b & 0x7F;
				while ((b & 0x80) != 0) {
					b = gz.read();
					if (b == -1) {
						break;
					}
					delta |= (b & 0x7F) << shift;
					shift += 7;
				}
				curQid += delta;
				int row = gz.read();
				if (row == -1) {
					break;
				}
				map.put(curQid, row);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to decode compact summary rows", e);
		}
		return Map.copyOf(map);
	}
}
