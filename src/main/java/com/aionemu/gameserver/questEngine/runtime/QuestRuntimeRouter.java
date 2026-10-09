package com.aionemu.gameserver.questEngine.runtime;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;

import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.retail.RetailDialogIntentClassifier;

/**
 * 将 XML owner 与原版 owner 路由到彼此隔离的运行时，同时保留跨 owner 广播语义。
 * Routes XML-owned and retail-owned quests to isolated runtimes while preserving cross-owner broadcasts.
 * <p>
 * 路由规则以 owner 为单位：指定 questId 时只访问唯一拥有该 ID 的子运行时；questId 为 0 时按
 * 派发合同组合两个子运行时的结果。重复 owner 视为启动目录裂分错误并立即失败。
 * Routing is owner-scoped: a named quest id reaches exactly one child runtime, while questId 0 composes
 * child results according to the dispatch contract. Duplicate ownership is a catalog split error and fails fast.</p>
 */
public final class QuestRuntimeRouter implements QuestRuntimeDispatcher {

	private final QuestCatalogRegistry catalog;
	private final QuestProductionDispatcher xmlDispatcher;
	private final QuestProductionDispatcher retailDispatcher;

	public QuestRuntimeRouter(QuestCatalogRegistry catalog, QuestProductionDispatcher xmlDispatcher,
			QuestProductionDispatcher retailDispatcher) {
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.xmlDispatcher = Objects.requireNonNull(xmlDispatcher, "xmlDispatcher");
		this.retailDispatcher = Objects.requireNonNull(retailDispatcher, "retailDispatcher");
	}

	@Override
	public QuestCatalogRegistry catalogRegistry() {
		return catalog;
	}

	@Override
	public List<QuestCatalogDrop> questDrops(int npcId) {
		if (npcId <= 0) {
			return List.of();
		}
		Set<QuestCatalogDrop> drops = new LinkedHashSet<>(xmlDispatcher.questDrops(npcId));
		drops.addAll(retailDispatcher.questDrops(npcId));
		return List.copyOf(drops);
	}

	@Override
	public boolean owns(int questId) {
		return questId > 0 && (xmlDispatcher.owns(questId) || retailDispatcher.owns(questId));
	}

	@Override
	public boolean hasRoutes(QuestEvent event) {
		return xmlDispatcher.hasRoutes(event) || retailDispatcher.hasRoutes(event);
	}

	@Override
	public boolean hasRoutes(QuestEvent event, int questId) {
		QuestProductionDispatcher owner = ownerOrNull(questId);
		return owner != null && owner.hasRoutes(event, questId);
	}

	@Override
	public boolean hasMatchingRoutes(QuestEvent event, int questId) {
		QuestProductionDispatcher owner = ownerOrNull(questId);
		return owner != null && owner.hasMatchingRoutes(event, questId);
	}

	@Override
	public List<Integer> owners() {
		Set<Integer> owners = new TreeSet<>(xmlDispatcher.owners());
		owners.addAll(retailDispatcher.owners());
		return List.copyOf(owners);
	}

	@Override
	public boolean dispatchRewardWindowAction(QuestEvent.TalkToNpc event, int playerId, int questId) {
		QuestProductionDispatcher owner = ownerOrNull(questId);
		return owner != null && owner.dispatchRewardWindowAction(event, playerId, questId);
	}

	@Override
	public OptionalInt itemPlayAnimationMillis(int itemId) {
		OptionalInt xml = xmlDispatcher.itemPlayAnimationMillis(itemId);
		return xml.isPresent() ? xml : retailDispatcher.itemPlayAnimationMillis(itemId);
	}

	@Override
	public QuestEventRouter.DispatchResult dispatch(QuestEvent event, int playerId, int questId,
			QuestDispatchContract contract) {
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(contract, "contract");
		if (questId > 0) {
			if (retailDispatcher.owns(questId) && isUnroutedLocalDialog(event, questId)) {
				return new QuestEventRouter.DispatchResult(contract,
					List.of(new QuestEventRouter.OwnerResult(questId, QuestRouteResult.HANDLED, null)));
			}
			QuestProductionDispatcher owner = ownerOrNull(questId);
			return owner == null ? new QuestEventRouter.DispatchResult(contract, List.of())
				: owner.dispatch(event, playerId, questId, contract);
		}
		if (questId < 0) {
			throw new IllegalArgumentException("questId must not be negative");
		}
		QuestEventRouter.DispatchResult xml = xmlDispatcher.dispatch(event, playerId, 0, contract);
		if (stopsAfterXml(xml, contract)) {
			return xml;
		}
		QuestEventRouter.DispatchResult retail = retailDispatcher.dispatch(event, playerId, 0, contract);
		return combine(contract, xml, retail);
	}

	@Override
	public boolean dispatchOwners(QuestEvent event, int playerId, int[] questIds, QuestDispatchContract contract) {
		Objects.requireNonNull(questIds, "questIds");
		boolean success = questIds.length > 0;
		for (int questId : questIds) {
			QuestEventRouter.DispatchResult result = dispatch(event, playerId, questId, contract);
			success &= !result.owners().isEmpty() && !result.failed();
		}
		return success;
	}

	@Override
	public void dispatchQuestStateChanged(int playerId, int changedQuestId) {
		xmlDispatcher.dispatchQuestStateChanged(playerId, changedQuestId);
		retailDispatcher.dispatchQuestStateChanged(playerId, changedQuestId);
	}

	@Override
	public boolean dispatchSharedQuestAccept(int playerId, int questId, int dialogId) {
		QuestProductionDispatcher owner = ownerOrNull(questId);
		return owner != null && owner.dispatchSharedQuestAccept(playerId, questId, dialogId);
	}

	private QuestProductionDispatcher ownerOrNull(int questId) {
		if (questId <= 0) {
			throw new IllegalArgumentException("questId must be positive");
		}
		boolean xmlOwns = xmlDispatcher.owns(questId);
		boolean retailOwns = retailDispatcher.owns(questId);
		if (xmlOwns && retailOwns) {
			throw new IllegalStateException("quest " + questId + " must be owned by exactly one runtime, xml="
				+ xmlOwns + ", retail=" + retailOwns);
		}
		return xmlOwns ? xmlDispatcher : retailOwns ? retailDispatcher : null;
	}

	private static boolean stopsAfterXml(QuestEventRouter.DispatchResult xml, QuestDispatchContract contract) {
		return switch (contract) {
			case EXCLUSIVE -> xml.claimed();
			case FIRST_REGISTERED -> !xml.owners().isEmpty();
			case FIRST_NON_UNKNOWN -> xml.owners().stream()
				.anyMatch(owner -> owner.result() != QuestRouteResult.UNKNOWN);
			case BROADCAST -> false;
		};
	}

	private static QuestEventRouter.DispatchResult combine(QuestDispatchContract contract,
			QuestEventRouter.DispatchResult xml, QuestEventRouter.DispatchResult retail) {
		List<QuestEventRouter.OwnerResult> owners = new ArrayList<>(xml.owners().size() + retail.owners().size());
		owners.addAll(xml.owners());
		owners.addAll(retail.owners());
		return new QuestEventRouter.DispatchResult(contract, List.copyOf(owners));
	}

	private boolean isUnroutedLocalDialog(QuestEvent event, int questId) {
		// 事件索引按宽键（例如 NPC ID）登记对话路由，因此这里必须按实际匹配判断：
		// 只有该 owner 确实没有匹配路由时，客户端本地翻页才由服务端确认而不进入任务运行时。
		// The event index registers dialogs under broad keys (for example the npc id), so
		// only an actual match check can prove the owner has no route for this action:
		// only then is a client-local page turn acknowledged without entering the runtime.
		if (retailDispatcher.hasMatchingRoutes(event, questId)) {
			return false;
		}
		int actionId = switch (event) {
			case QuestEvent.TalkToNpc talk -> talk.dialogId() == null ? 0 : talk.dialogId();
			case QuestEvent.QuestDialog dialog -> dialog.dialogId();
			default -> 0;
		};
		return actionId != 0 && RetailDialogIntentClassifier.classify(questId, actionId).isPresent();
	}
}
