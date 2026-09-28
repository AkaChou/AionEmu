package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端报告模式登记（只读内存视图）。
 * <p>
 * SimpleUseItem 的交付方式：CHECK = 交付物 HasItem 门控（5 个活动任务，交付物 = 用物品本体）；
 * REWARD = 1009 直接交付进领奖。该视图在 2026-09-28 退役 {@code quest_client_use_item_report.tsv}
 * 后转为内存静态视图。
 * <p>
 * In-memory view of the client report-mode registry for SimpleUseItem (retired TSV, in-memory view).
 */
public final class RetailClientUseItemReport {

	/** 交付模式。 / Report mode. */
	public enum Mode {

		/** select5 有检查按钮：39/20002 交付检查对。 / select5 carries the check button. */
		CHECK,
		/** select5 无检查按钮：1009 直接交付。 / No check button; direct 1009 hand-in. */
		REWARD
	}

	/** 5 个 CHECK 模式任务及其交付物 id（与退役前 TSV 逐项恒等）。 */
	private static final Map<Integer, Integer> CHECK_ITEMS = Map.of(
		80482, 182215419,
		80486, 182215421,
		80554, 182215444,
		80558, 182215445,
		80612, 182215579
	);

	private static final RetailClientUseItemReport DEFAULT =
		new RetailClientUseItemReport(
			Map.of(
				80482, Mode.CHECK,
				80486, Mode.CHECK,
				80554, Mode.CHECK,
				80558, Mode.CHECK,
				80612, Mode.CHECK
			),
			CHECK_ITEMS
		);

	private static final RetailClientUseItemReport EMPTY =
		new RetailClientUseItemReport(Map.of(), Map.of());

	private final Map<Integer, Mode> modes;
	private final Map<Integer, Integer> items;

	private RetailClientUseItemReport(Map<Integer, Mode> modes, Map<Integer, Integer> items) {
		this.modes = modes;
		this.items = items;
	}

	/** 缺省规范报告模式（退役后生产通道）。 / Default canonical report-mode registry. */
	public static RetailClientUseItemReport defaultReport() {
		return DEFAULT;
	}

	/** 空登记（全部按 REWARD 处理）。 / Empty registry: everything is treated as REWARD. */
	public static RetailClientUseItemReport empty() {
		return EMPTY;
	}

	/** 解析登记表（兼容方法；UTF-8 TSV；{@code #} 开头为注释）。 / Parses the registry TSV. */
	public static RetailClientUseItemReport load(InputStream input) throws IOException {
		Map<Integer, Mode> modes = new HashMap<>();
		Map<Integer, Integer> items = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 2) {
					continue;
				}
				int questId = Integer.parseInt(parts[0].trim());
				modes.put(questId, Mode.valueOf(parts[1].trim()));
				if (parts.length >= 3 && !parts[2].isBlank()
					&& parts[2].trim().chars().allMatch(Character::isDigit)) {
					items.put(questId, Integer.parseInt(parts[2].trim()));
				}
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client use-item report registry", e);
		}
		return new RetailClientUseItemReport(Map.copyOf(modes), Map.copyOf(items));
	}

	/** 该任务的交付模式（未登记按 REWARD）。 / The report mode of one quest (REWARD when unknown). */
	public Mode mode(int questId) {
		return modes.getOrDefault(questId, Mode.REWARD);
	}

	/** CHECK 模式的交付物品 id（-1 = 未登记）。 / The CHECK-mode hand-in item id (-1 when unknown). */
	public int checkItemId(int questId) {
		return items.getOrDefault(questId, -1);
	}
}
