package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * DD enterarea 别名 → 服务端登记区名解析表（{@code quest_enterarea_zone_resolution.tsv}，只读视图）。
 * <p>
 * 运行时 enter-zone 事件只按 zones XML 登记名派发（{@code PlayerController#onEnterZone} →
 * {@code QuestEngine#onEnterZone(ZoneName)}），DD value0 的驼峰别名不会命中任何模板——别名必须
 * 先解析成登记名，否则 EA 步是死边（静默卡行）。证据两源：遗留 enter-zone 序列（git 历史，
 * legacy-enterzone / legacy-enterzone-name-match）与真端世界 questscript_area（并注册进
 * zones_quest.xml，retail-world-questscript-area）。
 * <p>
 * Resolution table from DD enterarea aliases to the registered server zone names (read-only view).
 * Runtime enter-zone events dispatch by the zones-XML registered name only, so a raw camel-case DD
 * alias never matches any template and the EA step would be a dead edge; every alias resolves
 * through this table first. Evidence sources: the legacy enter-zone sequences (git history) and
 * the retail world questscript_area (registered into zones_quest.xml).
 */
public final class RetailEnterAreaZoneResolution {

	private static final RetailEnterAreaZoneResolution EMPTY = new RetailEnterAreaZoneResolution(Map.of());

	private final Map<Integer, Map<String, String>> zones;

	private RetailEnterAreaZoneResolution(Map<Integer, Map<String, String>> zones) {
		this.zones = zones;
	}

	/** 空解析表（测试合成器用）。 / Empty table for tests. */
	public static RetailEnterAreaZoneResolution empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the resolution TSV. */
	public static RetailEnterAreaZoneResolution load(InputStream input) throws IOException {
		Map<Integer, Map<String, String>> parsed = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank() || !Character.isDigit(line.charAt(0))) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 3) {
					continue;
				}
				parsed.computeIfAbsent(Integer.parseInt(parts[0].trim()), key -> new HashMap<>())
					.put(parts[1].trim(), parts[2].trim());
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("malformed enterarea zone-resolution registry", e);
		}
		Map<Integer, Map<String, String>> frozen = new HashMap<>();
		parsed.forEach((questId, aliases) -> frozen.put(questId, Map.copyOf(aliases)));
		return new RetailEnterAreaZoneResolution(Map.copyOf(frozen));
	}

	/** 别名的登记区名；未登记返回 null（编译器以 RETAIL_ENTERAREA_ZONE_UNRESOLVED 拒绝）。
	 * The registered zone name for the alias; null when unresolvable (the compiler rejects). */
	public String zoneName(int questId, String alias) {
		Map<String, String> aliases = zones.get(questId);
		return aliases == null ? null : aliases.get(alias);
	}
}
