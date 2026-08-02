package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDefinitionRegistryTest {
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "registry", "fixture");

	@Test
	void reloadCompilesBeforeAtomicReplacement() {
		QuestDefinitionRegistry registry = new QuestDefinitionRegistry();
		registry.reload(List.of(source(1001, 6)));
		assertEquals(1, registry.all().size());

		assertEquals("DUPLICATE_OWNER", assertThrows(QuestCompilationException.class,
				() -> registry.reload(List.of(source(1001, 6), source(1001, 6)))).code());
		assertEquals(1, registry.all().size());
	}

	@Test
	void incompatibleLayoutIsRejectedWithoutChangingActiveCatalog() {
		QuestDefinitionRegistry registry = new QuestDefinitionRegistry();
		registry.reload(List.of(source(1001, 6)));
		assertEquals("INCOMPATIBLE_PROGRESS_LAYOUT", assertThrows(QuestCompilationException.class,
				() -> registry.reload(List.of(source(1001, 10)))).code());
		assertEquals(6, registry.find(1001).orElseThrow().definition().progressLayout().field("var1").width());
	}

	private static QuestDefinition source(int id, int width) {
		return quest(id)
				.evidence(EVIDENCE)
				.progress(bitField("var1", 0, width, PersistenceMode.PERSISTENT))
				.node("start", project(com.aionemu.gameserver.questEngine.model.QuestStatus.START, vars("var1", 0)))
				.on(talkToNpc(700001)).from("start").goTo("start")
				.compile().definition();
	}
}
