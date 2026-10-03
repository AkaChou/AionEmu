package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Test;

/**
 * retail 表 schema 门（2026-10-03 DOCTYPE 剥离批 + 台账 XML 化批合并）：真资源对同目录 XSD 全文校验。
 * <p>
 * 承重面 = 每个 XSD 镜像其装载器的 fail-closed 规则 ∪ 观察形全域钉（口径与证据见
 * {@code .agents/summary/quest-engine-native/p0b/retail-doctype-xsd.zh-CN.md} 与
 * {@code .agents/summary/quest-engine-native/p0b/ledger-xml.zh-CN.md}）——schema 形漂移即红；
 * 另两钉子：①十八表不得再出现 DOCTYPE（剥离批不得回退）；②正文实体引用只允许 XML 预定义五实体
 * （非预定义实体在无 DTD 时是未定义实体炸解析，且真端自名实体语义 = 字面量展开，见批证据
 * `&hellip;` → 字面 `hellip`）。九张自造台账（原 TSV + D1 的 Quest-AI 注册面证据表）在本门与十张真端表同规：
 * 同目录同名 xml/xsd 对 + 同两条钉子。
 * <p>
 * Gate for the retail tables (the 2026-10-03 DOCTYPE strip plus the ledger-XML batch): the real
 * resources must validate against their sibling XSDs, which mirror the loader fail-closed rules
 * plus observed-universe shape pins (evidence under
 * {@code .agents/summary/quest-engine-native/p0b/}). Two further pins: no DOCTYPE may reappear,
 * and entity references in the bodies may only use the five XML predefined entities. The nine
 * self-made ledgers (formerly TSVs, plus the D1 Quest-AI registration evidence table) follow the
 * same rules as the ten retail tables: same-stem xml/xsd pairs and the same two pins.
 */
class RetailTableSchemaGateTest {

	private static final String DIR = "aion/data/static_data/quest/retail/";

	/** 十九张表 = (xml, xsd) 同名对（十张真端表 + 九张台账）。 / The nineteen same-stem (xml, xsd) pairs. */
	private static final List<Table> TABLES = List.of(
			new Table("Quest_SimpleHunt"),
			new Table("Quest_SimpleSerialHunt"),
			new Table("Quest_SimpleTalk"),
			new Table("Quest_SimpleCollectItem"),
			new Table("Quest_SimpleUseItem"),
			new Table("Quest_SimpleItemPlay"),
			new Table("Quest_CombineTask"),
			new Table("quest"),
			new Table("data_driven_quest"),
			new Table("npcfactions_quest"),
			new Table("quest_client_handin_npc_sets"),
			new Table("quest_legacy_heal_rows"),
			new Table("quest_name_string_ids"),
			new Table("retail-instance-entry-points"),
			new Table("retail-npc-name-aliases"),
			new Table("retail-quest-ai-name-groups"),
			new Table("retail-quest-ai-registrations"),
			new Table("retail-quest-string-ids"),
			new Table("retail-xml-retention"));

	private static final Set<String> PREDEFINED = Set.of("amp", "lt", "gt", "apos", "quot");
	private static final Pattern ENTITY_REF = Pattern.compile("&([A-Za-z][A-Za-z0-9]*);");

	@Test
	void realResourcesValidateAgainstTheirSiblingXsd() throws Exception {
		for (Table table : TABLES) {
			Validator validator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
					.newSchema(new StreamSource(resource(table.xsd())))
					.newValidator();
			try (InputStream input = resource(table.xml())) {
				validator.validate(new StreamSource(input));
			} catch (org.xml.sax.SAXException e) {
				throw new AssertionError(table.xml() + " violates " + table.xsd() + ": " + e.getMessage(), e);
			}
		}
	}

	@Test
	void retailTablesCarryNoDoctypeAndOnlyPredefinedEntities() throws IOException {
		for (Table table : TABLES) {
			String text = new String(resource(table.xml()).readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(!text.contains("<!DOCTYPE"),
					table.xml() + " must not carry a DOCTYPE (strip batch regression?)");
			Matcher matcher = ENTITY_REF.matcher(text);
			while (matcher.find()) {
				String name = matcher.group(1);
				assertTrue(PREDEFINED.contains(name), table.xml() + " has a non-predefined entity &"
						+ name + "; — undefined without a DTD and a hard parse failure");
			}
		}
	}

	private InputStream resource(String name) {
		InputStream input = getClass().getClassLoader().getResourceAsStream(DIR + name);
		assertTrue(input != null, "missing " + DIR + name);
		return input;
	}

	private record Table(String stem) {
		String xml() {
			return stem + ".xml";
		}

		String xsd() {
			return stem + ".xsd";
		}
	}
}
