package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.retail.RetailLedgerRows;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Kind;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Step;

/**
 * P7 步 2 步 c 门（计划 §10.2「P7 DataDriven」）：DD `enterarea` 轴的**原版同名区**解析与冻结。
 * <p>
 * 原版进区 handler `FUN_180c47bf0` 用名哈希（`FUN_1810798b0`）把当前步别名与进区区名逐值比对 ⇒ 绑定 = 同名。
 * 本门把该轴的四个面冻死，防止漂移：
 * <ol>
 *   <li><b>逐行裁定</b>：切换集（1467 行）里每个 `EnterArea` 步的别名，要么落到同名注册区，要么落在
 *       {@link NativeEnterAreaPort#RETAIL_ABSENT_ALIASES}（原版世界文件无定义）——两者并集必须覆盖全部别名，
 *       任何第三个桶都是 fail-closed 异常；</li>
 *   <li><b>几何逐字来自原版</b>：注册区的胞数、mapid 与多边形摘要必须等于冻结台账
 *       `quest/retail-enterarea-zone-resolution.tsv` 的复算值（坐标与 top/bottom 逐字取自
 *       `<原版根>/Map/Worlds/<world>/world{,_M,_N}.xml`；`_M` 大师服行按 R5 归一到活图 mapid，
 *       原版宿主留痕在台账 `normalized_from` 列）；</li>
 *   <li><b>禁止近似补</b>：冻结别名不得出现在任何 zones XML 里（旧「出生点 + r=10 球体」不许复活），
 *       且端口对未登记别名必须抛稳定码异常，不得静默成为死边；</li>
 *   <li><b>活图归一（R5，2026-10-08）</b>：原版把英吉斯温/格国的感官区只写在 `_M`（大师服）世界文件里
 *       （lf4=0/LF4_M=15、df4=0/DF4_M=13；其余世界都在普通文件里），而镜像世界不是玩家可达目标
 *       （`a7da0ad67` 活图迁移）⇒ DD 进区一律不得注册在镜像世界，`_M` 行必须归一到活图 mapid
 *       （10035 安格利浦关卡进区即活图死步实例）；归一仅换宿主，几何逐字不变。</li>
 * </ol>
 * 证据面：`p7/tools/enterarea-retail-zone-probe.py`（复算工具）、
 * `p7/tools/qe-enterarea-retail-zone-resolution.tsv`（逐行裁定）、`p7/P7-STEP2C-REPORT.zh-CN.md`（报告）。
 * <p>
 * P7 step 2c gate: freezes the retail enter-area axis — per-alias adjudication, retail-verbatim geometry of
 * every registered zone, and the fail-closed rule for aliases the retail world files never define.
 */
class DataDrivenEnterAreaPortGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.xml";
	private static final String EVIDENCE = "/quest/retail-enterarea-zone-resolution.tsv";
	private static final Path ZONES_DIR = Path.of("src/main/resources/aion/data/static_data/zones");
	private static final Path ZONE_FILE = ZONES_DIR.resolve("zones_retail_enterarea.xml");
	private static final Path ZONE_XSD = ZONES_DIR.resolve("zones.xsd");

	/** 切换集规模与两个进区轴规模（progress = 进程步，acquire = 接取 kind）。 */
	private static final int SWITCH_ROWS = 1467;
	private static final int ENTER_AREA_ALIASES = 120;
	private static final int PROGRESS_ALIASES = 105;
	private static final int PROGRESS_RESOLVED = 91;
	private static final int PROGRESS_ABSENT = 14;
	/** 接取轴（另批接线）：原版区几何可解析 14 / 无定义 1；本批不发区数据、不接线。 */
	private static final int ACQUIRE_ALIASES = 15;
	private static final int ACQUIRE_RESOLVED = 14;
	/** progress 轴注册胞总数（多胞感官区按胞计）。 / Total registered cells of the progress axis. */
	private static final int RESOLVED_CELLS = 120;
	/** 多胞样例（原版同名多 `<npc>` 胞）：胞数须逐区保留。 / Multi-cell samples. */
	private static final Map<String, Integer> MULTI_CELL_SAMPLES = Map.of(
		"IDEternity_War_ShugoSeller", 6,
		"DF5_SensoryArea_65_Deva_Q15322b", 3,
		"LF5_SensoryArea_65_Deva_Q25322j", 3);


	private static DataDrivenQuestTable table;
	private static Set<Integer> switchSet;
	private static Map<String, EvidenceRow> evidence;
	private static Map<String, Map<String, String>> registeredZones;
	private static NativeEnterAreaPort port;

	/** 冻结台账的一行。 / One frozen evidence row. */
	private record EvidenceRow(String alias, String axis, String status, String kind, String worldDir, String mapid,
			int cells, String digest, String normalizedFrom) {
	}

	@BeforeAll
	static void loadFixtures() throws Exception {
		table = DataDrivenQuestTable.load(resource(DD_TABLE));
		Map<Integer, String> owners = retentionOwners();
		switchSet = new TreeSet<>();
		for (int questId : table.questIds()) {
			if ("RETAIL_TABLE".equals(owners.get(questId))) {
				switchSet.add(questId);
			}
		}
		evidence = loadEvidence();
		registeredZones = loadZones(ZONE_FILE);
		Set<String> names = new TreeSet<>();
		for (Path path : zoneFiles()) {
			names.addAll(loadZones(path).keySet());
		}
		port = NativeEnterAreaPort.create(table, switchSet, names);
	}

	/** 生成的区注册表必须过自身 schema（多环 `<points>` 是合法声明形）。 / Schema validity. */
	@Test
	void generatedZoneFileValidatesAgainstItsSchema() throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(ZONE_XSD.toFile());
		schema.newValidator().validate(new StreamSource(ZONE_FILE.toFile()));
	}

	/** ① 逐行裁定：切换集进区别名 = 同名注册区 ∪ 原版缺席冻结，二者互斥且无第三桶。 */
	@Test
	void everySwitchSetAliasIsAdjudicated() {
		assertEquals(SWITCH_ROWS, switchSet.size(), "切换集规模冻结（原版活行 ∧ owner RETAIL_TABLE）");
		assertEquals(ENTER_AREA_ALIASES, evidence.size(), "切换集进区别名总数冻结");

		Set<String> resolved = new TreeSet<>();
		Set<String> absent = new TreeSet<>();
		int progress = 0;
		int acquire = 0;
		int acquireResolved = 0;
		for (EvidenceRow row : evidence.values()) {
			if ("progress".equals(row.axis())) {
				progress++;
				if ("OK".equals(row.status())) {
					resolved.add(row.alias());
				} else {
					absent.add(row.alias());
				}
			} else {
				acquire++;
				if ("OK".equals(row.status())) {
					acquireResolved++;
				}
			}
		}
		assertEquals(PROGRESS_ALIASES, progress, "progress 轴别名数冻结");
		assertEquals(PROGRESS_RESOLVED, resolved.size(), "progress 轴原版可解析别名规模冻结");
		assertEquals(PROGRESS_ABSENT, absent.size(), "progress 轴原版无区定义的冻结别名规模冻结");
		assertEquals(ACQUIRE_ALIASES, acquire, "acquire 轴别名数冻结（接取批消费）");
		assertEquals(ACQUIRE_RESOLVED, acquireResolved, "acquire 轴原版可解析别名数冻结（本批不接线）");
		Set<String> portResolved = upper(port.resolvedAliases());
		Set<String> evidenceResolved = upper(resolved);
		Set<String> onlyPort = new TreeSet<>(portResolved);
		onlyPort.removeAll(evidenceResolved);
		Set<String> onlyEvidence = new TreeSet<>(evidenceResolved);
		onlyEvidence.removeAll(portResolved);
		assertTrue(portResolved.equals(evidenceResolved), "端口解析集必须与冻结台账逐元素相等（仅端口 "
			+ onlyPort + " / 仅台账 " + onlyEvidence + "）");
		assertTrue(upper(port.frozenAbsentAliases()).equals(upper(absent)), "端口冻结集必须与冻结台账逐元素相等");
		assertTrue(upper(NativeEnterAreaPort.RETAIL_ABSENT_ALIASES).equals(upper(absent)),
			"代码内冻结常量必须与台账逐元素相等（新增/消失都要显式改表）");

		// 覆盖性：切换集里每个 EnterArea 步都必须被裁定（有名区或冻结），不得静默丢弃。
		Set<String> seenAliases = new TreeSet<>();
		for (int questId : switchSet) {
			Row row = table.find(questId).orElseThrow();
			for (Step step : row.steps()) {
				if (step.kind() != Kind.ENTER_AREA) {
					continue;
				}
				String alias = step.payload().trim();
				seenAliases.add(alias.toUpperCase(Locale.ROOT));
				boolean hasZone = port.zoneName(questId, step.index()).isPresent();
				boolean frozen = port.absentAliases(questId).contains(alias);
				assertTrue(hasZone ^ frozen, "每个 EnterArea 步必须恰落在「同名注册区」或「原版缺席冻结」一侧: quest "
					+ questId + " step " + step.index() + " alias " + alias);
			}
		}
		Set<String> progressAliases = new TreeSet<>();
		evidence.values().stream().filter(row -> "progress".equals(row.axis()))
			.forEach(row -> progressAliases.add(row.alias()));
		assertEquals(upper(progressAliases), seenAliases, "冻结台账必须覆盖切换集 progress 轴实际观测到的全部别名");
		// 接取轴的别名不得混进进程轴（两轴别名集合互斥，见工具口径）。
		for (EvidenceRow row : evidence.values()) {
			if ("acquire".equals(row.axis())) {
				assertFalse(seenAliases.contains(row.alias().toUpperCase(Locale.ROOT)),
					"接取轴别名不得出现在进程步里: " + row.alias());
			}
		}
	}

	/** ② 几何逐字来自原版：解析别名必须同名登记，且胞数 / mapid / 多边形摘要与冻结台账一致。 */
	@Test
	void resolvedAliasesAreRegisteredWithRetailGeometry() {
		int cells = 0;
		for (EvidenceRow row : evidence.values()) {
			if (!"OK".equals(row.status()) || !"progress".equals(row.axis())) {
				continue;
			}
			String key = row.alias().toUpperCase(Locale.ROOT);
			Map<String, String> zone = registeredZones.get(key);
			assertTrue(zone != null, "登记缺失（同 identity 解析要求区名 = 别名）: " + row.alias());
			assertEquals(row.mapid(), zone.get("mapid"), "mapid 必须等于原版世界目录的客户端派生映射: " + row.alias());
			assertEquals(String.valueOf(row.cells()), zone.get("cells"), "胞数必须等于原版世界文件: " + row.alias());
			assertEquals(row.digest(), zone.get("digest"), "多边形摘要必须逐字等于原版世界文件: " + row.alias());
			cells += row.cells();
		}
		assertEquals(RESOLVED_CELLS, cells, "progress 轴注册胞总数冻结（多胞区按胞计）");
		for (Map.Entry<String, Integer> sample : MULTI_CELL_SAMPLES.entrySet()) {
			Map<String, String> zone = registeredZones.get(sample.getKey().toUpperCase(Locale.ROOT));
			assertTrue(zone != null, "多胞样例必须登记: " + sample.getKey());
			assertEquals(String.valueOf(sample.getValue()), zone.get("cells"), "多胞区不得只登记一胞: " + sample.getKey());
		}
	}

	/** ③ 禁止近似补 + 禁止死数据：冻结别名不得注册；接取轴的原版区也不得提前落盘。 */
	@Test
	void frozenAliasesAreNeverRegisteredAnywhere() throws Exception {
		Set<String> all = new TreeSet<>();
		for (Path path : zoneFiles()) {
			all.addAll(loadZones(path).keySet());
		}
		for (String alias : NativeEnterAreaPort.RETAIL_ABSENT_ALIASES) {
			assertFalse(all.contains(alias.toUpperCase(Locale.ROOT)),
				"原版无区定义的别名不得注册（禁「出生点 + 半径」近似几何）: " + alias);
			assertFalse(upper(port.resolvedAliases()).contains(alias.toUpperCase(Locale.ROOT)),
				"冻结别名不得出现在解析集: " + alias);
		}
		// 步 f 起 acquire 轴区数据已落盘（接取侧同名区树）：OK 行必须已注册；原版无区定义的行
		// （R4 fail-closed）保持未注册 = 镜像原版死边。
		// Since step f the acquire-axis zones are registered: OK rows must be registered, the
		// retail-absent row stays unregistered (mirroring the retail-dead edge).
		for (EvidenceRow row : evidence.values()) {
			if ("acquire".equals(row.axis())) {
				assertEquals("OK".equals(row.status()),
					registeredZones.containsKey(row.alias().toUpperCase(Locale.ROOT)),
					"接取轴注册面必须逐行等于探针裁定（OK=已注册 / R4=未注册）: " + row.alias());
			}
		}
	}

	/** ④ 恒等解析：端口返回区名 = DD 别名本身（不做任何名字换算/遗留壳名映射）。 */
	@Test
	void zoneResolutionIsNameIdentityAndNeverALegacyShellName() {
		for (int questId : switchSet) {
			for (Step step : table.find(questId).orElseThrow().steps()) {
				if (step.kind() != Kind.ENTER_AREA) {
					continue;
				}
				String alias = step.payload().trim();
				port.zoneName(questId, step.index()).ifPresent(zone -> {
					assertEquals(alias.toUpperCase(Locale.ROOT), zone.toUpperCase(Locale.ROOT),
						"进区绑定 = 同名（原版名哈希比对），禁止任何名字换算: quest " + questId);
					assertFalse(zone.toUpperCase(Locale.ROOT).endsWith("_302340000"),
						"不得把遗留壳名当作解析结果: " + zone);
				});
			}
		}
		assertEquals("IDAB1_ERE_SENSORYAREA_Q10011A",
			port.zoneName(10011, enterAreaStepOf(10011, "IDAb1_Ere_SensoryArea_Q10011a")).orElseThrow()
				.toUpperCase(Locale.ROOT),
			"原版 IDAb1_Ere_SensoryArea_Q10011a 只能解析到同名区（不是 IDAB1_ERE_Q10011_A_302340000）");
	}

	/** ⑤ fail-closed：未登记别名必须抛稳定码异常，禁止静默成为死边。 */
	@Test
	void unresolvedAliasFailsClosed() throws Exception {
		String xml = "<quest_data_drivens><quest_data_driven><id>999999</id><name>Q999999</name>"
			+ "<dev_name>x</dev_name><category_acquire_>none</category_acquire_><reward_npc_name>X</reward_npc_name>"
			+ "<progress_info><data><category_progress_>EnterArea</category_progress_>"
			+ "<value0_progress_>NOT_A_REGISTERED_SENSORY_AREA</value0_progress_></data></progress_info>"
			+ "</quest_data_driven></quest_data_drivens>";
		DataDrivenQuestTable bogus = DataDrivenQuestTable.load(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> NativeEnterAreaPort.create(bogus, Set.of(999999), Set.of("SOME_OTHER_ZONE")));
		assertTrue(failure.getMessage().startsWith("DATA_DRIVEN_ENTER_AREA_ZONE_UNRESOLVED:"),
			"未解析别名必须使用稳定码: " + failure.getMessage());
	}

	/**
	 * ⑥ 活图归一（R5，2026-10-08）：原版把英吉斯温/格国的感官区只写在 `_M`（大师服）世界文件里
	 * （实读 `lf4`=0/`LF4_M`=15、`df4`=0/`DF4_M`=13；其余 254 个世界目录都在普通文件里），而镜像世界
	 * 不是玩家可达目标（`a7da0ad67` 活图迁移：传送/门户/任务/区域统一 210050000）⇒ DD 进区一律不得
	 * 注册在镜像世界；`_M` 行必须归一到活图 mapid，几何逐字不变（② 的胞数/摘要断言继续钉死几何）。
	 * 代表实例：10035 第 5 步（安格利浦关卡进区）曾注册在 210130000 ⇒ 活图英吉斯温死步。
	 */
	@Test
	void mirrorAuthoredEnterAreasRegisterOnTheLiveWorld() throws Exception {
		Map<String, String> worldNames = worldMapNames();
		int normalized = 0;
		for (EvidenceRow row : evidence.values()) {
			if (!"OK".equals(row.status())) {
				continue;
			}
			Map<String, String> zone = registeredZones.get(row.alias().toUpperCase(Locale.ROOT));
			assertTrue(zone != null, "登记缺失: " + row.alias());
			String host = worldNames.get(zone.get("mapid"));
			assertTrue(host != null,
				"注册宿主世界必须存在于 world_maps.xml: " + row.alias() + " -> " + zone.get("mapid"));
			assertFalse(host.contains("[Master Server]"),
				"DD 进区不得注册在镜像（大师服）世界（玩家不可达 ⇒ 活图死步）: " + row.alias() + " -> " + host);
			if (row.normalizedFrom().isEmpty()) {
				continue;
			}
			normalized++;
			String source = worldNames.get(row.normalizedFrom());
			assertTrue(source != null && source.contains("[Master Server]"),
				"归一行必须留痕原版镜像宿主: " + row.alias() + " -> " + row.normalizedFrom());
			assertEquals("210050000", zone.get("mapid"),
				"当前唯一归一行（LF4_M 感官区）的活图宿主 = 英吉斯温 210050000: " + row.alias());
		}
		assertEquals(1, normalized, "归一行数冻结（新增/消失都要按原版实读复核 LIVE_WORLD_MAPID）");
		// 代表任务 10035：安格利浦关卡感官区必须解析到活图注册的同名区，且端口绑定 = 同名。
		Map<String, String> zone = registeredZones.get("LF4_SENSORYAREA_Q10035A");
		assertTrue(zone != null, "10035 进区必须已注册（活图英吉斯温）");
		assertEquals("210050000", zone.get("mapid"), "安格利浦关卡感官区注册宿主 = 活图英吉斯温");
		assertEquals("210130000", evidence.get("LF4_SensoryArea_Q10035A").normalizedFrom(),
			"原版宿主（LF4_M = 210130000）必须留痕在台账 normalized_from");
		assertEquals("LF4_SENSORYAREA_Q10035A",
			port.zoneName(10035, enterAreaStepOf(10035, "LF4_SensoryArea_Q10035A")).orElseThrow()
				.toUpperCase(Locale.ROOT),
			"10035 第 5 步进区绑定 = 同名，且必须落在活图注册面");
	}

	private static int enterAreaStepOf(int questId, String aliasPrefix) {
		for (Step step : table.find(questId).orElseThrow().steps()) {
			if (step.kind() == Kind.ENTER_AREA && step.payload().startsWith(aliasPrefix)) {
				return step.index();
			}
		}
		throw new AssertionError("quest " + questId + " has no enter-area step " + aliasPrefix);
	}

	/** world_maps.xml 的 id → name 视图（镜像世界以 `[Master Server]` 命名）。 / World map id → name. */
	private static Map<String, String> worldMapNames() throws Exception {
		Map<String, String> names = new LinkedHashMap<>();
		for (Element map : elements(Path.of("src/main/resources/aion/data/static_data/world_maps.xml"), "map")) {
			names.put(map.getAttribute("id"), map.getAttribute("name"));
		}
		return names;
	}

	// ---------------------------------------------------------------- 复算辅助

	private static Set<String> upper(Set<String> values) {
		Set<String> result = new TreeSet<>();
		values.forEach(value -> result.add(value.toUpperCase(Locale.ROOT)));
		return result;
	}

	private static List<Path> zoneFiles() {
		try (Stream<Path> paths = Files.list(ZONES_DIR)) {
			return paths.filter(path -> path.getFileName().toString().startsWith("zones_"))
				.filter(path -> path.getFileName().toString().endsWith(".xml"))
				.sorted().toList();
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	/** 按 XML 原文取区名 / mapid / 胞数 / 摘要（与 Python 工具同口径，禁止浮点重排）。 / Raw-text zone view. */
	private static Map<String, Map<String, String>> loadZones(Path path) throws Exception {
		Map<String, Map<String, String>> zones = new TreeMap<>();
		for (org.w3c.dom.Element zone : elements(path, "zone")) {
			String name = zone.getAttribute("name");
			if (name.isBlank()) {
				continue;
			}
			List<String> cells = new ArrayList<>();
			for (org.w3c.dom.Element ring : children(zone, "points")) {
				List<String> points = new ArrayList<>();
				for (org.w3c.dom.Element point : children(ring, "point")) {
					points.add(point.getAttribute("x") + " " + point.getAttribute("y"));
				}
				cells.add(ring.getAttribute("bottom") + "|" + ring.getAttribute("top") + "|"
					+ String.join(",", points));
			}
			Map<String, String> view = new LinkedHashMap<>();
			view.put("name", name);
			view.put("mapid", zone.getAttribute("mapid"));
			view.put("cells", String.valueOf(cells.size()));
			view.put("digest", sha256(String.join(";", cells)).substring(0, 16));
			zones.put(name.toUpperCase(Locale.ROOT), view);
		}
		return zones;
	}

	private static List<org.w3c.dom.Element> elements(Path path, String tag) throws Exception {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		org.w3c.dom.Document document = factory.newDocumentBuilder().parse(path.toFile());
		org.w3c.dom.NodeList nodes = document.getElementsByTagName(tag);
		List<org.w3c.dom.Element> result = new ArrayList<>();
		for (int index = 0; index < nodes.getLength(); index++) {
			if (nodes.item(index) instanceof org.w3c.dom.Element element) {
				result.add(element);
			}
		}
		return result;
	}

	private static List<org.w3c.dom.Element> children(org.w3c.dom.Element parent, String tag) {
		List<org.w3c.dom.Element> result = new ArrayList<>();
		org.w3c.dom.NodeList nodes = parent.getChildNodes();
		for (int index = 0; index < nodes.getLength(); index++) {
			if (nodes.item(index) instanceof org.w3c.dom.Element element && tag.equals(element.getTagName())) {
				result.add(element);
			}
		}
		return result;
	}

	private static Map<String, EvidenceRow> loadEvidence() throws Exception {
		Map<String, EvidenceRow> rows = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource(EVIDENCE), StandardCharsets.UTF_8))) {
			String line;
			boolean header = true;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t", -1);
				if (header) {
					header = false;
					continue;
				}
				rows.put(cells[0], new EvidenceRow(cells[0], cells[1], cells[3], cells[5], cells[6], cells[7],
					Integer.parseInt(cells[8]), cells[9], cells.length > 15 ? cells[15] : ""));
			}
		}
		return rows;
	}

	private static Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new LinkedHashMap<>();
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			String questId = RetailLedgerRows.cell(row, "quest_id");
			owners.put(Integer.parseInt(questId), RetailLedgerRows.cell(row, "owner"));
		}
		return owners;
	}

	private static String sha256(String text) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder();
		for (byte value : digest) {
			hex.append(String.format("%02x", value));
		}
		return hex.toString();
	}

	private static InputStream resource(String name) {
		InputStream stream = DataDrivenEnterAreaPortGateTest.class.getResourceAsStream(name);
		if (stream == null) {
			throw new IllegalStateException("missing test resource " + name);
		}
		return stream;
	}
}
