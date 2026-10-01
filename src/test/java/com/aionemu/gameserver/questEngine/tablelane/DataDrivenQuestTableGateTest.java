package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.retail.RetailDataDrivenTable;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.ExtraAction;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Kind;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Row;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable.Step;

/**
 * P7 步 2 门：DD **原生行模型**（{@link DataDrivenQuestTable}）与真端表 / 旧视图 / 真端列 guard 的一致性。
 * <p>
 * ① 行集与步序必须与旧视图（`RetailDataDrivenTable`，仍然服务 IR 车道）逐行一致——同一张真端表，
 * 两个解析器不得漂移；② 切换集（owner `RETAIL_TABLE` ∧ 不在客户端孤行桶）的类别步数与 P7 步 1
 * 契约冻结一致；③ 附加动作分类必须等于真端 `LoadExtraAction` 按列号的裁定（列 1 发 / 2 扣 / 3 传送 /
 * 4 过场 / 5 生成 / 6 延迟 / 7 与 8 消息 / 9 进副本 / 10 定时器），且 CollectItem/PvP 无附加动作面；
 * ④ 非法类别 / 缺载荷 / 非法列组合必须 fail-closed（稳定码）。
 * <p>
 * P7 step 2 gate for the native DataDriven row model: agreement with the legacy view and the retail table,
 * the frozen switch-set histograms, the retail extra-action guard, and fail-closed validation.
 */
class DataDrivenQuestTableGateTest {

	private static final String DD_TABLE = "/aion/data/static_data/quest/retail/data_driven_quest.xml";
	private static final String RETENTION = "/quest/retail-xml-retention.tsv";
	private static final String ABSENT_FIXTURE = "/quest/retail-data-driven-client-absent.tsv";

	/** 真端 DD 活行（注释块不是行）。 / Live retail DD rows. */
	private static final int LIVE_ROWS = 2492;
	/** P7 切换集（owner `RETAIL_TABLE` ∧ 不在孤行/注释桶）。 / The P7 switch set. */
	private static final int SWITCH_ROWS = 1467;
	/** 每类 handler 的步数（P7 步 1 契约冻结）。 / Frozen per-kind step counts. */
	private static final Map<String, Integer> CATEGORY_STEPS = Map.of(
		"hunt", 827, "collectitem", 348, "pvp", 207, "talk", 403,
		"enterarea", 153, "itemplay", 42, "enterworld", 34, "talkfobj", 19);

	private static DataDrivenQuestTable nativeTable;
	private static RetailDataDrivenTable legacyTable;
	private static Set<Integer> switchSet;

	@BeforeAll
	static void loadFixtures() throws Exception {
		nativeTable = DataDrivenQuestTable.load(resource(DD_TABLE));
		legacyTable = RetailDataDrivenTable.load(resource(DD_TABLE));
		Map<Integer, String> owners = retentionOwners();
		Set<Integer> excluded = fixtureBucket("CLIENT_ABSENT_LIVE");
		excluded.addAll(fixtureBucket("COMMENTED_OUT"));
		switchSet = new TreeSet<>();
		for (int questId : nativeTable.questIds()) {
			if ("RETAIL_TABLE".equals(owners.get(questId)) && !excluded.contains(questId)) {
				switchSet.add(questId);
			}
		}
	}

	/** ① 原生模型与旧视图逐行一致（同一真端表，两个解析器不得漂移）。 */
	@Test
	void nativeModelAgreesWithTheLegacyViewRowByRow() {
		assertEquals(LIVE_ROWS, nativeTable.size(), "真端 DD 活行数冻结");
		assertEquals(legacyTable.questIds(), nativeTable.questIds(), "行集必须一致");
		for (int questId : nativeTable.questIds()) {
			Row row = nativeTable.find(questId).orElseThrow();
			RetailDataDrivenTable.Entry legacy = legacyTable.find(questId).orElseThrow();
			assertEquals(legacy.stepCategories(), row.steps().stream().map(Step::kind).map(Kind::tableName).toList(),
				"步序（类别序列）必须一致: " + questId);
			assertEquals(legacy.acquireCategory().toLowerCase(java.util.Locale.ROOT), row.acquireKind(),
				"接取类别必须一致: " + questId);
			assertEquals(legacy.rewardNpc(), row.rewardNpc(), "领奖 NPC 必须一致: " + questId);
			for (int index = 0; index < row.steps().size(); index++) {
				assertEquals(index, row.steps().get(index).index(), "步号 = 表序位置: " + questId);
			}
		}
	}

	/** ② 切换集与 P7 步 1 契约冻结的类别步数一致。 */
	@Test
	void switchSetMatchesTheFrozenHistogram() {
		assertEquals(SWITCH_ROWS, switchSet.size(), "切换集规模冻结");
		Map<String, Integer> categorySteps = new TreeMap<>();
		for (int questId : switchSet) {
			for (Step step : nativeTable.find(questId).orElseThrow().steps()) {
				categorySteps.merge(step.kind().tableName(), 1, Integer::sum);
			}
		}
		assertEquals(CATEGORY_STEPS, categorySteps, "每类 handler 步数必须与 P7 步 1 冻结一致");
	}

	/** ③ 附加动作分类 = 真端 `LoadExtraAction` 列裁定；CollectItem/PvP 无附加动作面。 */
	@Test
	void extraActionsFollowTheRetailGuard() {
		Map<Integer, ExtraAction> byColumn = new LinkedHashMap<>();
		byColumn.put(1, ExtraAction.GIVE_ITEMS);
		byColumn.put(2, ExtraAction.REMOVE_ITEMS);
		byColumn.put(3, ExtraAction.TELEPORT);
		byColumn.put(4, ExtraAction.CUTSCENE);
		byColumn.put(5, ExtraAction.SPAWN);
		byColumn.put(6, ExtraAction.DELAY);
		byColumn.put(7, ExtraAction.MESSAGE);
		byColumn.put(8, ExtraAction.MESSAGE);
		byColumn.put(9, ExtraAction.ENTER_INSTANCE);
		byColumn.put(10, ExtraAction.TIMER);
		for (int questId : switchSet) {
			Row row = nativeTable.find(questId).orElseThrow();
			for (Step step : row.steps()) {
				List<ExtraAction> expected = new java.util.ArrayList<>();
				for (Map.Entry<Integer, String> column : new TreeMap<>(step.columns()).entrySet()) {
					if (isPayloadColumn(step.kind(), column.getKey())) {
						continue;
					}
					ExtraAction action = byColumn.get(column.getKey());
					assertNotNull(action, "非载荷列必须是真端附加动作列: " + questId + " " + column.getKey());
					expected.add(action);
				}
				assertEquals(expected, step.extraActions(), "附加动作分类必须与真端列裁定一致: " + questId);
				if (step.kind() == Kind.COLLECT_ITEM || step.kind() == Kind.PVP) {
					assertTrue(step.extraActions().isEmpty(),
						"CollectItem/PvP 无附加动作面（真端 return 1）: " + questId);
				}
			}
		}
	}

	/** ④ 非法类别 / 缺载荷 / 非法列组合 fail-closed（稳定码）。 */
	@Test
	void illegalShapesFailClosed() {
		IllegalStateException unknownKind = assertThrows(IllegalStateException.class,
			() -> parse(synthetic("<category_progress_>Fishing</category_progress_>"
				+ "<value0_progress_>x</value0_progress_>")));
		assertTrue(unknownKind.getMessage().startsWith("DATA_DRIVEN_STEP_KIND_UNKNOWN"), unknownKind.getMessage());

		IllegalStateException missingPayload = assertThrows(IllegalStateException.class,
			() -> parse(synthetic("<category_progress_>Hunt</category_progress_>"
				+ "<value4_progress_>Cutscene 1</value4_progress_>")));
		assertTrue(missingPayload.getMessage().startsWith("DATA_DRIVEN_STEP_PAYLOAD_MISSING"),
			missingPayload.getMessage());

		IllegalStateException illegalColumn = assertThrows(IllegalStateException.class,
			() -> parse(synthetic("<category_progress_>CollectItem</category_progress_>"
				+ "<value0_progress_>Taranis</value0_progress_><value6_progress_>100</value6_progress_>")));
		assertTrue(illegalColumn.getMessage().startsWith("DATA_DRIVEN_STEP_COLUMN_ILLEGAL"),
			illegalColumn.getMessage());

		IllegalStateException huntExtra = assertThrows(IllegalStateException.class,
			() -> parse(synthetic("<category_progress_>Hunt</category_progress_>"
				+ "<value0_progress_>mob 1;</value0_progress_><value6_progress_>100</value6_progress_>")));
		assertTrue(huntExtra.getMessage().startsWith("DATA_DRIVEN_STEP_COLUMN_ILLEGAL"), huntExtra.getMessage());
	}

	/** 该列在该类别里是类别载荷（真端 `FUN_180c4b980`）而非附加动作。 / Whether the column is a category payload. */
	private static boolean isPayloadColumn(Kind kind, int column) {
		return switch (kind) {
			case COLLECT_ITEM -> column >= 0 && column <= 5;
			case PVP -> column >= 0 && column <= 3;
			default -> column == 0;
		};
	}

	// ---------------------------------------------------------------- 夹具

	private static DataDrivenQuestTable parse(String body) {
		try {
			return DataDrivenQuestTable.load(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			if (e instanceof RuntimeException runtime) {
				throw runtime;
			}
			throw new AssertionError(e);
		}
	}

	private static String synthetic(String dataBody) {
		return "<quest_data_drivens><quest_data_driven><id>1</id><name>Q1</name>"
			+ "<category_acquire_>Talk</category_acquire_><value0_acquire_>Npc</value0_acquire_>"
			+ "<reward_npc_name>Npc</reward_npc_name><progress_info><data>" + dataBody
			+ "</data></progress_info></quest_data_driven></quest_data_drivens>";
	}

	private static Set<Integer> fixtureBucket(String bucketName) throws Exception {
		Set<Integer> bucket = new TreeSet<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource(ABSENT_FIXTURE), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t", -1);
				if (cells[0].equals("quest_id")) {
					continue;
				}
				if (cells[6].equals(bucketName)) {
					bucket.add(Integer.parseInt(cells[0]));
				}
			}
		}
		return bucket;
	}

	private static Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource(RETENTION), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t", -1);
				owners.putIfAbsent(Integer.parseInt(cells[0]), cells[1]);
			}
		}
		return owners;
	}

	private static InputStream resource(String path) {
		InputStream input = DataDrivenQuestTableGateTest.class.getResourceAsStream(path);
		assertNotNull(input, "missing resource " + path);
		return input;
	}
}
