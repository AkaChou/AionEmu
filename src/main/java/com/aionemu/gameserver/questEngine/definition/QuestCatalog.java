package com.aionemu.gameserver.questEngine.definition;

import java.util.Collection;
import java.util.Optional;

/** The only lookup contract for compiled task definitions. */
public interface QuestCatalog {
	Optional<CompiledQuestDefinition> find(int questId);

	Collection<CompiledQuestDefinition> all();
}
