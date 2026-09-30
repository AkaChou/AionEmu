package com.aionemu.gameserver.questEngine.runtime;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 验证 XML 与真端运行时隔离以及真端本地对话页动作的 no-op 语义。
 * Verifies XML/retail runtime isolation and no-op semantics for retail-local dialog page actions.
 */
class QuestRuntimeRouterTest {

	@Test
	void retailLocalPageNavigationIsHandledWithoutEnteringXmlRuntime() {
		QuestCatalogRegistry retailCatalog = catalog(1000);
		QuestCatalogRegistry xmlCatalog = catalog(990001);
		QuestRuntimeComposition composition = QuestRuntimeComposition.production(retailCatalog);
		QuestProductionDispatcher xml = QuestProductionDispatcher.production(xmlCatalog, composition);
		QuestProductionDispatcher retail = QuestProductionDispatcher.production(retailCatalog, composition);
		QuestRuntimeRouter router = new QuestRuntimeRouter(retailCatalog, xml, retail);

		var result = router.dispatch(new QuestEvent.TalkToNpc(1, 1012), 1, 1000,
			QuestDispatchContract.EXCLUSIVE);

		assertTrue(result.handled());
		assertFalse(router.hasMatchingRoutes(new QuestEvent.TalkToNpc(1, 1012), 990001));
	}

	@Test
	void unknownOwnerDoesNotCreateAnImplicitLocalNavigationOwner() {
		QuestCatalogRegistry retailCatalog = catalog(1000);
		QuestRuntimeComposition composition = QuestRuntimeComposition.production(retailCatalog);
		QuestProductionDispatcher retail = QuestProductionDispatcher.production(retailCatalog, composition);
		QuestRuntimeRouter router = new QuestRuntimeRouter(retailCatalog, QuestProductionDispatcher.disabled(), retail);

		var result = router.dispatch(new QuestEvent.TalkToNpc(1, 1012), 1, 990001,
			QuestDispatchContract.EXCLUSIVE);

		assertFalse(result.handled());
		assertTrue(result.owners().isEmpty());
	}

	private static QuestCatalogRegistry catalog(int questId) {
		var definition = QuestDsl.quest(questId)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.TalkToNpc(1, 31)).from("unaccepted").goTo("started")
			.compile();
		return new QuestCatalogRegistry(new ImmutableQuestCatalog(List.of(definition)));
	}
}
