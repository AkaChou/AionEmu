package com.aionemu.boot.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 行级保留的 properties 文档模型测试。
 * Tests for the line-preserving properties document model.
 */
class PropertiesDocumentTest {

	/**
	 * 改一个键后，其余每一行都必须逐字节不变。
	 * After changing one key, every other line must stay byte-identical.
	 */
	@Test
	void changingOneKeyLeavesEveryOtherLineUntouched() {
		String text = """
			# 管理员命令 / ADMIN COMMAND
			gameserver.administration.gmpanel = 3
			# 默认值: 3 / Default: 3
			gameserver.administration.gmlevel = 3
			""";

		PropertiesDocument document = PropertiesDocument.parse(text);
		String updated = document.withValue("gameserver.administration.gmlevel", "5").render();

		List<String> before = List.of(text.split("\n", -1));
		List<String> after = List.of(updated.split("\n", -1));
		assertEquals(before.size(), after.size(), "the line count must not change");
		for (int i = 0; i < before.size(); i++) {
			if (before.get(i).contains("gmlevel")) {
				assertEquals("gameserver.administration.gmlevel = 5", after.get(i));
			} else {
				assertEquals(before.get(i), after.get(i), "line " + i + " must not change");
			}
		}
	}

	/**
	 * 双语注释块必须原样保留：配置文件的注释承载运维语义。
	 * The bilingual comment blocks must be preserved verbatim: the comments in the configuration files carry
	 * operational meaning.
	 */
	@Test
	void bilingualCommentBlocksArePreserved() {
		String text = """
			# GM 等级 / GM Level
			# 默认值: 3 / Default: 3
			gameserver.administration.gmlevel = 3
			""";

		String updated = PropertiesDocument.parse(text).withValue("gameserver.administration.gmlevel", "5").render();

		assertTrue(updated.contains("# GM 等级 / GM Level"));
		assertTrue(updated.contains("# 默认值: 3 / Default: 3"));
	}

	/**
	 * 中文键与特殊字符键必须能正确解析与替换。
	 * Chinese keys and special-character keys must parse and replace correctly.
	 */
	@Test
	void chineseAndSpecialKeysAreHandled() {
		PropertiesDocument document = PropertiesDocument.parse("""
			移动 = 3
			gameserver.admin.tag.1 = \\uE042 Support \\uE043 %s
			""");

		Map<String, String> values = document.values();
		assertEquals("3", values.get("移动"));
		assertEquals("\\uE042 Support \\uE043 %s", values.get("gameserver.admin.tag.1"));

		String updated = document.withValue("移动", "5").render();
		assertTrue(updated.contains("移动 = 5"));
	}

	/**
	 * 键不存在时文档保持不变。
	 * The document stays unchanged when the key is absent.
	 */
	@Test
	void unknownKeyLeavesTheDocumentUnchanged() {
		String text = "a = 1\n";
		PropertiesDocument document = PropertiesDocument.parse(text);

		assertEquals(text, document.withValue("missing", "2").render());
		assertTrue(document.lineIndexOf("missing").isEmpty());
	}

	/**
	 * 同一键出现多次时，全部出现都必须被替换。
	 * Every occurrence of a repeated key must be replaced.
	 */
	@Test
	void repeatedKeysAreAllReplaced() {
		PropertiesDocument document = PropertiesDocument.parse("a = 1\nb = 2\na = 3\n");

		assertEquals(2, document.occurrencesOf("a"));
		assertEquals("a = 9\nb = 2\na = 9\n", document.withAllValues("a", "9").render());
	}

	/**
	 * 值两端的空白被去除，值内部的空白保留；写入时该行原有的空白原样保留，值只被就地替换。
	 * Whitespace around the value is stripped while whitespace inside it is kept; on write the line keeps its
	 * original whitespace and only the value is replaced in place.
	 */
	@Test
	void valueWhitespaceIsNormalizedButInnerWhitespaceIsKept() {
		PropertiesDocument document = PropertiesDocument.parse("k =   hello world   \n");

		assertEquals("hello world", document.values().get("k"));
		assertEquals("k =   hello world   \n", document.withValue("k", "hello world").render(),
			"the value is replaced in place, so the surrounding whitespace stays untouched");
	}

	/**
	 * 生产配置文件必须能被模型完整解析，且不丢键。
	 * The production configuration files must parse completely without losing keys.
	 */
	@Test
	void productionAdministrationFilesParseWithoutLosingKeys() throws Exception {
		Path administration = Path.of("src/main/resources/aion/config/administration");
		assertTrue(Files.isDirectory(administration), "the administration config directory must exist");

		int parsedFiles = 0;
		for (Path file : Files.list(administration).filter(p -> p.toString().endsWith(".properties")).toList()) {
			String text = Files.readString(file);
			PropertiesDocument document = PropertiesDocument.parse(text);
			assertEquals(text, document.render(), "rendering an untouched document must reproduce the file");
			parsedFiles++;
		}
		assertEquals(5, parsedFiles, "the administration directory must hold the five editable properties files");
	}
}
