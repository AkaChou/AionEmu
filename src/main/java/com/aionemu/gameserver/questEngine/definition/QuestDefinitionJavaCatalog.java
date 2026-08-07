package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.definition.generated.GeneratedQuestDslCatalog;

import java.util.Optional;

/** Access point for definitions authored as generated or hand-written Java DSL. */
public final class QuestDefinitionJavaCatalog {
	private static final QuestCatalog CATALOG = GeneratedQuestDslCatalog.compile();

	private QuestDefinitionJavaCatalog() {
	}

	public static QuestCatalog compile() {
		return CATALOG;
	}

	public static Optional<CompiledQuestDefinition> find(int questId) {
		return CATALOG.find(questId);
	}

	public static CompiledQuestDefinition require(int questId) {
		return find(questId).orElseThrow(() -> new QuestCompilationException(
			"JAVA_DSL_DEFINITION_MISSING", "quest " + questId));
	}
}
