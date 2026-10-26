package com.aionemu.boot.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 配置分组测试：分组结构、分组归属与主服文件映射。
 * Tests for the configuration groups: the group structure, the group membership and the main file mapping.
 */
class ConfigGroupTest {

	/**
	 * 分组按菜单顺序注册，且标识唯一。
	 * The groups are registered in menu order and their ids are unique.
	 */
	@Test
	void groupsAreRegisteredInMenuOrderWithUniqueIds() {
		List<ConfigGroup> groups = ConfigGroup.all();

		assertEquals("administration", groups.get(0).id());
		assertEquals("main", groups.get(1).id());
		assertEquals("shop", groups.get(2).id());
		assertEquals(groups.size(), groups.stream().map(ConfigGroup::id).distinct().count(),
			"the group ids must be unique");
	}

	/**
	 * 每个分组都有中英文标签。
	 * Every group carries a Chinese and an English label.
	 */
	@Test
	void everyGroupCarriesBothLabels() {
		for (ConfigGroup group : ConfigGroup.all()) {
			assertNotNull(group.labelZh());
			assertNotNull(group.labelEn());
			assertTrue(!group.labelZh().isBlank(), group.id() + " must carry a Chinese label");
			assertTrue(!group.labelEn().isBlank(), group.id() + " must carry an English label");
		}
	}

	/**
	 * 管理分组覆盖首批的六个文件。
	 * The administration group covers the six files of the first batch.
	 */
	@Test
	void administrationGroupCoversTheFirstBatch() {
		List<EditableFile> files = ConfigGroup.ADMINISTRATION.files();

		assertEquals(6, files.size());
		assertTrue(files.contains(EditableFile.ADMIN));
		assertTrue(files.contains(EditableFile.ITEM_RESTRICTION));
	}

	/**
	 * 商城分组同时包含商城开关与商城限制两个文件，且它们也留在主服分组里。
	 * The shop group holds the shop switches and the shop limits, and they stay in the main group too.
	 */
	@Test
	void shopFilesAlsoBelongToTheMainGroup() {
		List<EditableFile> shopFiles = ConfigGroup.SHOP.files();

		assertEquals(2, shopFiles.size());
		for (EditableFile file : shopFiles) {
			assertTrue(file.groups().contains(ConfigGroup.SHOP));
			assertTrue(file.groups().contains(ConfigGroup.MAIN),
				file.name() + " must stay in the main group as well");
		}
	}

	/**
	 * 主服分组覆盖 {@code main/} 目录下的每个配置文件，且每个文件都映射到配置类。
	 * The main group covers every configuration file under {@code main/} and each file maps to a
	 * configuration class.
	 * @throws IOException 目录读取失败 / when the directory cannot be read
	 */
	@Test
	void mainGroupCoversEveryMainFileWithAConfigClass() throws IOException {
		Path mainDirectory = Path.of("src/main/resources/aion/config/main");
		assumeTrue(Files.isDirectory(mainDirectory));

		List<String> expected;
		try (Stream<Path> files = Files.list(mainDirectory)) {
			expected = files
				.filter(path -> path.getFileName().toString().endsWith(".properties"))
				.map(path -> path.getFileName().toString())
				.sorted()
				.toList();
		}

		List<EditableFile> registered = ConfigGroup.MAIN.files();
		assertEquals(expected.size(), registered.size(),
			"every main configuration file must be registered in the main group");
		for (EditableFile file : registered) {
			assertNotNull(file.configClass(),
				file.name() + " must map to a configuration class for the targeted hot-apply");
			assertTrue(file.applyTarget() == EditableFile.ApplyTarget.MAIN_CONFIG
				|| file.applyTarget() == EditableFile.ApplyTarget.LOGIN_VIP_CONFIG,
				file.name() + " must use a targeted hot-apply, but got " + file.applyTarget());
		}
	}
}
