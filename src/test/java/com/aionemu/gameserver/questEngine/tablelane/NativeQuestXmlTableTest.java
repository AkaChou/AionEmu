package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * quest.xml 元数据装载器门测试（夹具事实来自入仓表实读：10035 行、行 1007 的
 * fighter_selectable_item ×3、行 1000 的门字段）。 / quest.xml metadata loader gate tests
 * (fixture facts from live table reads: 10035 rows, quest 1007's three fighter_selectable_item
 * entries, quest 1000's gate fields).
 */
class NativeQuestXmlTableTest {

	private static final int EXPECTED_ROWS = 10035;

	@Test
	void loadsFullQuestXmlTable() {
		assertEquals(EXPECTED_ROWS, NativeQuestXmlTable.instance().size());
	}

	@Test
	void typedAccessorsReadDirectFields() {
		NativeQuestXmlTable.QuestRow row1000 = NativeQuestXmlTable.instance().require(1000);
		assertEquals("Q1000", row1000.text("name"));
		assertEquals("STR_QUEST_NAME_Q1000", row1000.text("desc"));
		assertEquals(Integer.valueOf(1), row1000.integer("minlevel_permitted"));
		assertEquals(Integer.valueOf(1), row1000.integer("max_repeat_count"));
		assertTrue(row1000.bool("cannot_giveup"));
		assertTrue(row1000.bool("cannot_share"));
		// 缺字段三态：text=""、integer=null、list=[]。 / Absent-field triple: "", null, [].
		assertEquals("", row1000.text("no_such_field"));
		assertNull(row1000.integer("no_such_field"));
		assertEquals(java.util.List.of(), row1000.list("no_such_field"));
	}

	@Test
	void repeatedDirectChildFieldsKeepFirstOccurrenceOrder() throws IOException {
		// 现网数据直接子元素全部单值；多值契约用合成流验证（首现序 + 首值访问器）。
		// Live data has only single-valued direct children; the multi-value contract is proven on
		// a synthetic stream (first-occurrence order + first-value accessor).
		NativeQuestXmlTable table = NativeQuestXmlTable.parse(new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quests>
					<quest><id>7</id><token>first</token><token>second</token><token>third</token></quest>
				</quests>
				""".getBytes(StandardCharsets.UTF_8)));
		NativeQuestXmlTable.QuestRow row = table.require(7);
		assertEquals(java.util.List.of("first", "second", "third"), row.list("token"));
		assertEquals("first", row.text("token"));
		// 真实行：单值字段走同一条 list 通路。 / Live row: single-valued fields use the same list path.
		assertEquals(1, NativeQuestXmlTable.instance().require(1000).list("cannot_giveup").size());
	}

	@Test
	void classPermittedFieldIsPreservedVerbatim() {
		NativeQuestXmlTable.QuestRow row85 = NativeQuestXmlTable.instance().require(85);
		// 真端 17 职业令牌空格分隔；字段保真不切分（切分语义归切换批）。 / 17 retail class tokens
		// space-separated; the field is preserved verbatim (splitting is a switch-batch concern).
		assertEquals("warrior scout mage cleric engineer artist fighter knight assassin ranger wizard "
				+ "elementallist chanter priest gunner bard rider", row85.text("class_permitted"));
	}

	@Test
	void negativeCasesFailClosed() throws IOException {
		// 重复任务 id。 / Duplicate quest id.
		IllegalStateException duplicate = assertThrows(IllegalStateException.class,
				() -> NativeQuestXmlTable.parse(new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quests>
					<quest><id>1</id><name>a</name></quest>
					<quest><id>1</id><name>b</name></quest>
				</quests>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(duplicate.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"), duplicate.getMessage());
		// 行缺 id。 / Row without id.
		IllegalStateException missingId = assertThrows(IllegalStateException.class,
				() -> NativeQuestXmlTable.parse(new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<quests><quest><name>a</name></quest></quests>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(missingId.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"), missingId.getMessage());
		// 根标签不符。 / Wrong root tag.
		IllegalStateException wrongRoot = assertThrows(IllegalStateException.class,
				() -> NativeQuestXmlTable.parse(new ByteArrayInputStream("""
				<?xml version="1.0" encoding="UTF-8"?>
				<wrong><quest><id>1</id></quest></wrong>
				""".getBytes(StandardCharsets.UTF_8))));
		assertTrue(wrongRoot.getMessage().startsWith("NATIVE_TABLE_PARSE_FAILED"), wrongRoot.getMessage());
	}
}
