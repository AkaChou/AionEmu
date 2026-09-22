package com.aionemu.gameserver.commands.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 中文地点表守卫：条目必须指向仍存在的 teleport_location.xml loc_id，且常用地图锚点不漂移。
 * Chinese location table gate: every entry must point at an existing teleport_location.xml loc_id and the
 * well-known map anchors must not drift.
 * <p>
 * 表由 .agents/summary/chinese-gm-commands/generate_teleport_names_zh.py 依据国服客户端字符串表生成；
 * 本用例覆盖"teleport_location.xml 改了 loc_id，但表没重新生成"的失效场景。
 * The table is generated from the CN client string tables by the script above; this test covers the failure
 * mode where teleport_location.xml changes loc ids without regenerating the table.
 */
class ChineseTeleportNamesTest {

	/** 运行期使用的中文地点表（与 {@link MoveToZh} 读取的是同一相对路径）。 */
	private static final Path NAME_TABLE = Path.of("src/main/resources/aion/config", MoveToZh.NAME_TABLE_PATH);

	/** teleport_location.xml 静态数据。 */
	private static final Path TELELOC = Path.of("src/main/resources/aion/data/static_data/teleport_location.xml");

	/** 每个 {@code teleloc_template} 标签。 */
	private static final Pattern TELELOC_TAG = Pattern.compile("<teleloc_template\\b[^>]*>");

	/**
	 * 表内每条记录都必须指向存在的 loc_id，并带有非空中文名。
	 * Every row must reference an existing loc id and carry a non-empty Chinese name.
	 */
	@Test
	void everyEntryPointsToAnExistingTelelocation() throws IOException {
		Map<Integer, Integer> telelocations = telelocationsByLocId();
		List<String> rows = nameTableRows();

		for (String row : rows) {
			String[] columns = row.split("\\t+");
			assertTrue(columns.length >= 2, "malformed row: " + row);

			int locId = Integer.parseInt(columns[0]);
			assertTrue(telelocations.containsKey(locId),
				"loc_id " + locId + " no longer exists in teleport_location.xml: " + row);
			assertTrue(!columns[1].trim().isEmpty(), "row without a Chinese name: " + row);
		}
		assertTrue(rows.size() > 200, "unexpectedly small Chinese location table: " + rows.size());
	}

	/**
	 * 常用地图锚点必须仍然指向各自的世界 ID，防止中文名与 loc_id 错位。
	 * Well-known map anchors must still resolve to their world ids so names cannot silently shift.
	 */
	@Test
	void anchorNamesKeepTheirWorlds() throws IOException {
		Map<Integer, Integer> telelocations = telelocationsByLocId();

		assertAnchor("泰奥勃莫斯", 210060000, telelocations);
		assertAnchor("普埃塔", 210010000, telelocations);
		assertAnchor("伏魔殿", 120010000, telelocations);
	}

	/**
	 * 校验单个中文名锚点。
	 * Verifies one Chinese-name anchor.
	 *
	 * @param name 中文名 / Chinese name
	 * @param worldId 期望的世界 ID / Expected world id
	 * @param telelocations loc_id → mapid / loc id to world id
	 * @throws IOException 读取地点表失败时 / When the table cannot be read
	 */
	private static void assertAnchor(String name, int worldId, Map<Integer, Integer> telelocations) throws IOException {
		List<Integer> matchedWorlds = new ArrayList<>();
		for (String row : nameTableRows()) {
			String[] columns = row.split("\\t+");
			if (columns.length >= 2 && columns[1].trim().equals(name)) {
				matchedWorlds.add(telelocations.get(Integer.parseInt(columns[0])));
			}
		}
		assertTrue(!matchedWorlds.isEmpty(), "anchor name missing from the Chinese location table: " + name);
		assertTrue(matchedWorlds.contains(worldId),
			"anchor " + name + " no longer maps to world " + worldId + ": " + matchedWorlds);
	}

	/**
	 * 读取中文地点表的数据行（跳过注释与空行）。
	 * Reads the data rows of the Chinese location table (comments and blank lines skipped).
	 *
	 * @return 数据行 / Data rows
	 * @throws IOException 读取失败时 / When the table cannot be read
	 */
	private static List<String> nameTableRows() throws IOException {
		List<String> rows = new ArrayList<>();
		for (String line : Files.readAllLines(NAME_TABLE, StandardCharsets.UTF_8)) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
				rows.add(trimmed);
			}
		}
		return rows;
	}

	/**
	 * 解析 teleport_location.xml 的 loc_id → mapid 索引。
	 * Parses the loc_id to mapid index of teleport_location.xml.
	 *
	 * @return loc_id → mapid / loc id to world id
	 * @throws IOException 读取失败时 / When the file cannot be read
	 */
	private static Map<Integer, Integer> telelocationsByLocId() throws IOException {
		Map<Integer, Integer> telelocations = new HashMap<>();
		Matcher tags = TELELOC_TAG.matcher(Files.readString(TELELOC, StandardCharsets.UTF_8));
		while (tags.find()) {
			telelocations.put(attribute(tags.group(), "loc_id"), attribute(tags.group(), "mapid"));
		}
		return telelocations;
	}

	/**
	 * 读取标签属性值。
	 * Reads one attribute value of a tag.
	 *
	 * @param tag 标签文本 / Tag text
	 * @param name 属性名 / Attribute name
	 * @return 属性值 / Attribute value
	 */
	private static int attribute(String tag, String name) {
		Matcher matcher = Pattern.compile(name + "=\"(\\d+)\"").matcher(tag);
		assertTrue(matcher.find(), "missing " + name + " attribute: " + tag);
		return Integer.parseInt(matcher.group(1));
	}
}
