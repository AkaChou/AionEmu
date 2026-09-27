package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 物品 {@code name_desc} → item_id 索引。
 * <p>
 * 真端 quest.xml 用符号名（如 {@code quest_1002b}、{@code %Quest_L_magical_30a} 的成员）描述奖励与
 * 交付物，而服务端运行时按 item_id 工作；本索引与 {@link RetailNpcNameIndex} 同构，桥接两者。
 * Index from item {@code name_desc} to item ids, the item-side twin of {@link RetailNpcNameIndex}.
 */
public final class RetailItemNameIndex {

	private static final Pattern TEMPLATE = Pattern.compile("<item_template\\b[^>]*>");
	private static final Pattern NAME_DESC = Pattern.compile("name_desc=\"([^\"]*)\"");
	private static final Pattern ITEM_ID = Pattern.compile("\\bid=\"(\\d+)\"");

	private final Map<String, Integer> byName;

	private RetailItemNameIndex(Map<String, Integer> byName) {
		this.byName = Map.copyOf(byName);
	}

	/** 按符号名解析 item_id（大小写不敏感）；未解析返回 null。 / Resolves an item id by symbolic name, or null. */
	public Integer resolve(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		String key = name.trim().toLowerCase(Locale.ROOT);
		if (key.chars().allMatch(Character::isDigit)) {
			return Integer.valueOf(key);
		}
		return byName.get(key);
	}

	public int size() {
		return byName.size();
	}

	/**
	 * 从若干物品模板文件流构建索引；同一 name_desc 重复出现时保留首个（真端同名同 id）。
	 * Builds the index from item template streams; the first id wins on duplicate names.
	 */
	public static RetailItemNameIndex build(Collection<InputStream> templates) throws IOException {
		Map<String, Integer> byName = new LinkedHashMap<>();
		try {
			for (InputStream input : templates) {
				String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
				Matcher template = TEMPLATE.matcher(text);
				while (template.find()) {
					String tag = template.group();
					Matcher name = NAME_DESC.matcher(tag);
					Matcher id = ITEM_ID.matcher(tag);
					if (!name.find() || !id.find()) {
						continue;
					}
					byName.putIfAbsent(name.group(1).toLowerCase(Locale.ROOT), Integer.valueOf(id.group(1)));
				}
			}
		} finally {
			for (InputStream input : templates) {
				input.close();
			}
		}
		return new RetailItemNameIndex(byName);
	}

	/** 批量解析名字集合；未解析项原样返回。 / Resolves names, reporting unresolved ones verbatim. */
	public record Resolution(Set<String> unresolved, Map<String, Integer> resolved) {
	}
}
