package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * DD enterarea 别名 → 服务端登记区名解析表内存规范视图（原 {@code quest_enterarea_zone_resolution.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * Memory canonical view of the DD enterarea alias to registered zone name resolution table.
 */
public final class RetailEnterAreaZoneResolution {

	private static final RetailEnterAreaZoneResolution EMPTY = new RetailEnterAreaZoneResolution(Map.of());
	private static volatile RetailEnterAreaZoneResolution defaultInstance;

	private final Map<Integer, Map<String, String>> zones;

	private RetailEnterAreaZoneResolution(Map<Integer, Map<String, String>> zones) {
		this.zones = zones;
	}

	/** 内存静态规范实例（83 个任务 146 个区域映射全覆盖）。 / Memory static canonical instance. */
	public static RetailEnterAreaZoneResolution defaultZoneResolution() {
		RetailEnterAreaZoneResolution instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailEnterAreaZoneResolution.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	private static RetailEnterAreaZoneResolution decodeDefaultInstance() {
		InputStream in = RetailEnterAreaZoneResolution.class.getResourceAsStream("/quest/quest_enterarea_zone_resolution.tsv");
		if (in == null) {
			in = RetailEnterAreaZoneResolution.class.getResourceAsStream("/aion/data/static_data/quest/retail/quest_enterarea_zone_resolution.tsv");
		}
		if (in == null) {
			return EMPTY;
		}
		try (InputStream input = in) {
			return load(input);
		} catch (IOException e) {
			return EMPTY;
		}
	}

	public static RetailEnterAreaZoneResolution load() {
		return defaultZoneResolution();
	}

	/** 空解析表（测试合成器用）。 / Empty table for tests. */
	public static RetailEnterAreaZoneResolution empty() {
		return EMPTY;
	}

	/** 全量区域别名映射（任务 ID → (别名 → 登记名)）。 / All zone alias mappings. */
	public Map<Integer, Map<String, String>> zones() {
		return zones;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the resolution TSV. */
	public static RetailEnterAreaZoneResolution load(InputStream input) throws IOException {
		if (input == null) {
			return defaultZoneResolution();
		}
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
