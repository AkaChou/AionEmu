package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端报告模式登记（{@code quest_client_use_item_report.tsv}，只读视图）。
 * <p>
 * SimpleUseItem 的交付方式由客户端任务书 select5 页的按钮决定：{@code CHECK} = 页面按钮是
 * {@code HACTION_CHECK_USER_HAS_QUEST_ITEM}（39/20002 交付检查对，交付物 = 用物品本体）；
 * {@code REWARD} = 页面无检查按钮，1009 直接交付进领奖。生成脚本
 * {@code p3b_client_use_item_report.py} 从客户端 Dialogs 的 select5 页烘焙。
 * <p>
 * Read-only view of the client report-mode registry for SimpleUseItem (CHECK pair vs direct 1009).
 */
public final class RetailClientUseItemReport {

	/** 交付模式。 / Report mode. */
	public enum Mode {

		/** select5 有检查按钮：39/20002 交付检查对。 / select5 carries the check button. */
		CHECK,
		/** select5 无检查按钮：1009 直接交付。 / No check button; direct 1009 hand-in. */
		REWARD
	}

	private static final RetailClientUseItemReport EMPTY =
		new RetailClientUseItemReport(Map.of(), Map.of());

	private final Map<Integer, Mode> modes;
	private final Map<Integer, Integer> items;

	private RetailClientUseItemReport(Map<Integer, Mode> modes, Map<Integer, Integer> items) {
		this.modes = modes;
		this.items = items;
	}

	/** 空登记（全部按 REWARD 处理）。 / Empty registry: everything is treated as REWARD. */
	public static RetailClientUseItemReport empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 开头为注释）。 / Parses the registry TSV. */
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
