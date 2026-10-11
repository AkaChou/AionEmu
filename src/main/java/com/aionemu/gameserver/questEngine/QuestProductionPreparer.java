package com.aionemu.gameserver.questEngine;

import java.util.List;
import java.util.function.IntFunction;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestBroadcastPort;
import com.aionemu.gameserver.questEngine.runtime.PlayerSerialExecutor;
import com.aionemu.gameserver.questEngine.runtime.QuestDispatchContract;
import com.aionemu.gameserver.questEngine.runtime.QuestExecutionCoordinator;
import com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectValidator;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionDispatcher;
import com.aionemu.gameserver.questEngine.runtime.QuestProductionEventWiring;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeComposition;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeRouter;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

/**
 * 生产 typed 目录准备器：把 raw catalog 组装为可发布的校验快照
 * （组合目录 + 事件接线校验 + 交互对象合同 + item-play 索引合同）。
 * Production typed-catalog preparer: assembles a publishable validated snapshot (combined catalog,
 * event wiring validation, interaction-object contract, item-play index contract) from the raw
 * catalog.
 */
final class QuestProductionPreparer {

	private QuestProductionPreparer() {
	}

	/**
	 * 生产 NPC AI 查询（启动期固定使用）：{@code DataManager.NPC_DATA} 未就绪时返回 null。
	 * Production NPC AI lookup; returns null while the runtime {@code DataManager} is not ready.
	 */
	static String productionNpcAi(int templateId) {
		if (DataManager.NPC_DATA == null) {
			return null;
		}
		var template = DataManager.NPC_DATA.getNpcTemplate(templateId);
		return template == null ? null : template.getAi();
	}

	/** 以生产 NPC AI 索引准备目录。 / Prepares the catalog with the production NPC AI index. */
	static QuestEngine.PreparedProductionDefinitions prepare(QuestCatalog catalog) {
		return prepare(catalog, QuestProductionPreparer::productionNpcAi);
	}

	/**
	 * 生产准备的可注入重载：NPC AI 索引由调用方给出，其余启动期合同完全一致。
	 * <p>
	 * 生产调用固定注入 {@link #productionNpcAi(int)}（{@code DataManager.NPC_DATA}）；启动期门禁注入测试侧
	 * AI 索引，从而在不起静态数据的情况下跑完整准备路径（交互对象合同 + 事件接线合同 + item-play 索引）。
	 * Production preparation with an injectable NPC AI index; the gates use it to exercise the whole
	 * startup path without the runtime {@code DataManager} bootstrap.
	 */
	static QuestEngine.PreparedProductionDefinitions prepare(QuestCatalog catalog,
			IntFunction<String> aiNameByTemplate) {
		java.util.Objects.requireNonNull(aiNameByTemplate, "aiNameByTemplate");
		RuntimeCatalogs catalogs = splitRuntimeCatalogs(catalog);
		QuestRuntimeComposition snapshotComposition = QuestRuntimeComposition.production(catalogs.combined());
		QuestExecutionCoordinator coordinator = new QuestExecutionCoordinator(new PlayerSerialExecutor());
		QuestProductionDispatcher xmlDispatcher = QuestProductionDispatcher.production(catalogs.xml(),
			snapshotComposition, coordinator);
		QuestProductionDispatcher retailDispatcher = catalogs.retail().entries().isEmpty()
			? QuestProductionDispatcher.disabled()
			: QuestProductionDispatcher.production(catalogs.retail(), snapshotComposition, coordinator);
		QuestRuntimeRouter dispatcher = new QuestRuntimeRouter(catalogs.combined(), xmlDispatcher, retailDispatcher);
		QuestInteractionObjectValidator.validate(xmlDispatcher, aiNameByTemplate);
		QuestInteractionObjectValidator.validate(retailDispatcher, aiNameByTemplate);
		snapshotComposition.installBroadcastPort(new PlayerQuestBroadcastPort(
			playerId -> GameWorldBootstrapServices.world().findPlayer(playerId),
			(player, questIds) -> dispatcher.dispatchOwners(new QuestEvent.ZoneMissionEnd(), player.getObjectId(),
				questIds, QuestDispatchContract.EXCLUSIVE),
			(player, questIds) -> dispatcher.dispatchOwners(new QuestEvent.EventQuestRefresh(), player.getObjectId(),
				questIds, QuestDispatchContract.EXCLUSIVE)));
		for (CompiledQuestDefinition definition : catalogs.combined().executables()) {
			QuestProductionEventWiring.validateDefinition(definition);
			if (definition.definition().transitions().stream()
					.map(QuestTransition::event)
					.filter(QuestEvent.ItemPlay.class::isInstance)
					.map(QuestEvent.ItemPlay.class::cast)
					.mapToInt(QuestEvent.ItemPlay::itemId)
					.distinct()
					.anyMatch(itemId -> dispatcher.itemPlayAnimationMillis(itemId).isEmpty())) {
				throw new IllegalStateException("typed item-play event has no indexed route");
			}
		}
		return new QuestEngine.PreparedProductionDefinitions(catalogs.combined(), dispatcher);
	}

	/**
	 * 将生产目录按原版保留清单拆成 XML 与原版两个互斥子目录。
	 * Splits the production catalog into disjoint XML and retail child catalogs according to the retail owner manifest.
	 * <p>
	 * P7 步 f 起 retail 编译产物为零（七族 + DataDriven 1467 行全部原生直驱），retail 子目录恒空，
	 * 生产与测试目录都走 combined 单目录兼容形；归属核验仍由
	 * {@link RetailQuestDriver#overlayProduction(QuestCatalog)} 在装载前完成。
	 * Since P7 step f the retail compile products are zero (all families and the 1467 DataDriven rows
	 * are native-driven), so the retail child catalog is always empty and both production and test
	 * catalogs use the combined form; ownership is still validated by overlayProduction before load.
	 */
	private static RuntimeCatalogs splitRuntimeCatalogs(QuestCatalog catalog) {
		QuestCatalogRegistry combined = catalog instanceof QuestCatalogRegistry existing
			? existing : new QuestCatalogRegistry(catalog);
		return new RuntimeCatalogs(combined, combined, new QuestCatalogRegistry(new ImmutableQuestCatalog(List.of())));
	}

	/** 已校验的组合目录与两个互斥子目录。 / Validated combined catalog and its two disjoint child catalogs. */
	private record RuntimeCatalogs(QuestCatalogRegistry combined, QuestCatalogRegistry xml, QuestCatalogRegistry retail) {
	}
}
