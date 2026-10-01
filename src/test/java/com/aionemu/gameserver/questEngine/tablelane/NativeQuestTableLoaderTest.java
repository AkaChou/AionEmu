package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader.SimpleTalkRow;

/**
 * SimpleHunt 表装载器门测试（夹具事实来自 P1 对拍：1863 活跃行、13912 槽 3 空名单、37116 跨行
 * monster 名单、4 行零计数休眠行）。 / SimpleHunt table loader gate tests (fixture facts from the P1
 * reconciliation: 1863 active rows, 13912's empty slot-3 list, 37116's multi-line monster list,
 * the 4 zero-count dormant rows).
 */
class NativeQuestTableLoaderTest {

	private static final int EXPECTED_ROWS = 1863;
	/** 真端休眠零计数行（相机行必须拒绝派生）。 / Retail-dormant zero-count rows (camera derivation must refuse). */
	private static final Set<Integer> ZERO_COUNT_ROWS = Set.of(11013, 11014, 11208, 11209);

	@Test
	void loadsFullSimpleHuntTable() {
		assertEquals(EXPECTED_ROWS, NativeQuestTableLoader.instance().size());
	}

	@Test
	void loadsFullSimpleSerialHuntTable() {
		assertEquals(16, NativeQuestTableLoader.instance().serialHuntRows().size());
		NativeQuestTableLoader.SimpleSerialHuntRow row30600 = NativeQuestTableLoader.instance().requireSerial(30600);
		assertEquals("Hejitor", row30600.acquiredNpcName());
		assertEquals("Hejitor", row30600.rewardNpcName());
		assertEquals(List.of("Linocus"), row30600.talkNpcNames());
		assertEquals(2, row30600.stages().size());
		assertEquals(1, row30600.stages().get(0).count());
		assertEquals(1, row30600.stages().get(1).count());

		CameraRegistry.RowSpec spec = NativeQuestTableLoader.instance().cameraSpec(row30600);
		assertEquals(30600, spec.questId());
		assertEquals(RawQuestVarsCodec.Width.SIX, spec.width());
		assertEquals((1 << 0) | (1 << 6), spec.fullValue());
	}

	@Test
	void spotChecksRetailRows() {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		// P0a 相机锚：2354 = 6 位 {槽1:6, 槽2:4}。 / P0a camera anchor: 2354 = 6-bit {slot1:6, slot2:4}.
		NativeQuestTableLoader.SimpleHuntRow row2354 = loader.require(2354);
		assertEquals(Map.of(1, 6, 2, 4), row2354.killSlots().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().count())));
		assertTrue(!row2354.acquiredNpcName().isBlank());
		assertTrue(!row2354.rewardNpcName().isBlank());
		// 13912 形：槽 1 有怪，槽 3 有计数无怪（真端自身无事件源，行为永不满足）。 / The 13912 shape:
		// slot 1 has monsters; slot 3 has a count and no monsters (retail wires no event source either).
		NativeQuestTableLoader.SimpleHuntRow row13912 = loader.require(13912);
		assertEquals(List.of(), row13912.killSlots().get(3).monsters());
		assertEquals(1, row13912.killSlots().get(3).count());
		assertTrue(row13912.killSlots().get(1).monsters().size() > 0);
		// 37116 的 monster 名单在源表跨行，装载必须归一内部空白。 / 37116's monster list spans lines in
		// the source table; loading must normalize inner whitespace.
		assertEquals(12, loader.require(37116).killSlots().get(1).monsters().size());
	}

	@Test
	void derivesCameraSpecsMatchingTheScriptMatrix() {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		// 13912：表推导 fullValue 0x1001 == 脚本字面 fullValue（P0a 矩阵该行 BAD 只是脚本调用集
		// 缺槽 3 事件源，表推导如实含槽 3）。 / 13912: table-derived fullValue 0x1001 equals the
		// script's literal fullValue (the P0a matrix marked this row BAD only because the script's
		// call set lacks a slot-3 event source; the table derivation carries slot 3 faithfully).
		CameraRegistry.RowSpec spec13912 = loader.cameraSpec(loader.require(13912));
		assertEquals(RawQuestVarsCodec.Width.SIX, spec13912.width());
		assertEquals(0x1001, spec13912.fullValue());
		assertEquals(Map.of(1, 1, 3, 1), spec13912.slotRequires());
		// 1842：10 位锚 {槽1:80, 槽2:1} → 0x450（fun_763.cpp:958 / fun_760.cpp:643）。
		// 1842: the 10-bit anchor {slot1:80, slot2:1} → 0x450.
		CameraRegistry.RowSpec spec1842 = loader.cameraSpec(loader.require(1842));
		assertEquals(RawQuestVarsCodec.Width.TEN, spec1842.width());
		assertEquals(0x450, spec1842.fullValue());
		// 10 位全表无槽 4/5（对拍实证），width.shift 对越界槽的拒绝保持 fail-closed 语义。
		// No 10-bit row uses slots 4/5 (reconciled evidence); width.shift keeps failing closed on
		// out-of-range slots.
	}

	@Test
	void zeroCountDormantRowsNeverDeriveCameraRows() {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		Set<Integer> zeroCount = new TreeSet<>();
		for (NativeQuestTableLoader.SimpleHuntRow row : loader.rows()) {
			if (row.killSlots().isEmpty()) {
				zeroCount.add(row.questId());
			}
		}
		assertEquals(ZERO_COUNT_ROWS, zeroCount);
		for (int questId : ZERO_COUNT_ROWS) {
			IllegalStateException rejected = assertThrows(IllegalStateException.class,
					() -> loader.cameraSpec(loader.require(questId)));
			assertTrue(rejected.getMessage().startsWith("NATIVE_CAMERA_ROW_MISSING"), rejected.getMessage());
		}
	}

	@Test
	void wholeTableRegistersIntoTheCameraRegistry() {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		List<CameraRegistry.RowSpec> specs = loader.rows().stream()
				.filter(row -> !row.killSlots().isEmpty())
				.map(loader::cameraSpec)
				.toList();
		CameraRegistry registry = CameraRegistry.fromSpecs(specs);
		// 1859 = 1863 - 4 零计数休眠行；全表零拒绝（无混宽/无超掩码/无 fullValue 矛盾）。
		// 「1859 之中哪些行接入运行时」由切换批按脚本接线集决定（对拍证据：1812 接线 / 47+4 休眠，
		// 见 p1/tools/reconcile_hunt_table_vs_camera.py 输出），不在本测试复刻该数据集。
		// 1859 = 1863 minus the 4 zero-count dormant rows; the whole table registers without a single
		// rejection (no width mixing, no over-mask, no fullValue conflicts). Which of the 1859 rows
		// reach the runtime is the switch batch's call per the script-wired set (reconciliation
		// evidence: 1812 wired / 47+4 dormant, see p1/tools/reconcile_hunt_table_vs_camera.py
		// output) — this test deliberately does not replicate that data set.
		assertEquals(EXPECTED_ROWS - ZERO_COUNT_ROWS.size(), registry.size());
	}

	/**
	 * SimpleTalk 行模型装载门（P3 第一步）：真端 3152 行全量、双 NPC 结构 100%、列填充率与表侧实测一致。
	 * SimpleTalk row-model gate (P3 step one): all 3152 retail rows load; the two-NPC shape is total;
	 * column fill rates match the measured table facts.
	 */
	@Test
	void loadsFullSimpleTalkTable() {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		assertEquals(3152, loader.talkSize());
		List<SimpleTalkRow> rows = List.copyOf(loader.talkRows());
		assertTrue(rows.stream().allMatch(row ->
			!row.acquiredNpcName().isBlank() && !row.rewardNpcName().isBlank()),
			"真端 3152/3152 行都带接取与交付 NPC / every retail row carries both NPCs");
		assertEquals(468, rows.stream().filter(row -> !row.talkNpcNames().isEmpty()).count(),
			"talk_npc1..3 中继链行数 / relay-chain rows");
		assertEquals(1988, rows.stream().filter(SimpleTalkRow::itemCheck).count(),
			"item_check 行数 / work-item check rows");
		assertEquals(492, rows.stream().filter(row -> row.conQuest() != null).count(),
			"con_quest 行数 / chain-acquire rows");
	}

	/** 长尾列按原文装载并按真端分槽：give_item = 接取侧，give_itemK/remove_itemK = 第 K 步。 */
	@Test
	void talkRowsKeepRetailTextForLongTailColumns() {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();

		// 1131 与真端 cab520/cabb10 立即数逐字节对拍：接取发 1131A；步 1 发 1131B 且扣 1131A。
		SimpleTalkRow single = loader.requireTalk(1131);
		assertEquals("Hyacinte", single.acquiredNpcName());
		assertEquals("Nadaelo", single.rewardNpcName());
		assertEquals(List.of("Shugo_LF1a_01"), single.talkNpcNames());
		assertEquals(1132, single.conQuest());
		assertEquals("ITEM_QUEST_1131A 1", single.acceptGiveItem());
		assertEquals(Arrays.asList("ITEM_DOC_QUEST_1131B 1", null, null), single.stepGiveItems());
		assertEquals(Arrays.asList("ITEM_QUEST_1131A 1", null, null), single.stepRemoveItems());
		assertFalse(single.itemCheck(), "1131 无 item_check / 1131 declares no item_check");

		// 41536 的中继链发放按下标对齐（give_item1/2/3 ↔ talk_npc1/2/3）。
		SimpleTalkRow chained = loader.requireTalk(41536);
		assertEquals(List.of("LDF4b_FOBJ_T4_TombstoneA", "LDF4b_FOBJ_T4_TombstoneB", "LDF4b_FOBJ_T4_TombstoneC"),
			chained.talkNpcNames());
		assertEquals(List.of("ITEM_QUEST_41536A 1", "ITEM_QUEST_41536B 1", "ITEM_QUEST_41536C 1"),
			chained.stepGiveItems());
		assertEquals(Arrays.asList(null, null, null), chained.stepRemoveItems());
		assertNull(chained.acceptGiveItem(), "41536 无 give_item / no accept-side grant");
		assertTrue(chained.itemCheck());
		assertNull(chained.conQuest(), "41536 无 con_quest / no chain-acquire");

		SimpleTalkRow cutscene = loader.requireTalk(1941);
		assertEquals(93, cutscene.cutsceneId());
		assertEquals(1009, cutscene.cutsceneAction());
	}

	/** 双 NPC 缺一即 fail-closed（真端不存在此形）。 / A missing NPC fails closed: retail has no such shape. */
	@Test
	void talkRowsFailClosedWhenAnNpcIsMissing() throws IOException {
		IllegalStateException missingReward = assertThrows(IllegalStateException.class,
			() -> NativeQuestTableLoader.parse(huntStream(), serialStream(), new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quest_simpletalks>
					<id id="9001"><acquired_npc_name>a</acquired_npc_name></id>
				</quest_simpletalks>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(missingReward.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"), missingReward.getMessage());
		// 非数字 con_quest 同为 fail-closed。
		IllegalStateException badConQuest = assertThrows(IllegalStateException.class,
			() -> NativeQuestTableLoader.parse(huntStream(), serialStream(), new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quest_simpletalks>
					<id id="9002"><acquired_npc_name>a</acquired_npc_name><reward_npc_name>b</reward_npc_name>
						<con_quest>soon</con_quest></id>
				</quest_simpletalks>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(badConQuest.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"), badConQuest.getMessage());
	}

	private static InputStream huntStream() {
		InputStream stream = NativeQuestTableLoaderTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml");
		assertNotNull(stream, "missing Quest_SimpleHunt.xml");
		return stream;
	}

	private static InputStream serialStream() {
		InputStream stream = NativeQuestTableLoaderTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/retail/Quest_SimpleSerialHunt.xml");
		assertNotNull(stream, "missing Quest_SimpleSerialHunt.xml");
		return stream;
	}

	@Test
	void negativeCasesFailClosed() throws IOException {
		// 重复任务 id。 / Duplicate quest id.
		IllegalStateException duplicate = assertThrows(IllegalStateException.class, () -> NativeQuestTableLoader
				.parse(new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quest_simplehunts>
					<id id="9001"><acquired_npc_name>a</acquired_npc_name><reward_npc_name>a</reward_npc_name>
						<count1>3</count1><monster1>m_a</monster1></id>
					<id id="9001"><acquired_npc_name>b</acquired_npc_name><reward_npc_name>b</reward_npc_name>
						<count1>3</count1><monster1>m_b</monster1></id>
				</quest_simplehunts>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(duplicate.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"), duplicate.getMessage());
		// 有 monster 无 count：真端不存在此形。 / Monster without count: no such retail shape.
		IllegalStateException missingCount = assertThrows(IllegalStateException.class,
				() -> NativeQuestTableLoader.parse(new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quest_simplehunts>
					<id id="9002"><acquired_npc_name>a</acquired_npc_name><reward_npc_name>a</reward_npc_name>
						<count1>3</count1><monster1>m_a</monster1><monster2>m_b</monster2></id>
				</quest_simplehunts>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(missingCount.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"),
				missingCount.getMessage());
		// count 非数字。 / Non-numeric count.
		IllegalStateException garbage = assertThrows(IllegalStateException.class,
				() -> NativeQuestTableLoader.parse(new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quest_simplehunts>
					<id id="9003"><acquired_npc_name>a</acquired_npc_name><reward_npc_name>a</reward_npc_name>
						<count1>many</count1><monster1>m_a</monster1></id>
				</quest_simplehunts>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(garbage.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"), garbage.getMessage());
	}
}
