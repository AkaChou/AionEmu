package com.aionemu.gameserver.questEngine.definition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Merges definition sources while giving typed Java definitions ownership of an ID. */
final class QuestDefinitionCatalogMerger {
	private QuestDefinitionCatalogMerger() {
	}

	static QuestCatalog javaOverridesXml(QuestCatalog xmlCatalog, QuestCatalog javaCatalog) {
		Objects.requireNonNull(xmlCatalog, "xmlCatalog");
		Objects.requireNonNull(javaCatalog, "javaCatalog");
		Map<Integer, CompiledQuestDefinition> definitions = new LinkedHashMap<>();
		for (CompiledQuestDefinition definition : xmlCatalog.all()) {
			definitions.put(definition.id(), definition);
		}
		for (CompiledQuestDefinition definition : javaCatalog.all()) {
			// Java DSL is the explicit owner when XML and Java describe the same quest.
			definitions.put(definition.id(), definition);
		}
		return new ImmutableQuestCatalog(definitions.values());
	}
}
