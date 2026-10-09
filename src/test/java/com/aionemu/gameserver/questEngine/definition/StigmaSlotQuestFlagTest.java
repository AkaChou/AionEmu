package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 烙印槽位资格的数据面：原版 {@code reward_extend_stigma1} ↔ 定义 XML 的 {@code extend-stigma-slots} 属性。
 * Java 侧据元数据判定资格，不再硬编码任务 ID、步数或教学结晶。
 * <p>
 * The data face of the stigma-slot entitlement: retail {@code reward_extend_stigma1} maps to the
 * {@code extend-stigma-slots} attribute of the definition XML, and the Java side decides from the
 * metadata instead of hardcoding quest ids, step numbers, or tutorial items.
 */
class StigmaSlotQuestFlagTest {
	private static final Path QUESTS = Path.of(
		"src/main/resources/aion/data/static_data/quest/definitions/quests");

	/** 原版 quest.xml 中带 {@code reward_extend_stigma1=1} 的 16 行中，本仓已有定义 XML 的 4 行。 / The four flagged retail rows that already have a definition XML here. */
	private static final Set<Integer> FLAGGED_QUESTS = Set.of(1929, 2900, 30217, 30317);

	@Test
	void onlyTheHeadStartQuestDefinitionsDeclareTheStigmaSlotExtension() throws Exception {
		Set<Integer> flagged = new TreeSet<>();
		try (Stream<Path> files = Files.list(QUESTS)) {
			for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".xml")).toList()) {
				if (Files.readString(file).contains("extend-stigma-slots=\"true\"")) {
					flagged.add(Integer.parseInt(file.getFileName().toString().replace(".xml", "")));
				}
			}
		}
		assertEquals(FLAGGED_QUESTS, flagged, "unexpected quest definitions flagged with extend-stigma-slots");
	}

	@Test
	void theFlaggedDefinitionsCompileIntoTheStigmaSlotMetadataFlag() throws Exception {
		for (int questId : FLAGGED_QUESTS) {
			assertTrue(compile(QUESTS.resolve(questId + ".xml")).definition().metadata().extendStigmaSlots(),
				"quest " + questId + " must carry the stigma-slot flag");
		}
	}

	@Test
	void unflaggedDefinitionsDefaultToNoSlotExtension() throws Exception {
		assertFalse(compile(QUESTS.resolve("1001.xml")).definition().metadata().extendStigmaSlots());
	}

	@Test
	void theProductionCatalogExposesTheFlagForEveryRetainedStigmaQuest() {
		for (int questId : FLAGGED_QUESTS) {
			assertTrue(ProductionQuestDefinitions.definition(questId).definition().metadata().extendStigmaSlots(),
				"production catalog must expose extendStigmaSlots for quest " + questId);
		}
	}

	private static CompiledQuestDefinition compile(Path path) throws Exception {
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
