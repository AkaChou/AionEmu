package com.aionemu.boot.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 配置键备注解析测试。
 * Tests for the configuration key comment parser.
 */
class ConfigCommentParserTest {

	/**
	 * 键上方的双语注释被拆成中文说明、英文说明与默认值。
	 * The bilingual comment above a key is split into a Chinese description, an English description and the
	 * default value.
	 */
	@Test
	void bilingualCommentIsSplitIntoDescriptionAndDefault() {
		String text = """
			# GM 面板 / GM Panel
			# 默认值: 3 / Default: 3
			gameserver.administration.gmpanel = 3
			""";

		Map<String, ConfigKeyComment> comments = ConfigCommentParser.parse(text);

		ConfigKeyComment comment = comments.get("gameserver.administration.gmpanel");
		assertEquals("GM 面板", comment.description());
		assertEquals("GM Panel", comment.descriptionEn());
		assertEquals("3", comment.defaultValue());
	}

	/**
	 * 默认值里含斜杠时不被截断。
	 * A default value containing a slash is not truncated.
	 */
	@Test
	void defaultValueWithSlashIsNotTruncated() {
		String text = """
			# 掉落目录 / Drop directory
			# 默认值: data/drops / Default: data/drops
			gameserver.drop.directory = data/drops
			""";

		ConfigKeyComment comment = ConfigCommentParser.parse(text)
			.get("gameserver.drop.directory");

		assertEquals("data/drops", comment.defaultValue());
		assertEquals("掉落目录", comment.description());
		assertEquals("Drop directory", comment.descriptionEn());
	}

	/**
	 * 键没有注释时不出现在结果里。
	 * A key without a comment does not show up in the result.
	 */
	@Test
	void keyWithoutCommentIsAbsent() {
		String text = """
			gameserver.administration.gmpanel = 3
			""";

		assertFalse(ConfigCommentParser.parse(text).containsKey("gameserver.administration.gmpanel"));
	}

	/**
	 * 注释块里的空行不打断说明与键的关联。
	 * A blank line inside a comment block does not break the link between the description and the key.
	 */
	@Test
	void blankLineInsideCommentBlockKeepsTheDescription() {
		String text = """
			# GM 面板配置 / GM PANEL Config's:
			#
			# 技能面板 / SKILL PANEL
			# 默认值: 3 / Default: 3
			gameserver.administration.skilpanel = 3
			""";

		ConfigKeyComment comment = ConfigCommentParser.parse(text)
			.get("gameserver.administration.skilpanel");

		assertEquals("技能面板", comment.description());
		assertEquals("SKILL PANEL", comment.descriptionEn());
		assertEquals("3", comment.defaultValue());
	}

	/**
	 * 只有说明没有默认值时，默认值为空。
	 * A description without a default value leaves the default value empty.
	 */
	@Test
	void descriptionWithoutDefaultValue() {
		String text = """
			# 管理员选项 / Admin Options:
			gameserver.administration.gm.mode.connection = false
			""";

		ConfigKeyComment comment = ConfigCommentParser.parse(text)
			.get("gameserver.administration.gm.mode.connection");

		assertEquals("管理员选项", comment.description());
		assertNull(comment.defaultValue());
	}

	/**
	 * 真实的 administration 配置文件必须能被解析出双语说明。
	 * The real administration configuration files must yield bilingual descriptions.
	 * @throws Exception 读取失败 / when reading fails
	 */
	@Test
	void realAdministrationFilesYieldBilingualDescriptions() throws Exception {
		Path file = Path.of("src/main/resources/aion/config/administration/admin.properties");
		assumeTrue(Files.isRegularFile(file));

		Map<String, ConfigKeyComment> comments = ConfigCommentParser.parse(Files.readString(file));

		ConfigKeyComment gmpanel = comments.get("gameserver.administration.gmpanel");
		assertTrue(gmpanel != null && gmpanel.isPresent());
		assertEquals("GM 面板", gmpanel.description());
		assertEquals("GM Panel", gmpanel.descriptionEn());
		assertEquals("3", gmpanel.defaultValue());
	}
}
