package com.aionemu.boot.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.aionemu.boot.config.AionConfigFilePropertySource;
import com.aionemu.boot.config.AionConfigSourceRefresher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

/**
 * 管理控制台配置服务测试：行级保留写入、差异预览与失败回滚。
 * Tests for the admin console configuration service: line-preserving writes, diff preview and the failure
 * rollback.
 */
class AdminConfigServiceTest {

	@TempDir
	Path tempDir;

	private Path configDir;

	@BeforeEach
	void setUp() throws Exception {
		configDir = tempDir.resolve("config");
		Files.createDirectories(configDir.resolve("administration"));
		Files.writeString(configDir.resolve("administration/admin.properties"), """
			# GM 等级 / GM Level
			# 默认值: 3 / Default: 3
			gameserver.administration.gmlevel = 3
			""");
	}

	/**
	 * 构造配置服务。
	 * Builds the configuration service.
	 * @return 配置服务 / configuration service
	 */
	private AdminConfigService service() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(
			new AionConfigFilePropertySource(AionConfigFilePropertySource.readEntries(configDir)));
		AionConfigSourceRefresher refresher = new AionConfigSourceRefresher(environment);
		EffectiveValueResolver resolver = new EffectiveValueResolver(environment, configDir);
		return new AdminConfigService(configDir, refresher, resolver);
	}

	/**
	 * 读取文件时必须保留注释块。
	 * Reading a file must keep the comment blocks.
	 */
	@Test
	void readingAFileKeepsTheCommentBlocks() {
		AdminConfigService.FileView view = service().read(EditableFile.ADMIN);

		assertEquals("3", view.values().get("gameserver.administration.gmlevel"));
		assertTrue(view.text().contains("# 默认值: 3 / Default: 3"));
	}

	/**
	 * 预览差异不得落盘。
	 * The diff preview must not touch the disk.
	 */
	@Test
	void previewDoesNotTouchTheDisk() throws Exception {
		Path file = configDir.resolve("administration/admin.properties");
		long before = Files.getLastModifiedTime(file).toMillis();

		AdminConfigService.DiffPreview preview = service().preview(EditableFile.ADMIN,
			Map.of("gameserver.administration.gmlevel", "5"));

		assertFalse(preview.lines().isEmpty(), "the preview must show the changed line");
		assertEquals(before, Files.getLastModifiedTime(file).toMillis(), "the preview must not write the file");
		assertTrue(Files.readString(file).contains("gmlevel = 3"), "the file must still hold the old value");
	}

	/**
	 * 写入后文件里的注释与未改动的行必须原样保留。
	 * After a write, the comments and the untouched lines must stay verbatim.
	 */
	@Test
	void writingKeepsCommentsAndUntouchedLines() throws Exception {
		AdminConfigService.ApplyResult result = service().apply(EditableFile.ADMIN,
			Map.of("gameserver.administration.gmlevel", "5"));

		assertTrue(result.applied());
		String text = Files.readString(configDir.resolve("administration/admin.properties"));
		assertTrue(text.contains("gameserver.administration.gmlevel = 5"));
		assertTrue(text.contains("# GM 等级 / GM Level"));
		assertTrue(text.contains("# 默认值: 3 / Default: 3"));
	}

	/**
	 * 没有任何改动时不得写盘。
	 * Nothing must be written when there is no change at all.
	 */
	@Test
	void applyingTheSameValueChangesNothing() throws Exception {
		Path file = configDir.resolve("administration/admin.properties");
		String before = Files.readString(file);

		AdminConfigService.ApplyResult result = service().apply(EditableFile.ADMIN,
			Map.of("gameserver.administration.gmlevel", "3"));

		assertTrue(result.applied());
		assertFalse(result.rolledBack());
		assertEquals(before, Files.readString(file), "the file must be untouched");
	}

	/**
	 * 差异预览必须只报告真正变化的行。
	 * The diff preview must report only the lines that actually changed.
	 */
	@Test
	void previewReportsOnlyChangedLines() {
		AdminConfigService.DiffPreview preview = service().preview(EditableFile.ADMIN,
			Map.of("gameserver.administration.gmlevel", "5"));

		assertEquals(1, preview.lines().size(), "only the changed line may be reported");
		AdminConfigService.DiffLine line = preview.lines().get(0);
		assertTrue(line.before().contains("gmlevel = 3"));
		assertTrue(line.after().contains("gmlevel = 5"));
	}

	/**
	 * 物品 ID 列表按 ID 解析，注释行被忽略。
	 * The item id list is parsed by id and the comment lines are ignored.
	 */
	@Test
	void itemIdListIsParsedById() throws Exception {
		Files.writeString(configDir.resolve("administration/item.restriction.txt"), """
			# item destriction file
			# put itemId on a line to allow mail/broker/"//add" it

			10000001
			10000002
			""");

		AdminConfigService.FileView view = service().read(EditableFile.ITEM_RESTRICTION);

		assertEquals(List.of("10000001", "10000002"), List.copyOf(view.values().keySet()));
	}
}
