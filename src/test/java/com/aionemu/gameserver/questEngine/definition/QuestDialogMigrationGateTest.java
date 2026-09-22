package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 防止生产源码重新依赖混用动作/页面命名空间的旧 {@code QuestDialog} 枚举。
 * Prevents production sources from depending again on the legacy {@code QuestDialog} enum
 * that mixed the action and page namespaces.
 */
class QuestDialogMigrationGateTest {
	private static final Path LEGACY_ENUM = Path.of(
		"src/main/java/com/aionemu/gameserver/questEngine/model/QuestDialog.java");
	private static final Pattern LEGACY_ENUM_REFERENCE = Pattern.compile("\\bQuestDialog\\.[A-Z][A-Z0-9_]*");
	private static final List<Pattern> LEGACY_QUEST_XML_SYNTAX = List.of(
		Pattern.compile("<talk-to-npc\\b"),
		Pattern.compile("<quest-dialog\\b"),
		Pattern.compile("<show-quest-dialog\\b"),
		Pattern.compile("<show-quest-selection-dialog\\b"),
		Pattern.compile("\\b(?:start-dialog-id|preview-dialog-ids|dialog-ids)\\s*="));

	@Test
	void legacyQuestDialogEnumStaysDeleted() {
		assertFalse(Files.exists(LEGACY_ENUM), () -> LEGACY_ENUM + " must not be restored");
	}

	@Test
	void productionSourcesDoNotUseTheLegacyQuestDialogEnum() throws Exception {
		try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
			paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				String source = read(path);
				assertFalse(source.contains("questEngine.model.QuestDialog"),
					() -> path + " imports the legacy QuestDialog enum");
				assertFalse(LEGACY_ENUM_REFERENCE.matcher(source).find(),
					() -> path + " references a legacy QuestDialog constant");
			});
		}
	}

	@Test
	void productionQuestXmlUsesTypedDialogActionsAndPages() throws Exception {
		try (Stream<Path> paths = Files.walk(Path.of("src/main/resources/aion/data/static_data/quest_definition"))) {
			paths.filter(path -> {
				String name = path.toString();
				return name.endsWith(".xml") || name.endsWith(".xsd");
			}).forEach(path -> {
				String source = read(path);
				for (Pattern pattern : LEGACY_QUEST_XML_SYNTAX) {
					assertFalse(pattern.matcher(source).find(),
						() -> path + " contains legacy quest-dialog syntax " + pattern);
				}
			});
		}
	}

	@Test
	void dialogEnumGeneratorDoesNotScanLegacySyntax() {
		Path generator = Path.of(".agents/summary/quest/generate_quest_dialog_enums.py");
		assertFalse(LEGACY_QUEST_XML_SYNTAX.stream().anyMatch(pattern -> pattern.matcher(read(generator)).find()),
			() -> generator + " scans legacy quest-dialog syntax");
	}

	private static String read(Path path) {
		try {
			return Files.readString(path);
		} catch (Exception e) {
			throw new IllegalStateException("cannot read " + path, e);
		}
	}
}
