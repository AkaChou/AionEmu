package com.aionemu.gameserver.questEngine.tablelane;

import java.util.Locale;
import java.util.Set;

import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;

/**
 * 原版物品符号解析（native 车道的共享规则，P5 起抽出）。
 * <p>
 * 单元 = 「符号 [数量]」；符号按原版两通道约定解析：先按原名查物品 {@code name_desc}，未命中再去
 * {@code ITEM_} 前缀重查（原版表事实，2026-10-01 全量复算：SimpleTalk give/remove 663 个符号全为
 * {@code ITEM_X} 形式，quest.xml collect/work 列则全为原名形式）。未解符号进调用方的未解集合，
 * 调用方据此 fail-closed。
 * <p>
 * Shared retail item-symbol parsing for the native lane: a cell is {@code SYMBOL [COUNT]}, and the
 * symbol goes through the two-channel rule (the plain name first, then the {@code ITEM_}-stripped
 * alias). Unresolved symbols are recorded for the caller's fail-closed verdict.
 */
public final class NativeItemSymbols {

	/** 一件物品（解析结果）。 / One parsed item. */
	public record ItemStack(int itemId, int count) {
	}

	private NativeItemSymbols() {
	}

	/**
	 * 解析单元；空单元返回 null，未解符号登记后返回 null。
	 * Parses one cell; blank cells and unresolved symbols return null (the latter recorded).
	 */
	public static ItemStack parse(String symbol, int questId, RetailItemNameIndex itemIndex,
			Set<String> unresolved) {
		if (symbol == null || symbol.isBlank()) {
			return null;
		}
		String[] parts = symbol.trim().split("\\s+");
		int count;
		try {
			count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		} catch (NumberFormatException e) {
			throw new IllegalStateException(
				"NATIVE_TABLE_PARSE_FAILED: quest " + questId + " has a non-numeric item count in " + symbol);
		}
		String stem = parts[0].toLowerCase(Locale.ROOT);
		Integer itemId = itemIndex.resolve(stem);
		if (itemId == null && stem.startsWith("item_")) {
			itemId = itemIndex.resolve(stem.substring("item_".length()));
		}
		if (itemId == null) {
			unresolved.add(symbol);
			return null;
		}
		return new ItemStack(itemId, count);
	}
}
