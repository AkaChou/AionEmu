package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 常设防漂移门：真端 TSV 清单冻结（quest-native-dispatch 工程口径）。
 * <p>
 * 背景：本工程停止为微观 HTML 页码扩建 TSV 补丁，因此
 * {@code src/main/resources/aion/data/static_data/quest/retail/} 与
 * {@code src/main/resources/aion/definitions/quest_dialog/} 下的 {@code *.tsv} 是**冻结面**：
 * 新增或删除都必须显式登记到 {@code quest-retail-tsv-manifest.tsv}，否则本门变红。
 * 三条断言：①磁盘 *.tsv 集合 == 清单登记集合（双向零差集）；②清单每行 4 列、file 唯一、
 * role/status 取自词表；③登记总数 == {@link #EXPECTED_TSV_COUNT}（防止"用清单追认新增"）。
 * 列数、file 非空与唯一性在共享解析器 {@link #manifest()} 里 fail-fast，因此断言 ① 也会先撞上结构错误。
 * <p>
 * Permanent anti-drift gate: the quest-retail TSV surface is frozen. The on-disk {@code *.tsv} set of
 * both directories must equal the manifest set in both directions, every manifest row must be well
 * formed, and the frozen row count must equal an explicit constant so that a new page-class TSV cannot
 * be retro-fitted into the manifest without a visible constant change in review.
 */
class RetailTsvManifestGateTest {

	/** 冻结目录：真端残留表。 / Frozen directory: the retail leftovers. */
	private static final Path RETAIL_DIR = Path.of("src/main/resources/aion/data/static_data/quest/retail");
	/** 冻结目录：对话契约与例外账。 / Frozen directory: dialog contract and exception ledger. */
	private static final Path DIALOG_DIR = Path.of("src/main/resources/aion/definitions/quest_dialog");
	/** 冻结范围 = 两目录的 *.tsv 并集。 / The frozen surface: the union of both directories. */
	private static final List<Path> FROZEN_DIRS = List.of(RETAIL_DIR, DIALOG_DIR);
	/** 清单文件名：位于 quest_retail 目录内，按文件名从冻结集合里排除。 / Manifest file name. */
	private static final String MANIFEST_NAME = "quest-retail-tsv-manifest.tsv";
	private static final Path MANIFEST = RETAIL_DIR.resolve(MANIFEST_NAME);
	/** role 词表（与清单头部注释一致）。 / Role vocabulary, mirrored in the manifest header. */
	private static final Set<String> ALLOWED_ROLES = Set.of("retail-table", "client-registry", "name-index",
		"server-registry", "retention", "heal", "fingerprint", "contract");
	/** status 词表：在役 / 已知可退役候选（退役前置写在 note 列）。 / Status vocabulary. */
	private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "RETIREMENT_CANDIDATE");
	/**
	 * 冻结总数（双保险）：只有覆盖断言时，"新建 TSV + 顺手登记一行"仍能悄悄放行；
	 * 计数常量迫使新增/退役必须同时改本类常量，从而必然出现在评审 diff 里。
	 * 改此常量必须在任务报告里登记理由（新增了什么、或退役了哪一个及其前置已满足）。
	 * Frozen row count: the only legal way to add or retire a TSV row is to change this constant,
	 * which keeps every change of the frozen surface visible in review.
	 */
	private static final int EXPECTED_TSV_COUNT = 6;

	/**
	 * ①清单覆盖：磁盘上的 *.tsv 集合与清单登记集合双向零差集。
	 * 新增页码类 TSV 必须先登记到清单并在任务报告里写明理由；删除 TSV 必须同步删行。
	 * Coverage: the on-disk {@code *.tsv} set and the manifest set must match in both directions.
	 */
	@Test
	void everyTsvOnDiskIsRegisteredAndEveryRegisteredTsvExists() throws Exception {
		SortedMap<String, String> onDisk = diskTsvs();
		Set<String> registered = new TreeSet<>(manifest().keySet());

		Set<String> unregistered = new TreeSet<>(onDisk.keySet());
		unregistered.removeAll(registered);
		assertTrue(unregistered.isEmpty(), () -> "磁盘上有未登记的 TSV（新建页码类 TSV 必须先登记到 "
			+ MANIFEST_NAME + " 并在报告中说明理由）: " + locate(unregistered, onDisk));

		Set<String> dangling = new TreeSet<>(registered);
		dangling.removeAll(onDisk.keySet());
		assertTrue(dangling.isEmpty(), () -> "清单登记了磁盘上不存在的 TSV（删除文件后必须同步删行）: " + dangling);
	}

	/** ②清单结构：每行 4 列、file 唯一、role/status 取自词表、note 非空。 / Manifest structure. */
	@Test
	void manifestRowsAreWellFormed() throws Exception {
		Map<String, Row> rows = manifest();

		List<String> badRoles = rows.values().stream()
			.filter(row -> !ALLOWED_ROLES.contains(row.role()))
			.map(row -> row.file() + " -> " + row.role())
			.toList();
		assertTrue(badRoles.isEmpty(), () -> "清单 role 列越出词表 " + ALLOWED_ROLES + ": " + badRoles);

		List<String> badStatus = rows.values().stream()
			.filter(row -> !ALLOWED_STATUS.contains(row.status()))
			.map(row -> row.file() + " -> " + row.status())
			.toList();
		assertTrue(badStatus.isEmpty(), () -> "清单 status 列越出词表 " + ALLOWED_STATUS + ": " + badStatus);

		List<String> blankNotes = rows.values().stream()
			.filter(row -> row.note().isBlank())
			.map(Row::file)
			.toList();
		assertTrue(blankNotes.isEmpty(),
			() -> "清单 note 列不得为空（登记行要写清用途与退役前置）: " + blankNotes);
	}

	/**
	 * ③冻结计数：登记总数 == {@link #EXPECTED_TSV_COUNT}，堵住"用清单追认新增"的旁路。
	 * The registered row count must equal the explicit frozen constant.
	 */
	@Test
	void manifestStaysAtTheFrozenSize() throws Exception {
		int registered = manifest().size();
		assertEquals(EXPECTED_TSV_COUNT, registered,
			() -> "TSV 清单规模变动：改 EXPECTED_TSV_COUNT 必须在任务报告里登记理由（新增/退役哪种 TSV，"
				+ "退役前置是否已满足）/ the frozen TSV count changed without a registered reason");
	}

	/** 一行清单登记：file / role / status / note。 / One manifest row. */
	record Row(String file, String role, String status, String note) {
	}

	/**
	 * 清单全量读取（{@code file \t role \t status \t note}；{@code #} 起头为注释）。
	 * 结构错误在这里 fail-fast：列数、file 非空、file 唯一。
	 * Every manifest row; structural problems fail fast here.
	 */
	private static Map<String, Row> manifest() throws IOException {
		assertTrue(Files.isRegularFile(MANIFEST),
			() -> "缺少 TSV 清单，无法判定冻结集合（请在仓库根目录运行 Maven）: " + MANIFEST);
		Map<String, Row> rows = new LinkedHashMap<>();
		for (String line : Files.readAllLines(MANIFEST, StandardCharsets.UTF_8)) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			assertEquals(4, parts.length, () -> "清单行必须 4 列（file/role/status/note）: " + line);
			assertFalse(parts[0].isBlank(), () -> "清单 file 列不得为空: " + line);
			Row previous = rows.put(parts[0], new Row(parts[0], parts[1], parts[2], parts[3]));
			assertNull(previous, () -> "清单 file 列重复: " + parts[0]);
		}
		assertFalse(rows.isEmpty(), "TSV 清单不得为空 / the manifest must not be empty");
		return rows;
	}

	/**
	 * 磁盘 *.tsv：两目录并集（键 = 裸文件名，值 = 目录名；清单自身排除）。
	 * On-disk {@code *.tsv} of both frozen directories, keyed by bare file name.
	 */
	private static SortedMap<String, String> diskTsvs() throws IOException {
		SortedMap<String, String> found = new TreeMap<>();
		for (Path dir : FROZEN_DIRS) {
			assertTrue(Files.isDirectory(dir), () -> "冻结目录不存在（请在仓库根目录运行 Maven）: " + dir);
			try (Stream<Path> entries = Files.list(dir)) {
				for (Path entry : entries.filter(Files::isRegularFile).toList()) {
					String name = entry.getFileName().toString();
					if (!name.endsWith(".tsv") || MANIFEST_NAME.equals(name)) {
						continue;
					}
					// 清单用裸文件名登记，因此两目录不得出现同名 TSV（否则集合会静默塌缩）。
					// Bare names are the manifest key, so the two directories must not collide.
					String previous = found.put(name, dir.getFileName().toString());
					assertNull(previous, () -> "两个冻结目录存在同名 TSV，清单裸文件名无法消歧: " + name);
				}
			}
		}
		return found;
	}

	private static List<String> locate(Set<String> names, SortedMap<String, String> onDisk) {
		return names.stream().map(name -> onDisk.get(name) + "/" + name).toList();
	}
}
