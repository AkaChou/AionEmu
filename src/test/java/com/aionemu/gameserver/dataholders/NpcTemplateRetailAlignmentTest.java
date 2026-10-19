package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

/**
 * 真端 npc 模板对齐的回归闸门：攻击档位写回与伤害区间覆盖。
 * Regression gate for the retail NPC template alignment: attack profile write-back and damage range coverage.
 * <p>真端值取自 {@code 58Server/Map/XML/npcs.xml}（800031/833817 等已逐字段核对）；
 * 测试只读仓库内已提交的数据，不依赖工程外部路径。
 * The retail values were verified field by field against {@code 58Server/Map/XML/npcs.xml}; the test only reads
 * committed data inside the repository and never depends on paths outside the project.
 */
class NpcTemplateRetailAlignmentTest {

	private static final Path NPC_SHARDS = Path.of("src/main/resources/aion/data/static_data/npcs");
	private static final Path NPC_COMBAT = Path.of("src/main/resources/aion/definitions/compact/npc-combat.xml");

	@Test
	void attackProfileIsAlignedWithRetailNpcs() throws Exception {
		Map<Integer, Map<String, String>> templates = readTemplates(
			Set.of(201072, 201042, 206356, 833817, 800031));

		assertProfile(templates, 201072, "15", "1000", "2000");
		assertProfile(templates, 201042, "4", "1000", "6000");
		assertProfile(templates, 206356, "2", "1000", "2000");
		assertProfile(templates, 833817, "25", "1000", "2300");
		assertProfile(templates, 800031, "2", "1000", "2000");
	}

	@Test
	void combatRangesCoverRetailProjection() throws Exception {
		Map<Integer, Map<String, String>> combat = readCombat();

		assertEquals(87_525, combat.size(), "真端伤害区间投影行数（41,455 既有 + 46,070 新增）");
		assertRange(combat, 201072, "297", "446", "1000");
		assertRange(combat, 206356, "273", "410", "1000");
		assertRange(combat, 833817, "1950", "2050", "1150");
		assertRange(combat, 857882, "0", "0", "1000");
		// 真端存在 min > max 的区间（14 行），运行时由 StatFunctions.rollNpcAttackDamage 的 Math.min/max 归一化。
		// Retail ships reversed ranges (14 rows); StatFunctions.rollNpcAttackDamage normalises them via Math.min/max.
		assertRange(combat, 282141, "2468", "426", "1000");

		for (Map.Entry<Integer, Map<String, String>> e : combat.entrySet()) {
			int min = Integer.parseInt(e.getValue().get("min_damage"));
			int max = Integer.parseInt(e.getValue().get("max_damage"));
			int ratio = Integer.parseInt(e.getValue().get("stat_ratio"));
			assertTrue(min >= 0 && max >= 0, () -> "negative damage range: " + e.getKey());
			assertTrue(ratio > 0, () -> "stat_ratio must stay positive: " + e.getKey());
		}
	}

	private static void assertProfile(Map<Integer, Map<String, String>> templates, int npcId, String range,
		String rate, String delay) {
		Map<String, String> attrs = templates.get(npcId);
		assertTrue(attrs != null, () -> "missing npc template " + npcId);
		assertEquals(range, attrs.get("attack_range"), () -> "attack_range of " + npcId);
		assertEquals(rate, attrs.get("attack_rate"), () -> "attack_rate of " + npcId);
		assertEquals(delay, attrs.get("attack_delay"), () -> "attack_delay of " + npcId);
	}

	private static void assertRange(Map<Integer, Map<String, String>> combat, int npcId, String min, String max,
		String ratio) {
		Map<String, String> row = combat.get(npcId);
		assertTrue(row != null, () -> "missing npc-combat row " + npcId);
		assertEquals(min, row.get("min_damage"), () -> "min_damage of " + npcId);
		assertEquals(max, row.get("max_damage"), () -> "max_damage of " + npcId);
		assertEquals(ratio, row.get("stat_ratio"), () -> "stat_ratio of " + npcId);
	}

	private static Map<Integer, Map<String, String>> readTemplates(Set<Integer> wanted) throws Exception {
		Map<Integer, Map<String, String>> rows = new HashMap<>();
		List<Path> files;
		try (Stream<Path> stream = Files.list(NPC_SHARDS)) {
			files = stream.filter(p -> p.getFileName().toString().startsWith("npc_template_")).toList();
		}
		XMLInputFactory factory = XMLInputFactory.newFactory();
		for (Path file : files) {
			try (InputStream in = Files.newInputStream(file)) {
				XMLStreamReader reader = factory.createXMLStreamReader(in);
				while (reader.hasNext()) {
					if (reader.next() != XMLStreamConstants.START_ELEMENT
						|| !"npc_template".equals(reader.getLocalName())) {
						continue;
					}
					String id = reader.getAttributeValue(null, "npc_id");
					if (id == null || !wanted.contains(Integer.parseInt(id))) {
						continue;
					}
					Map<String, String> attrs = new HashMap<>();
					for (String name : List.of("attack_range", "attack_rate", "attack_delay")) {
						attrs.put(name, reader.getAttributeValue(null, name));
					}
					rows.put(Integer.parseInt(id), attrs);
				}
				reader.close();
			}
		}
		return rows;
	}

	private static Map<Integer, Map<String, String>> readCombat() throws Exception {
		Map<Integer, Map<String, String>> rows = new HashMap<>();
		Set<Integer> duplicates = new HashSet<>();
		try (InputStream in = Files.newInputStream(NPC_COMBAT)) {
			XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !"npc".equals(reader.getLocalName())) {
					continue;
				}
				int id = Integer.parseInt(reader.getAttributeValue(null, "id"));
				Map<String, String> row = new HashMap<>();
				for (String name : List.of("min_damage", "max_damage", "stat_ratio")) {
					row.put(name, reader.getAttributeValue(null, name));
				}
				if (rows.put(id, row) != null) {
					duplicates.add(id);
				}
			}
			reader.close();
		}
		assertTrue(duplicates.isEmpty(), () -> "duplicated npc-combat ids: " + duplicates);
		return rows;
	}
}
