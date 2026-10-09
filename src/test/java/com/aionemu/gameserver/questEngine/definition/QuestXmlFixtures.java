package com.aionemu.gameserver.questEngine.definition;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 任务定义 XML 的测试装载入口（仅生产 XML）。
 * <p>
 * 迁移到原版驱动的任务不再保留 XML 副本：历史内容在 git 里可回溯，退役后的生产定义由
 * {@link ProductionQuestDefinitions} 提供。断言"生产仍在用 XML"的测试必须用
 * {@link #productionXmlPresent(int)} 看源树，不能用 classpath（{@code target/classes} 会残留已删除资源）。
 * <p>
 * Test-scope loader for the production quest-definition XML only; retired quests go through the retail view.
 */
public final class QuestXmlFixtures {

	private static final String PRODUCTION_RESOURCE = "/aion/data/static_data/quest/definitions/quests/%d.xml";
	private static final Path PRODUCTION_DIR =
		Path.of("src/main/resources/aion/data/static_data/quest/definitions/quests");

	private QuestXmlFixtures() {
	}

	/**
	 * 打开生产 XML；任务已退役（迁移到原版驱动）时失败——此类测试必须改用
	 * {@link ProductionQuestDefinitions}（历史 XML 在 git 里可回溯，不再进仓）。
	 * Opens the production XML; retired quests must go through {@link ProductionQuestDefinitions}.
	 */
	public static InputStream open(int questId) {
		InputStream production = QuestXmlFixtures.class
			.getResourceAsStream(String.format(PRODUCTION_RESOURCE, questId));
		return Objects.requireNonNull(production, "quest " + questId
			+ " no longer has a production XML (retail-driven); use ProductionQuestDefinitions");
	}

	/** 编译定义 XML。 / Compiles the definition XML. */
	public static CompiledQuestDefinition compile(int questId) throws Exception {
		try (InputStream input = open(questId)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 按生产资源路径打开定义 XML，缺失时回落到退役冻结副本；路径末段的任务号即索引。
	 * <p>
	 * 供仍按 {@code getResourceAsStream("/aion/data/.../quests/<id>.xml")} 形态读取定义的测试使用：
	 * 任务迁到原版驱动后生产资源消失，读到的内容与冻结副本逐字节相同。
	 * <p>
	 * Opens a definition by its production resource path with a retired-fixture fallback.
	 */
	public static InputStream openResource(String productionPath) {
		String name = productionPath.substring(productionPath.lastIndexOf('/') + 1);
		String digits = name.replaceAll("\\D+", "");
		if (digits.isEmpty()) {
			throw new IllegalArgumentException("quest definition path has no quest id: " + productionPath);
		}
		return open(Integer.parseInt(digits));
	}

	/** 生产源树里是否仍有该任务的 XML（退役判据）。 / Whether the production source tree still holds the XML. */
	public static boolean productionXmlPresent(int questId) {
		return Files.exists(PRODUCTION_DIR.resolve(questId + ".xml"));
	}
}
