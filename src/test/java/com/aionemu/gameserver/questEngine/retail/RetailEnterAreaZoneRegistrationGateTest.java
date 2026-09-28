package com.aionemu.gameserver.questEngine.retail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 常设门：DD {@code enterarea} 别名解析表的**目标区名必须已在 zones XML 登记**（fail-closed）。
 * <p>
 * 背景：运行时 enter-zone 事件只按 zones XML 的登记名派发（{@code PlayerController#onEnterZone} →
 * {@code QuestEngine#onEnterZone(ZoneName)}）。{@code quest_enterarea_zone_resolution.tsv} 把 DD value0
 * 的驼峰别名解析成登记名；解析表若指向一个**未登记**的区名，该 EA 步就是死边——任务静默卡行，
 * 且编译期看不出来（编译器只校验"别名已登记在解析表里"，不校验"目标区名在 zones XML 里"）。
 * <p>
 * 本门把后半句补上：解析表每个目标区名必须出现在 {@code aion/data/static_data/zones/} 的
 * {@code zones_*.xml} 里（既含 {@code zones_quest.xml} 的 quest 子区，也含逐地图
 * {@code zones_<mapId>.xml} 的地图区——实测 101 个目标名里 15 个只登记在逐地图文件）。
 * <p>
 * Standing gate: every target zone name of the DD enterarea alias resolution table must be registered
 * by a zones XML template. A resolution entry pointing at an unregistered name makes the enter-area
 * step a dead edge that no compile-time check currently catches.
 */
class RetailEnterAreaZoneRegistrationGateTest {

	private static final String RESOLUTION =
		"/aion/data/static_data/quest_retail/quest_enterarea_zone_resolution.tsv";
	private static final String ZONES_DIR = "/aion/data/static_data/zones";
	private static final Pattern ZONE_NAME = Pattern.compile("<zone\\b[^>]*\\bname=\"([^\"]+)\"");
	/** zones 登记名总量的下界（防空表通过：目录缺失或解析失效时立刻红）。 / Sanity floor. */
	private static final int MIN_REGISTERED_ZONES = 4000;

	/** ①解析表非空、每行三列完整（quest / alias / zone_name）。 / Table shape. */
	@Test
	void resolutionTableIsWellFormed() throws Exception {
		List<String[]> rows = resolutionRows();
		assertFalse(rows.isEmpty(), "解析表不得为空");
		List<String> malformed = new ArrayList<>();
		for (String[] row : rows) {
			for (String column : row) {
				if (column.isBlank()) {
					malformed.add(String.join("|", row));
					break;
				}
			}
		}
		assertTrue(malformed.isEmpty(), () -> "解析表存在空列（quest/alias/zone_name 必填）: " + malformed);
	}

	/** ②解析目标（逐个不同区名）必须全部已登记。 / Every distinct target name must be registered. */
	@Test
	void everyResolvedZoneNameIsRegistered() throws Exception {
		Set<String> registered = registeredZoneNames();
		Map<String, String> unresolved = new LinkedHashMap<>();
		for (String[] row : resolutionRows()) {
			String zoneName = row[2];
			if (!registered.contains(zoneName)) {
				unresolved.putIfAbsent(zoneName, row[1]);
			}
		}
		assertTrue(unresolved.isEmpty(), () -> "解析表目标区名未在 zones XML 登记（EA 步会成静默死边，"
			+ "登记名或别名归属需要重新裁定）: " + unresolved);
	}

	private static List<String[]> resolutionRows() throws Exception {
		try (InputStream input = RetailEnterAreaZoneRegistrationGateTest.class.getResourceAsStream(RESOLUTION)) {
			assertNotNull(input, () -> "缺少解析表资源: " + RESOLUTION);
			List<String[]> rows = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("#") || line.isBlank() || !Character.isDigit(line.charAt(0))) {
						continue;
					}
					String[] parts = line.split("\t");
					if (parts.length >= 3) {
						rows.add(new String[] { parts[0].trim(), parts[1].trim(), parts[2].trim() });
					}
				}
			}
			return rows;
		}
	}

	private static Set<String> registeredZoneNames() throws Exception {
		URL url = RetailEnterAreaZoneRegistrationGateTest.class.getResource(ZONES_DIR);
		assertNotNull(url, () -> "缺少 zones 数据目录: " + ZONES_DIR);
		Path dir = Path.of(url.toURI());
		Set<String> names = new TreeSet<>();
		try (Stream<Path> entries = Files.list(dir)) {
			for (Path entry : entries.filter(Files::isRegularFile).toList()) {
				String file = entry.getFileName().toString();
				if (!file.startsWith("zones_") || !file.endsWith(".xml")) {
					continue;
				}
				Matcher matcher = ZONE_NAME.matcher(Files.readString(entry, StandardCharsets.UTF_8));
				while (matcher.find()) {
					names.add(matcher.group(1));
				}
			}
		}
		assertTrue(names.size() >= MIN_REGISTERED_ZONES,
			() -> "zones XML 登记名解析结果异常偏少（解析失效或数据缺失）: " + names.size());
		return names;
	}
}
